package com.ranni.app.data

import android.content.Context
import android.net.Uri

/**
 * SharedPreferences wrapper for custom alarm sound settings.
 * Stores persistable URIs picked via the SAF document picker.
 */
class AlarmPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the custom rest alarm URI, or null if using the default. */
    fun getCustomRestAlarmUri(): Uri? {
        val uriString = prefs.getString(KEY_REST_ALARM_URI, null)
        return uriString?.let { Uri.parse(it) }
    }

    /** Saves a custom rest alarm URI, or clears it (pass null to reset to default). */
    fun setCustomRestAlarmUri(uri: Uri?) {
        prefs.edit().apply {
            if (uri != null) {
                putString(KEY_REST_ALARM_URI, uri.toString())
            } else {
                remove(KEY_REST_ALARM_URI)
            }
            apply()
        }
    }

    /** Returns true if a custom rest alarm has been set. */
    fun hasCustomRestAlarm(): Boolean = prefs.contains(KEY_REST_ALARM_URI)

    companion object {
        private const val PREFS_NAME = "alarm_prefs"
        private const val KEY_REST_ALARM_URI = "rest_alarm_uri"
    }
}
