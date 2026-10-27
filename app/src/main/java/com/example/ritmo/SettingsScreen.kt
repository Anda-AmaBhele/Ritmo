package com.example.ritmo.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TAG = "RitmoSettings"

// Ritmo palette (sage green + cream) from the Canva mockups
private val Sage = Color(0xFF8A9A7B)
private val Cream = Color(0xFFF8F6F1)
private val Ink = Color(0xFF2B2B2B)

/**
 * Simple wrapper around SharedPreferences so every screen reads the
 * user's settings from one place.
 * Reference: Android Developers (2024) Save key-value data with SharedPreferences.
 * https://developer.android.com/training/data-storage/shared-preferences
 */
object RitmoPrefs {
    private const val FILE = "ritmo_prefs"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_24H = "use_24h_time"
    private const val KEY_LANGUAGE = "language"

    // Languages offered in the app (English + two South African languages)
    val languages = listOf("English", "isiZulu", "Afrikaans")

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun notificationsEnabled(context: Context) =
        prefs(context).getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS, value).apply()
        Log.d(TAG, "Notifications enabled set to $value")
    }

    fun use24Hour(context: Context) =
        prefs(context).getBoolean(KEY_24H, true)

    fun setUse24Hour(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_24H, value).apply()
        Log.d(TAG, "24-hour time set to $value")
    }

    fun language(context: Context) =
        prefs(context).getString(KEY_LANGUAGE, "English") ?: "English"

    fun setLanguage(context: Context, value: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, value).apply()
        Log.d(TAG, "Language set to $value")
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Load the saved values once, then keep them in Compose state so the UI updates instantly
    var notifications by remember { mutableStateOf(RitmoPrefs.notificationsEnabled(context)) }
    var use24h by remember { mutableStateOf(RitmoPrefs.use24Hour(context)) }
    var language by remember { mutableStateOf(RitmoPrefs.language(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Sage)
        }

        // Notifications
        SectionTitle("Notifications")
        SettingSwitchRow(
            title = "Push notifications",
            subtitle = "Alerts for new posts and messages",
            checked = notifications,
            onCheckedChange = {
                notifications = it
                RitmoPrefs.setNotificationsEnabled(context, it)
            }
        )
        Divider(color = Sage.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

        // Time format
        SectionTitle("Time")
        SettingSwitchRow(
            title = "24-hour time",
            subtitle = "Turn off for 12-hour time",
            checked = use24h,
            onCheckedChange = {
                use24h = it
                RitmoPrefs.setUse24Hour(context, it)
            }
        )
        Divider(color = Sage.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

        // Language
        SectionTitle("Language")
        RitmoPrefs.languages.forEach { lang ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        language = lang
                        RitmoPrefs.setLanguage(context, lang)
                    }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = language == lang,
                    onClick = {
                        language = lang
                        RitmoPrefs.setLanguage(context, lang)
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = Sage)
                )
                Text(lang, color = Ink, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Sage,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Ink)
            Text(subtitle, fontSize = 13.sp, color = Ink.copy(alpha = 0.6f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Sage)
        )
    }
}