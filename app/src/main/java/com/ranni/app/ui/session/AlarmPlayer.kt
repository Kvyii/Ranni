package com.ranni.app.ui.session

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.ranni.app.R
import com.ranni.app.data.AlarmPreferences

class AlarmPlayer(private val context: Context) {
    private var setPlayer: MediaPlayer? = null
    private var restPlayer: MediaPlayer? = null
    private val alarmPrefs = AlarmPreferences(context)

    fun playSetComplete() {
        setPlayer = MediaPlayer.create(context, R.raw.alarm_set)?.also {
            it.setOnCompletionListener { mp -> mp.release(); setPlayer = null }
            it.start()
        }
    }

    fun playRestComplete() {
        val customUri = alarmPrefs.getCustomRestAlarmUri()
        restPlayer = if (customUri != null) {
            // Try to play the user-picked custom alarm sound
            createPlayerFromUri(customUri)
        } else {
            null
        }

        // Fall back to the bundled default if no custom URI or if it failed
        if (restPlayer == null) {
            restPlayer = MediaPlayer.create(context, R.raw.alarm_rest)
        }

        restPlayer?.also {
            it.setOnCompletionListener { mp -> mp.release(); restPlayer = null }
            it.start()
        }
    }

    /**
     * Creates a MediaPlayer from a persistable SAF content URI.
     * Returns null if the URI can't be played (file deleted, permission lost, etc.).
     */
    private fun createPlayerFromUri(uri: Uri): MediaPlayer? {
        return try {
            MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
            }
        } catch (e: Exception) {
            Log.w("AlarmPlayer", "Custom alarm URI failed, falling back to default", e)
            null
        }
    }

    fun stopSet() {
        setPlayer?.stop()
        setPlayer?.release()
        setPlayer = null
    }

    fun stopRest() {
        restPlayer?.stop()
        restPlayer?.release()
        restPlayer = null
    }

    fun stopAll() {
        stopSet()
        stopRest()
    }

    fun release() {
        setPlayer?.release(); setPlayer = null
        restPlayer?.release(); restPlayer = null
    }
}
