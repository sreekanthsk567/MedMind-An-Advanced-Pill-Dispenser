package com.example.myapplication
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityLanguagePickerBinding
class LanguagePickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguagePickerBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguagePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val currentLang = LocaleHelper.getLanguage(this)
        if (currentLang.isNotEmpty()) {
            goToLogin()
            return
        }
        binding.btnEnglish.setOnClickListener { setLanguageAndProceed("en") }
        binding.btnHindi.setOnClickListener { setLanguageAndProceed("hi") }
        binding.btnMalayalam.setOnClickListener { setLanguageAndProceed("ml") }
        binding.btnKannada.setOnClickListener { setLanguageAndProceed("kn") }
    }
    private fun setLanguageAndProceed(langCode: String) {
        LocaleHelper.setLocale(this, langCode)
        val intent = baseContext.packageManager.getLaunchIntentForPackage(
            baseContext.packageName
        )
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}