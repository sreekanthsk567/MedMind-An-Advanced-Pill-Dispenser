package com.example.myapplication

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
class MissedDoseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val localizedContext = LocaleHelper.setLocale(context)
        val patientId = intent.getStringExtra("PATIENT_ID") ?: return
        val pillName = intent.getStringExtra("PILL_NAME") ?: "Medicine"
        val servo = intent.getIntExtra("SERVO_NUM", 1)
        val pills = intent.getIntExtra("PILL_COUNT", 1)

        val pendingResult = goAsync()
        Log.d("MissedDoseReceiver", "Checking if $pillName was taken")
        val recentTime = System.currentTimeMillis() - (20 * 60 * 1000)
        val db = Firebase.database.reference
        db.child("adherence_logs")
            .orderByChild("timestamp")
            .startAt(recentTime.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var wasTaken = false
                    for (child in snapshot.children) {
                        val log = child.getValue(AdherenceLog::class.java)
                        if (log != null && log.patientId == patientId &&
                            log.pillName == pillName && log.taken) {
                            wasTaken = true
                            break
                        }
                    }
                    if (wasTaken) {
                        Log.d("MissedDoseReceiver", "Pill was taken. Decrementing count.")
                        updatePillCount(patientId, servo, -pills)
                    } else {
                        Log.w("MissedDoseReceiver", "Pill NOT taken! Sending alert.")
                        sendNotification(
                            localizedContext,
                            "MedMind: Missed Dose!",
                            "You haven't taken your $pillName yet. Please take it now!"
                        )
                    }
                    pendingResult.finish()
                }
                override fun onCancelled(error: DatabaseError) {
                    pendingResult.finish()
                }
            })
    }
    private fun updatePillCount(patientId: String, servoNum: Int, amount: Int) {
        val dbFirestore = Firebase.firestore
        val fieldToUpdate = if (servoNum == 1) "pillsChamber1" else "pillsChamber2"

        dbFirestore.collection("users").document(patientId)
            .update(fieldToUpdate, FieldValue.increment(amount.toLong()))
            .addOnSuccessListener { Log.d("MissedDoseReceiver", "Pill count updated.") }
            .addOnFailureListener { e -> Log.e("MissedDoseReceiver", "Failed to update count", e) }
    }
    private fun sendNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, "MEDMIND_CHANNEL_ID")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}