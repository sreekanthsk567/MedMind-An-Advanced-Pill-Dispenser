package com.example.myapplication
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
class PrivacyPolicyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply {
            text = "MedMind Privacy Policy:\n\nWe use Health Connect to read your Heart Rate, Steps, and Oxygen levels to provide better health insights to your doctor.\n\nYour data is stored securely in Firebase and is only accessible by you and your authorized caregiver."
            textSize = 18f
            setPadding(32, 32, 32, 32)
        }
        setContentView(textView)
    }
}