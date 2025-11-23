package com.example.myapplication
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.random.Random
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth = Firebase.auth
    private val dbFirestore = Firebase.firestore
    private val dbRealtime = Firebase.database
    private var currentUser: User? = null
    private var patientId: String? = null
    private val scheduleList = mutableListOf<Schedule>()
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var alarmManager: AlarmManager
    private val pendingIntents = mutableMapOf<String, PendingIntent>()
    private val NOTIFICATION_PERMISSION_CODE = 101
    private val providerPackageName = "com.google.android.apps.healthdata"
    private lateinit var healthConnectClient: HealthConnectClient
    private var isHealthConnectAvailable = false
    private val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )
    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(PERMISSIONS)) {
            Toast.makeText(this, "Health permissions granted!", Toast.LENGTH_SHORT).show()
            readVitalsFromWatch()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Health permissions were not granted. To sync your watch, open Health Connect settings.")
                .setPositiveButton("Open Settings") { _, _ -> openHealthConnectAppSettings() }
                .setNegativeButton("Use Simulation") { _, _ -> simulateVitals() }
                .show()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        createNotificationChannel()
        NotificationHelper.createChannel(this)
        requestNotificationPermission()
        requestAlarmPermission()
        patientId = auth.currentUser?.uid
        if (patientId == null) { goToLogin(); return }
        checkHealthConnectStatusAndPrompt()
        binding.btnConnectWatch.setOnClickListener {
            if (isHealthConnectAvailable) showHealthConnectRationaleAndRequest() else simulateVitals()
        }
        binding.rvSchedules.layoutManager = LinearLayoutManager(this)
        loadPatientData(patientId!!)
        listenForSchedules(patientId!!)
        listenForIncomingMessages()
        binding.btnLogout.setOnClickListener { auth.signOut(); goToLogin() }
        binding.btnRefill1.setOnClickListener { updatePillCount(1, binding.etRefillPills1.text.toString().toIntOrNull() ?: 0); binding.etRefillPills1.text.clear() }
        binding.btnRefill2.setOnClickListener { updatePillCount(2, binding.etRefillPills2.text.toString().toIntOrNull() ?: 0); binding.etRefillPills2.text.clear() }
        binding.btnAddNewSchedule.setOnClickListener { startActivity(Intent(this, AddScheduleActivity::class.java)) }
        binding.btnDispenseNow1.setOnClickListener { if (currentUser != null && currentUser!!.pillsChamber1 >= 1) writeScheduleToRTDB(1, 1, "NOW") else Toast.makeText(this, "Chamber 1 Empty!", Toast.LENGTH_SHORT).show() }
        binding.btnDispenseNow2.setOnClickListener { if (currentUser != null && currentUser!!.pillsChamber2 >= 1) writeScheduleToRTDB(2, 1, "NOW") else Toast.makeText(this, "Chamber 2 Empty!", Toast.LENGTH_SHORT).show() }
        binding.btnViewHistory.setOnClickListener { startActivity(Intent(this, AdherenceActivity::class.java).apply { putExtra("PATIENT_ID", patientId); putExtra("PATIENT_NAME", "My") }) }
        binding.btnSaveCaregiver.setOnClickListener { saveCaregiverId(binding.etCaregiverId.text.toString().trim()) }
        binding.btnChat.setOnClickListener { openChat() }
        binding.btnAiCompanion.setOnClickListener { startActivity(Intent(this, AiCompanionActivity::class.java)) }

        binding.btnSOS.setOnClickListener { toggleEmergencyState() }
    }
    private fun listenForIncomingMessages() {
        val myId = auth.currentUser?.uid ?: return

        dbFirestore.collection("chats")
            .whereEqualTo("receiverId", myId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null || snapshots.isEmpty) return@addSnapshotListener

                for (doc in snapshots.documentChanges) {
                    if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val msg = doc.document.toObject(Message::class.java)
                        // Check if message is recent ( < 10 seconds old) to avoid spamming on startup
                        if (System.currentTimeMillis() - (msg.timestamp?.time ?: 0) < 10000) {
                            NotificationHelper.showNewMessageNotification(this, "Caregiver", msg.text)
                        }
                    }
                }
            }
    }
    private fun checkHealthConnectStatusAndPrompt() {
        val status = HealthConnectClient.getSdkStatus(this, providerPackageName)
        if (status == HealthConnectClient.SDK_AVAILABLE) {
            isHealthConnectAvailable = true
            healthConnectClient = HealthConnectClient.getOrCreate(this)
            binding.btnConnectWatch.text = "Sync Watch Data"
            checkPermissionsSilent()
        } else {
            isHealthConnectAvailable = false
            binding.btnConnectWatch.text = "Simulate Watch Data"
        }
    }
    private fun checkPermissionsSilent() {
        lifecycleScope.launch { try { if (healthConnectClient.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)) readVitalsFromWatch() } catch (e: Exception) {} }
    }
    private fun showHealthConnectRationaleAndRequest() {
        AlertDialog.Builder(this).setTitle("Connect Watch").setMessage("Allow MedMind to read Heart Rate & Steps?").setPositiveButton("OK") { _, _ -> requestPermissions.launch(PERMISSIONS) }.setNegativeButton("No", null).show()
    }
    private fun openHealthConnectAppSettings() {
        try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:$providerPackageName") }) } catch (e: Exception) { openHealthConnectPlayStore() }
    }
    private fun openHealthConnectPlayStore() {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$providerPackageName"))) } catch (e: Exception) {}
    }
    private fun readVitalsFromWatch() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Syncing...", Toast.LENGTH_SHORT).show()
                val now = Instant.now(); val startTime = now.minus(24, ChronoUnit.HOURS)
                val hrResponse = healthConnectClient.readRecords(ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(startTime, now)))
                val oxyResponse = healthConnectClient.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, TimeRangeFilter.between(startTime, now)))
                val stepsResponse = healthConnectClient.readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant(), now)))

                updateVitalsUI(hrResponse.records.lastOrNull()?.samples?.lastOrNull()?.beatsPerMinute, oxyResponse.records.lastOrNull()?.percentage?.value, stepsResponse.records.sumOf { it.count })
                saveVitalsToFirestore(hrResponse.records.lastOrNull()?.samples?.lastOrNull()?.beatsPerMinute, oxyResponse.records.lastOrNull()?.percentage?.value, stepsResponse.records.sumOf { it.count })
            } catch (e: Exception) { simulateVitals() }
        }
    }
    private fun simulateVitals() {
        val hr = Random.nextLong(65, 95); val oxy = Random.nextDouble(96.0, 99.0); val steps = Random.nextLong(1500, 8000)
        updateVitalsUI(hr, oxy, steps); saveVitalsToFirestore(hr, oxy, steps)
        Toast.makeText(this, "Vitals Simulated", Toast.LENGTH_SHORT).show()
    }
    private fun updateVitalsUI(hr: Long?, oxy: Double?, steps: Long?) {
        binding.tvHeartRate.text = "Heart Rate: ${hr ?: "--"} BPM"
        binding.tvBloodOxygen.text = "Blood Oxygen: ${if (oxy != null) String.format("%.1f", oxy) else "--"} %"
        binding.tvSteps.text = "Steps Today: ${steps ?: "--"}"
    }
    private fun saveVitalsToFirestore(hr: Long?, oxy: Double?, steps: Long?) {
        dbFirestore.collection("vitals").document(patientId!!).set(VitalsData(System.currentTimeMillis(), hr, oxy, steps))
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_CODE)
        }
    }
    private fun requestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
    }
    private fun listenForSchedules(uid: String) {
        dbRealtime.getReference("schedules/$uid").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scheduleList.clear()
                cancelAllAlarms()
                for (child in snapshot.children) {
                    val schedule = child.getValue(Schedule::class.java)
                    if (schedule != null && schedule.type != "NOW") {
                        scheduleList.add(schedule)
                        schedulePhoneAlarms(schedule)
                    }
                }
                scheduleAdapter = ScheduleAdapter(scheduleList) { scheduleToDelete ->
                    cancelSchedule(scheduleToDelete)
                }
                binding.rvSchedules.adapter = scheduleAdapter
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun schedulePhoneAlarms(schedule: Schedule) {
        if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) return
        for (time in schedule.times) {
            val intent = Intent(this, ReminderReceiver::class.java).apply { putExtra("PILL_NAME", schedule.pillName); putExtra("SERVO_NUM", schedule.servo); putExtra("PATIENT_ID", patientId); putExtra("PILL_COUNT", schedule.pills) }
            val pi = PendingIntent.getBroadcast(this, (schedule.id+time.hour).hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, time.hour); set(Calendar.MINUTE, time.minute); set(Calendar.SECOND, 0) }
            if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_MONTH, 1)
            try { alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi) } catch (e: SecurityException) {}
        }
    }
    private fun cancelAllAlarms() { for (intent in pendingIntents.values) alarmManager.cancel(intent); pendingIntents.clear() }
    private fun createNotificationChannel() { if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("MEDMIND_CHANNEL_ID", "MedMind", NotificationManager.IMPORTANCE_HIGH)) }
    private fun sendRefillNotification(s: Int, c: Int) { val nm = getSystemService(NotificationManager::class.java); val b = NotificationCompat.Builder(this, "MEDMIND_CHANNEL_ID").setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("Refill needed!").setContentText("Chamber $s is running low ($c pills left)."); nm.notify(999+s, b.build()) }
    private fun loadPatientData(uid: String) { dbFirestore.collection("users").document(uid).addSnapshotListener { s, _ -> currentUser = s?.toObject(User::class.java); if (currentUser != null) { binding.tvPillCount1.text = "Ch 1: ${currentUser!!.pillsChamber1}"; binding.tvPillCount2.text = "Ch 2: ${currentUser!!.pillsChamber2}"; binding.tvUserId.text = "ID: $uid"; binding.tvCurrentCaregiver.text = "Caregiver: ${currentUser?.caregiverId}"; if(currentUser!!.pillsChamber1<=5) sendRefillNotification(1, currentUser!!.pillsChamber1); if(currentUser!!.pillsChamber2<=5) sendRefillNotification(2, currentUser!!.pillsChamber2); if(currentUser!!.isEmergencyActive) { binding.btnSOS.text="STOP ALARM"; binding.btnSOS.setBackgroundColor(getColor(android.R.color.holo_green_dark)) } else { binding.btnSOS.text="SOS"; binding.btnSOS.setBackgroundColor(getColor(android.R.color.holo_red_dark)) } } } }
    private fun toggleEmergencyState() { val ns = !(currentUser?.isEmergencyActive?:false); dbFirestore.collection("users").document(patientId!!).update("isEmergencyActive", ns) }
    private fun saveCaregiverId(id: String) { if (id.isNotEmpty()) dbFirestore.collection("users").document(patientId!!).update("caregiverId", id).addOnSuccessListener { Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show() } }
    private fun openChat() { val cid = currentUser?.caregiverId; if (cid != null && cid != "NOT_SET") startActivity(Intent(this, ChatActivity::class.java).apply { putExtra("RECEIVER_ID", cid); putExtra("RECEIVER_NAME", "Caregiver") }) else Toast.makeText(this, "Save Caregiver ID first", Toast.LENGTH_SHORT).show() }
    private fun updatePillCount(s: Int, c: Int) { val f = if (s==1) "pillsChamber1" else "pillsChamber2"; dbFirestore.collection("users").document(patientId!!).update(f, c) }
    private fun cancelSchedule(schedule: Schedule) { dbRealtime.getReference("schedules").child(patientId!!).child(schedule.id).removeValue() }
    private fun writeScheduleToRTDB(servo: Int, pills: Int, type: String) { val id = dbRealtime.getReference("schedules").push().key ?: ""; val s = Schedule(id, patientId!!, "Manual", servo, pills, type, startDate=System.currentTimeMillis(), times=listOf(SimpleTime(-1,-1))); dbRealtime.getReference("schedules").child(patientId!!).child(id).setValue(s); if(type=="NOW") { val f = if (servo==1) "pillsChamber1" else "pillsChamber2"; dbFirestore.collection("users").document(patientId!!).update(f, FieldValue.increment(-pills.toLong())); Toast.makeText(this, "Dispensing...", Toast.LENGTH_SHORT).show() } }
    private fun goToLogin() { startActivity(Intent(this, LoginActivity::class.java)); finish() }
}