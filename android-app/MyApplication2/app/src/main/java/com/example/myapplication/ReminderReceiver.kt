package com.example.myapplication

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val localizedContext = LocaleHelper.setLocale(context)
        val pillName = intent.getStringExtra("PILL_NAME") ?: "Medicine"
        val servo = intent.getIntExtra("SERVO_NUM", 1)
        val patientId = intent.getStringExtra("PATIENT_ID") ?: return
        val pills = intent.getIntExtra("PILL_COUNT", 1)
        Log.d("ReminderReceiver", "ALARM FIRED: Time to take $pillName")
        sendNotification(
            localizedContext,
            "MedMind: Time to take $pillName",
            "It is time for your dose from Chamber $servo."
        )
        scheduleMissedCheck(context, patientId, pillName, servo, pills)
    }
    private fun scheduleMissedCheck(context: Context, patientId: String, pillName: String, servo: Int, pills: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val checkTime = System.currentTimeMillis() + (15 * 60 * 1000)
        val checkIntent = Intent(context, MissedDoseReceiver::class.java).apply {
            putExtra("PATIENT_ID", patientId)
            putExtra("PILL_NAME", pillName)
            putExtra("SERVO_NUM", servo)
            putExtra("PILL_COUNT", pills)
        }
        val requestCode = (pillName + "missed" + System.currentTimeMillis()).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            checkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, checkTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkTime, pendingIntent)
            }
            Log.d("ReminderReceiver", "Scheduled missed dose check for +15 mins")
        } catch (e: SecurityException) {
            Log.e("ReminderReceiver", "Failed to schedule check", e)
        }
    }
    private fun sendNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        val builder = NotificationCompat.Builder(context, "MEDMIND_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
        notificationManager.notify(notificationId, builder.build())
    }
}