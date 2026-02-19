package com.ranni.app.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.app.data.model.Exercise
import com.ranni.app.data.repository.ExerciseRepository
import com.ranni.app.data.repository.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SessionPhase {
    object Loading : SessionPhase()
    data class SetReady(val currentSet: Int, val totalSets: Int) : SessionPhase()
    data class SetActive(val currentSet: Int, val totalSets: Int, val remainingSeconds: Int?) : SessionPhase()
    data class RestReady(val currentSet: Int, val totalSets: Int) : SessionPhase()
    data class RestActive(val currentSet: Int, val totalSets: Int, val remainingSeconds: Int) : SessionPhase()
    object Complete : SessionPhase()
}

class SessionViewModel(
    private val exerciseRepo: ExerciseRepository,
    private val sessionRepo: SessionRepository,
    context: Context
) : ViewModel() {

    private val alarmPlayer = AlarmPlayer(context.applicationContext)

    private val _phase = MutableStateFlow<SessionPhase>(SessionPhase.Loading)
    val phase: StateFlow<SessionPhase> = _phase.asStateFlow()

    private val _exerciseName = MutableStateFlow("")
    val exerciseName: StateFlow<String> = _exerciseName.asStateFlow()

    private var exercise: Exercise? = null
    private var currentSet = 1
    private var timerJob: Job? = null

    fun load(exerciseId: Long) {
        viewModelScope.launch {
            val ex = exerciseRepo.getById(exerciseId) ?: return@launch
            exercise = ex
            _exerciseName.value = ex.name
            currentSet = 1
            _phase.value = SessionPhase.SetReady(currentSet, ex.sets)
        }
    }

    fun startSet() {
        val ex = exercise ?: return
        alarmPlayer.stopRest()
        val timed = ex.setDurationSeconds
        if (timed != null) {
            _phase.value = SessionPhase.SetActive(currentSet, ex.sets, timed)
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                var remaining = timed
                while (remaining > 0) {
                    delay(1000)
                    remaining--
                    _phase.value = SessionPhase.SetActive(currentSet, ex.sets, remaining)
                }
                // auto-transition to rest without requiring user tap
                alarmPlayer.playSetComplete()
                startRest(autoStart = true)
            }
        } else {
            _phase.value = SessionPhase.SetActive(currentSet, ex.sets, null)
        }
    }

    fun finishSet() {
        // Called by user when exercise has no set duration
        timerJob?.cancel()
        startRest()
    }

    private fun startRest(autoStart: Boolean = false) {
        val ex = exercise ?: return
        if (currentSet >= ex.sets) {
            logAndComplete(ex.name)
            return
        }
        if (autoStart) {
            beginRest()
        } else {
            _phase.value = SessionPhase.RestReady(currentSet, ex.sets)
        }
    }

    fun beginRest() {
        val ex = exercise ?: return
        _phase.value = SessionPhase.RestActive(currentSet, ex.sets, ex.restDurationSeconds)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = ex.restDurationSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _phase.value = SessionPhase.RestActive(currentSet, ex.sets, remaining)
            }
            // advance
            alarmPlayer.playRestComplete()
            currentSet++
            if (currentSet > ex.sets) {
                logAndComplete(ex.name)
            } else {
                _phase.value = SessionPhase.SetReady(currentSet, ex.sets)
            }
        }
    }

    fun stopAlarms() {
        timerJob?.cancel()
        alarmPlayer.stopAll()
    }

    private fun logAndComplete(name: String) {
        viewModelScope.launch {
            sessionRepo.logSession(name)
            _phase.value = SessionPhase.Complete
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        alarmPlayer.release()
    }
}
