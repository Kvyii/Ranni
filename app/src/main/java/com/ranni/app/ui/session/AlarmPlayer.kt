package com.ranni.app.ui.session

import android.content.Context
import android.media.MediaPlayer
import com.ranni.app.R

class AlarmPlayer(private val context: Context) {
    private var setPlayer: MediaPlayer? = null
    private var restPlayer: MediaPlayer? = null

    fun playSetComplete() {
        setPlayer = MediaPlayer.create(context, R.raw.alarm_set)?.also {
            it.setOnCompletionListener { mp -> mp.release(); setPlayer = null }
            it.start()
        }
    }

    fun playRestComplete() {
        restPlayer = MediaPlayer.create(context, R.raw.alarm_rest)?.also {
            it.setOnCompletionListener { mp -> mp.release(); restPlayer = null }
            it.start()
        }
    }

    fun stopRest() {
        restPlayer?.stop()
        restPlayer?.release()
        restPlayer = null
    }

    fun release() {
        setPlayer?.release(); setPlayer = null
        restPlayer?.release(); restPlayer = null
    }
}
