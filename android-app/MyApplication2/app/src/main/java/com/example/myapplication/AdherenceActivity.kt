package com.example.myapplication
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityAdherenceBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.Calendar
import java.util.concurrent.TimeUnit
class AdherenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdherenceBinding
    private val dbRealtime = Firebase.database
    private val dbFirestore = Firebase.firestore
    private var patientId: String? = null
    private var patientName: String? = null
    private lateinit var logAdapter: AdherenceAdapter
    private val logList = mutableListOf<AdherenceLog>()
    private val systemMessage = "You are the MedMind AI Companion — a warm, supportive, health-focused assistant inside the MedMind app. You help users stay consistent with their medications by offering gentle reminders, celebrating streaks, motivating habits, and providing emotional support. You speak simply, positively, and empathetically. You avoid medical diagnosis or advice beyond adherence support. You use adherence history, streaks, and daily patterns to provide personalized insights. Always encourage the user kindly, never guilt-trip, and guide them toward steady habit formation.You always try to convey the idea in 2-3 sentences"
    private val generativeModel by lazy {
        val config = generationConfig { temperature = 0.8f }
        GenerativeModel(
            modelName = "gemini-2.5-flash-preview-09-2025",
            apiKey = "Our Private Gemini Key will be inserted here",
            generationConfig = config
        )
    }
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdherenceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        patientId = intent.getStringExtra("PATIENT_ID")
        patientName = intent.getStringExtra("PATIENT_NAME")
        if (patientId == null) { finish(); return }
        binding.tvTitle.text = getString(R.string.adherence_history_title, patientName ?: "My")
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.rvAdherenceLog.layoutManager = LinearLayoutManager(this)

        setupCharts()
        fetchAdherenceLogs()
    }
    private fun setupCharts() {
        binding.pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            holeRadius = 60f
            setUsePercentValues(true)
            setHoleColor(Color.TRANSPARENT)
        }
    }
    private fun fetchAdherenceLogs() {
        binding.progressBar.visibility = View.VISIBLE
        logList.clear()
        dbRealtime.getReference("adherence_logs").orderByChild("patientId").equalTo(patientId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.progressBar.visibility = View.GONE
                    if (!snapshot.exists()) {
                        binding.tvNoHistory.visibility = View.VISIBLE
                        fetchVitalsAndGetSuggestion(0, 0, 0)
                        return
                    }
                    binding.tvNoHistory.visibility = View.GONE
                    val allLogs = mutableListOf<AdherenceLog>()
                    for (child in snapshot.children) {
                        val log = child.getValue(AdherenceLog::class.java)
                        if (log != null) allLogs.add(log)
                    }
                    allLogs.sortByDescending { it.timestamp }
                    var takenCount = 0
                    var missedCount = 0
                    for (log in allLogs) {
                        if (log.taken) takenCount++ else missedCount++
                    }
                    logAdapter = AdherenceAdapter(allLogs)
                    binding.rvAdherenceLog.adapter = logAdapter

                    val streak = calculateAdherenceStreak(allLogs)
                    binding.tvAdherenceStreak.text = "$streak Day Streak"
                    binding.tvDosesTaken.text = "$takenCount Taken"
                    binding.tvDosesMissed.text = "$missedCount Missed"
                    updateProgressRing(streak)
                    updateBadgesAndCelebrate(streak, takenCount + missedCount, takenCount)
                    updatePieChart(takenCount, missedCount)
                    fetchVitalsAndGetSuggestion(takenCount, missedCount, streak)
                }
                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                }
            })
    }
    private fun updateProgressRing(streak: Int) {
        val progress = streak % 7
        val target = 7
        binding.progressRing.max = target
        binding.progressRing.progress = progress
        binding.tvProgressText.text = "$progress/$target"
    }
    private fun updateBadgesAndCelebrate(streak: Int, total: Int, taken: Int) {
        if (streak >= 7) {
            binding.badge7Day.alpha = 1.0f
            if (streak % 7 == 0 && streak > 0) triggerConfetti()
        }
        if (streak >= 30) binding.badge30Day.alpha = 1.0f
        val rate = if (total > 0) taken.toFloat()/total else 0f
        if (rate == 1.0f && total >= 7) binding.badgePerfect.alpha = 1.0f
    }
    private fun triggerConfetti() {
        val party = Party(
            speed = 0f, maxSpeed = 30f, damping = 0.9f, spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.5, 0.3)
        )
        binding.konfettiView.start(party)
        AlertDialog.Builder(this)
            .setTitle("Awesome Streak!")
            .setMessage("You've reached a 7-day streak! Keep up the great work.")
            .setPositiveButton("Let's Go!", null)
            .show()
    }

    private fun fetchVitalsAndGetSuggestion(taken: Int, missed: Int, streak: Int) {
        binding.llAiSuggestion.visibility = View.VISIBLE

        dbFirestore.collection("vitals").document(patientId!!)
            .get()
            .addOnSuccessListener { doc ->
                val vitals = doc.toObject(VitalsData::class.java)
                if(vitals != null) {
                    binding.tvHeartRateDisplay.text = "HR: ${vitals.heartRate ?: "--"}"
                    binding.tvStepsDisplay.text = "Steps: ${vitals.steps ?: "--"}"
                    binding.tvOxygenDisplay.text = "O2: ${vitals.oxygenSaturation ?: "--"}%"
                }
                getAiSuggestion(taken, missed, streak, vitals)
            }
            .addOnFailureListener {
                getAiSuggestion(taken, missed, streak, null)
            }
    }

    private fun getAiSuggestion(taken: Int, missed: Int, streak: Int, vitals: VitalsData?) {
        val prompt = "$systemMessage\nAdherence: $taken taken, $missed missed. Streak: $streak.\nVitals: HR=${vitals?.heartRate} Steps=${vitals?.steps}."
        lifecycleScope.launch {
            try {
                val resp = generativeModel.generateContent(prompt)
                binding.tvAiSuggestion.text = resp.text ?: "No suggestion generated."
            } catch (e: Exception) {
                binding.tvAiSuggestion.text = "Coach offline."
            } finally {
                binding.tvAiSuggestion.visibility = View.VISIBLE
                binding.pbAiSuggestion.visibility = View.GONE
            }
        }
    }

    private fun calculateAdherenceStreak(logs: List<AdherenceLog>): Int {
        if (logs.isEmpty()) return 0
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val map = logs.groupBy {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.DAY_OF_YEAR)
        }
        val todayLogs = map[today]
        val takenToday = todayLogs != null && !todayLogs.any { !it.taken }

        var streak = if (takenToday) 1 else 0
        for (i in 1..7) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val d = cal.get(Calendar.DAY_OF_YEAR)
            val l = map[d]
            if (l != null && l.all { it.taken }) {
                streak++
            } else if (l == null) {
                break
            } else {
                break
            }
        }
        return streak
    }

    private fun updatePieChart(taken: Int, missed: Int) {
        if (taken == 0 && missed == 0) {
            binding.pieChart.visibility = View.INVISIBLE
            return
        }
        binding.pieChart.visibility = View.VISIBLE
        val ds = PieDataSet(listOf(PieEntry(taken.toFloat()), PieEntry(missed.toFloat())), "").apply {
            colors = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#F44336"))
            valueTextColor = Color.WHITE
            valueTextSize = 14f
            valueFormatter = PercentFormatter(binding.pieChart)
        }
        binding.pieChart.data = PieData(ds).apply { setValueTextColor(Color.TRANSPARENT) }
        binding.pieChart.centerText = "${(taken.toFloat() / (taken + missed) * 100).toInt()}%"
        binding.pieChart.setCenterTextColor(Color.WHITE)
        binding.pieChart.setCenterTextSize(20f)
        binding.pieChart.legend.isEnabled = false
        binding.pieChart.description.isEnabled = false
        binding.pieChart.invalidate()
    }
}