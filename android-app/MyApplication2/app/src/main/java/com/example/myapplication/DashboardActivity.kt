package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityDashboardBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val userId = auth.currentUser?.uid ?: ""
    private lateinit var patientAdapter: PatientAdapter
    private val patientList = mutableListOf<User>()
    private val patientListeners = mutableListOf<ListenerRegistration>()
    private var mediaPlayer: MediaPlayer? = null
    private var isAlarmPlaying = false
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        NotificationHelper.createChannel(this)
        binding.btnLogout.setOnClickListener { auth.signOut(); startActivity(Intent(this, LoginActivity::class.java)); finish() }
        setupRecyclerView()
        loadMyPatients()
        binding.btnAddPatient.setOnClickListener { if (binding.etPatientId.text.toString().isNotEmpty()) addPatient(binding.etPatientId.text.toString().trim()) }
        listenForIncomingMessages()
    }
    private fun listenForIncomingMessages() {
        val myId = auth.currentUser?.uid ?: return
        db.collection("chats")
            .whereEqualTo("receiverId", myId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null || snapshots.isEmpty) return@addSnapshotListener
                for (doc in snapshots.documentChanges) {
                    if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val msg = doc.document.toObject(Message::class.java)
                        if (System.currentTimeMillis() - (msg.timestamp?.time ?: 0) < 10000) {
                            NotificationHelper.showNewMessageNotification(this, "Patient", msg.text)
                        }
                    }
                }
            }
    }

    override fun onDestroy() { super.onDestroy(); stopAlarm(); patientListeners.forEach { it.remove() } }
    private fun setupRecyclerView() {
        patientAdapter = PatientAdapter(patientList,
            onAdherenceClick = { patient -> startActivity(Intent(this, AdherenceActivity::class.java).apply { putExtra("PATIENT_ID", patient.uid); putExtra("PATIENT_NAME", patient.fullName) }) },
            onChatClick = { patient -> startActivity(Intent(this, ChatActivity::class.java).apply { putExtra("RECEIVER_ID", patient.uid); putExtra("RECEIVER_NAME", patient.fullName) }) }
        )
        binding.rvPatients.layoutManager = LinearLayoutManager(this)
        binding.rvPatients.adapter = patientAdapter
    }
    private fun loadMyPatients() {
        db.collection("users").document(userId).addSnapshotListener { snapshot, _ ->
            val pIds = snapshot?.toObject(User::class.java)?.patientIds ?: emptyList()
            if (pIds.isEmpty()) { binding.tvNoPatients.visibility = View.VISIBLE; binding.rvPatients.visibility = View.GONE; return@addSnapshotListener }
            binding.tvNoPatients.visibility = View.GONE; binding.rvPatients.visibility = View.VISIBLE
            patientListeners.forEach { it.remove() }; patientListeners.clear(); patientList.clear(); patientAdapter.notifyDataSetChanged()
            for (pid in pIds) {
                val reg = db.collection("users").document(pid).addSnapshotListener { pDoc, _ ->
                    if (pDoc != null && pDoc.exists()) {
                        val pUser = pDoc.toObject(User::class.java)
                        if (pUser != null) {
                            val idx = patientList.indexOfFirst { it.uid == pUser.uid }
                            if (idx != -1) { patientList[idx] = pUser; patientAdapter.notifyItemChanged(idx) } else { patientList.add(pUser); patientAdapter.notifyItemInserted(patientList.size - 1) }
                            if (pUser.isEmergencyActive) triggerAlarm(pUser.fullName)
                        }
                    }
                }
                patientListeners.add(reg)
            }
        }
    }
    private fun triggerAlarm(name: String) { if(isAlarmPlaying) return; try { mediaPlayer = MediaPlayer().apply { setDataSource(this@DashboardActivity, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)); isLooping = true; prepare(); start() }; isAlarmPlaying = true; AlertDialog.Builder(this).setTitle("EMERGENCY!").setMessage("$name sent SOS!").setCancelable(false).setNegativeButton("STOP") { _, _ -> stopAlarm() }.show() } catch (e: Exception) {} }
    private fun stopAlarm() { mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null; isAlarmPlaying = false }
    private fun addPatient(id: String) { db.collection("users").document(id).get().addOnSuccessListener { if (it.exists()) db.collection("users").document(userId).update("patientIds", com.google.firebase.firestore.FieldValue.arrayUnion(id)).addOnSuccessListener { Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show(); binding.etPatientId.text.clear() } else Toast.makeText(this, "Not Found", Toast.LENGTH_SHORT).show() } }
}