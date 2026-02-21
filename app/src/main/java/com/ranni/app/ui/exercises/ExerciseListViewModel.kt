package com.ranni.app.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.app.data.model.Exercise
import com.ranni.app.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseListViewModel(private val repo: ExerciseRepository) : ViewModel() {

    // Exercises ordered by orderIndex (DB column), then name for ties
    val exercises: StateFlow<List<Exercise>> = repo.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(exercise: Exercise) {
        viewModelScope.launch { repo.delete(exercise) }
    }

    /**
     * Moves the exercise at [fromIndex] to [toIndex], then persists the full new order
     * to the DB by reassigning orderIndex values for all affected exercises.
     */
    fun moveExercise(fromIndex: Int, toIndex: Int) {
        val current = exercises.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        viewModelScope.launch { repo.updateOrder(current) }
    }
}
