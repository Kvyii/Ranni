package com.ranni.app.ui.climb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.app.data.model.ClimbType
import com.ranni.app.data.model.InjurySeverity
import com.ranni.app.data.repository.ClimbRepository
import com.ranni.app.data.repository.InjuryRepository
import kotlinx.coroutines.launch

class ClimbViewModel(
    private val repo: ClimbRepository,
    private val injuryRepo: InjuryRepository
) : ViewModel() {
    // Log a climb with the specified type — score should already have the multiplier applied
    fun logClimb(color: String, score: Int, climbType: ClimbType = ClimbType.NEW) {
        viewModelScope.launch { repo.logClimb(color, score, climbType) }
    }

    // Log an injury marker for today with the given severity
    fun logInjury(severity: InjurySeverity) {
        viewModelScope.launch { injuryRepo.logInjury(severity) }
    }
}
