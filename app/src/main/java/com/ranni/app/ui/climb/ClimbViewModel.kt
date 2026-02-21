package com.ranni.app.ui.climb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.app.data.GymOrderPreferences
import com.ranni.app.data.model.ClimbType
import com.ranni.app.data.model.Gym
import com.ranni.app.data.model.InjurySeverity
import com.ranni.app.data.model.gyms
import com.ranni.app.data.repository.ClimbRepository
import com.ranni.app.data.repository.InjuryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClimbViewModel(
    private val repo: ClimbRepository,
    private val injuryRepo: InjuryRepository,
    private val gymOrderPrefs: GymOrderPreferences
) : ViewModel() {

    // Active gyms are reorderable; coming-soon are locked at the bottom and never change
    private val allActiveGyms: List<Gym> = gyms.filter { !it.comingSoon }
    val comingSoonGyms: List<Gym> = gyms.filter { it.comingSoon }

    // Ordered active gyms — initialised from saved prefs, falls back to default order
    private val _orderedActiveGyms = MutableStateFlow(loadOrderedGyms())
    val orderedActiveGyms: StateFlow<List<Gym>> = _orderedActiveGyms.asStateFlow()

    // Log a climb with the specified type — score should already have the multiplier applied
    fun logClimb(color: String, gymName: String, score: Int, climbType: ClimbType = ClimbType.NEW) {
        viewModelScope.launch { repo.logClimb(color, gymName, score, climbType) }
    }

    // Log an injury marker for today with the given severity
    fun logInjury(severity: InjurySeverity) {
        viewModelScope.launch { injuryRepo.logInjury(severity) }
    }

    /**
     * Moves the active gym at [fromIndex] to [toIndex], updates the state flow,
     * and persists the new order to SharedPreferences immediately.
     */
    fun moveGym(fromIndex: Int, toIndex: Int) {
        val current = _orderedActiveGyms.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _orderedActiveGyms.value = current
        gymOrderPrefs.setOrder(current.map { it.name })
    }

    /**
     * Reads the saved gym order from prefs and maps names back to Gym objects.
     * Any active gym not present in the saved list (e.g. newly added gyms) is appended at the end.
     * Falls back to the default hardcoded order if prefs are empty.
     */
    private fun loadOrderedGyms(): List<Gym> {
        val savedNames = gymOrderPrefs.getOrder()
        if (savedNames.isEmpty()) return allActiveGyms

        val gymMap = allActiveGyms.associateBy { it.name }
        val ordered = savedNames.mapNotNull { gymMap[it] }
        // Append any active gyms added after the prefs were last saved
        val remaining = allActiveGyms.filter { it.name !in savedNames }
        return ordered + remaining
    }
}
