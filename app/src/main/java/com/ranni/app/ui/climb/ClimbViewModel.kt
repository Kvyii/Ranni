package com.ranni.app.ui.climb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.app.data.model.ClimbType
import com.ranni.app.data.repository.ClimbRepository
import kotlinx.coroutines.launch

class ClimbViewModel(private val repo: ClimbRepository) : ViewModel() {
    // Log a climb with the specified type — score should already have the multiplier applied
    fun logClimb(color: String, score: Int, climbType: ClimbType = ClimbType.NEW) {
        viewModelScope.launch { repo.logClimb(color, score, climbType) }
    }
}
