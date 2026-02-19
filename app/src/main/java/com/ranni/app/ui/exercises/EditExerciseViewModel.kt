package com.ranni.app.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.app.data.model.Exercise
import com.ranni.app.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditExerciseState(
    val name: String = "",
    val sets: String = "3",
    val hasSetDuration: Boolean = false,
    val setDurationSeconds: String = "30",
    val restDurationSeconds: String = "60"
)

class EditExerciseViewModel(private val repo: ExerciseRepository) : ViewModel() {

    private val _state = MutableStateFlow(EditExerciseState())
    val state: StateFlow<EditExerciseState> = _state.asStateFlow()

    private var editingId: Long? = null

    fun loadExercise(id: Long) {
        viewModelScope.launch {
            val ex = repo.getById(id) ?: return@launch
            editingId = ex.id
            _state.value = EditExerciseState(
                name = ex.name,
                sets = ex.sets.toString(),
                hasSetDuration = ex.setDurationSeconds != null,
                setDurationSeconds = ex.setDurationSeconds?.toString() ?: "30",
                restDurationSeconds = ex.restDurationSeconds.toString()
            )
        }
    }

    fun update(s: EditExerciseState) { _state.value = s }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val exercise = Exercise(
            id = editingId ?: 0,
            name = s.name.trim(),
            sets = s.sets.toIntOrNull() ?: 1,
            setDurationSeconds = if (s.hasSetDuration) s.setDurationSeconds.toIntOrNull() ?: 30 else null,
            restDurationSeconds = s.restDurationSeconds.toIntOrNull() ?: 60
        )
        viewModelScope.launch {
            repo.save(exercise)
            onDone()
        }
    }
}
