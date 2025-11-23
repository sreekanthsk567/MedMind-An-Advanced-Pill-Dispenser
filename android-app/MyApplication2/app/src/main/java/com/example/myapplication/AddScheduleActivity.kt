package com.example.myapplication
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityAddScheduleBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
class AddScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddScheduleBinding
    private val auth = Firebase.auth
    private val dbRealtime = Firebase.database
    private val dbFirestore = Firebase.firestore
    private val patientId = auth.currentUser?.uid ?: ""
    private var selectedStartDate = Calendar.getInstance()
    private var selectedEndDate: Calendar? = null
    private val selectedTimes = mutableListOf<SimpleTime>()
    private lateinit var timesAdapter: ArrayAdapter<String>
    private val selectedDays = mutableSetOf<String>()
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupSpinners()
        setupDatePickers()
        setupTimePicker()
        setupDayToggles()
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSaveSchedule.setOnClickListener { saveSchedule() }
    }

    private fun setupSpinners() {
        val frequencies = arrayOf("Daily", "Specific Days")
        val freqAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, frequencies)
        binding.spinnerFrequency.adapter = freqAdapter
        binding.spinnerFrequency.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View, position: Int, id: Long) {
                binding.layoutSpecificDays.visibility = if (position == 1) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
        val chambers = arrayOf("Chamber 1", "Chamber 2")
        val chamberAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, chambers)
        binding.spinnerChamber.adapter = chamberAdapter
    }

    private fun setupDayToggles() {
        val dayToggles = mapOf(
            binding.toggleMon to "MON", binding.toggleTue to "TUE", binding.toggleWed to "WED",
            binding.toggleThu to "THU", binding.toggleFri to "FRI", binding.toggleSat to "SAT",
            binding.toggleSun to "SUN"
        )
        dayToggles.forEach { (toggle, day) ->
            toggle.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedDays.add(day) else selectedDays.remove(day)
            }
        }
    }

    private fun setupDatePickers() {
        val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US)
        binding.etStartDate.setText(dateFormat.format(selectedStartDate.time))
        binding.etStartDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedStartDate.set(y, m, d); binding.etStartDate.setText(dateFormat.format(selectedStartDate.time))
            }, selectedStartDate.get(Calendar.YEAR), selectedStartDate.get(Calendar.MONTH), selectedStartDate.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding.cbEndDate.setOnCheckedChangeListener { _, isChecked ->
            binding.etEndDate.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) selectedEndDate = null
        }
        binding.etEndDate.setOnClickListener {
            val cal = selectedEndDate ?: Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
            DatePickerDialog(this, { _, y, m, d ->
                selectedEndDate = Calendar.getInstance().apply { set(y, m, d) }; binding.etEndDate.setText(dateFormat.format(selectedEndDate!!.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.minDate = selectedStartDate.timeInMillis; show()
            }
        }
    }
    private fun setupTimePicker() {
        timesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.lvTimes.adapter = timesAdapter
        binding.btnAddTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                val newTime = SimpleTime(h, m); selectedTimes.add(newTime)
                timesAdapter.add(String.format(Locale.US, "%02d:%02d", h, m)); timesAdapter.notifyDataSetChanged()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }
        binding.lvTimes.setOnItemClickListener { _, _, pos, _ ->
            selectedTimes.removeAt(pos); timesAdapter.remove(timesAdapter.getItem(pos)); timesAdapter.notifyDataSetChanged()
        }
    }
    private fun saveSchedule() {
        val pillName = binding.etPillName.text.toString()
        val pillsPerDose = binding.etPillQuantity.text.toString().toIntOrNull()
        val servo = binding.spinnerChamber.selectedItemPosition + 1
        val type = if (binding.spinnerFrequency.selectedItemPosition == 0) "DAILY" else "SPECIFIC_DAYS"
        if (pillName.isEmpty() || pillsPerDose == null || pillsPerDose <= 0 || selectedTimes.isEmpty()) {
            Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }
        if (type == "SPECIFIC_DAYS" && selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
            return
        }
        val daysList = if (type == "DAILY") listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN") else selectedDays.toList()

        selectedStartDate.set(Calendar.HOUR_OF_DAY, 0); selectedStartDate.set(Calendar.MINUTE, 0)
        selectedEndDate?.set(Calendar.HOUR_OF_DAY, 23); selectedEndDate?.set(Calendar.MINUTE, 59)
        val scheduleRef = dbRealtime.getReference("schedules").child(patientId).push()
        val scheduleId = scheduleRef.key ?: "sched_${System.currentTimeMillis()}"
        val newSchedule = Schedule(
            id = scheduleId, userId = patientId, pillName = pillName, servo = servo, pills = pillsPerDose,
            type = type, daysOfWeek = daysList, startDate = selectedStartDate.timeInMillis,
            endDate = selectedEndDate?.timeInMillis, times = selectedTimes, lastDispenseTimestamp = 0L
        )
        scheduleRef.setValue(newSchedule)
            .addOnSuccessListener {
                Toast.makeText(this, "Schedule saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}