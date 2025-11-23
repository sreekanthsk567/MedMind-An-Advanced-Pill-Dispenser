package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale
object LocaleHelper {
    private const val PREFS_NAME = "MedMindPrefs"
    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"
    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }
    fun setLocale(context: Context): Context {
        val language = getLanguage(context)
        return updateResources(context, language)
    }
    fun getLanguage(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(SELECTED_LANGUAGE, "") ?: ""
    }
    private fun persist(context: Context, language: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SELECTED_LANGUAGE, language).apply()
    }
    private fun updateResources(context: Context, language: String): Context {
        if (language.isEmpty()) {
            return context
        }
        val locale = Locale(language)
        Locale.setDefault(locale)

        val res: Resources = context.resources
        val config = Configuration(res.configuration)

        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}