package com.ranni.app.ui.climb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.app.data.repository.ClimbRepository
import kotlinx.coroutines.launch

class ClimbViewModel(private val repo: ClimbRepository) : ViewModel() {
    fun logClimb(color: String, score: Int) {
        viewModelScope.launch { repo.logClimb(color, score) }
    }
}
