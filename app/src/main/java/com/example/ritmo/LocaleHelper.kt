package com.example.ritmo

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import java.util.Locale

/**
 * Stores the chosen language and applies it to the app's resources.
 * Reference: Android Developers (2024) Language and locale resources.
 * https://developer.android.com/guide/topics/resources/localization
 */
object LocaleHelper {
    private const val TAG = "RitmoLocale"
    private const val FILE = "ritmo_prefs"
    private const val KEY = "language"

    // English + two South African languages (brief requires at least two)
    val languages = listOf("English", "isiZulu", "isiXhosa")

    private fun code(name: String) = when (name) {
        "isiZulu" -> "zu"
        "isiXhosa" -> "xh"
        else -> "en"
    }

    fun getSaved(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, "English") ?: "English"

    fun save(context: Context, name: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, name).apply()
        Log.d(TAG, "Language saved: $name")
    }

    // Called from MainActivity.attachBaseContext so every stringResource() uses the saved language
    fun wrap(base: Context): Context {
        val locale = Locale(code(getSaved(base)))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}