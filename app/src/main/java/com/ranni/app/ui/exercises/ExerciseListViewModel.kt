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

    val exercises: StateFlow<List<Exercise>> = repo.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(exercise: Exercise) {
        viewModelScope.launch { repo.delete(exercise) }
    }
}
