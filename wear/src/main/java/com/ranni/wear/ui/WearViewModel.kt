package com.ranni.wear.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.wear.data.Gym
import com.ranni.wear.data.RouteColor
import com.ranni.wear.data.gyms
import com.ranni.wear.data.readFavouriteGym
import com.ranni.wear.data.sendClimbToPhone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LogResult { NONE, SUCCESS, FAILED }

// AndroidViewModel used because DataClient requires an Application context
class WearViewModel(application: Application) : AndroidViewModel(application) {

    // Only active gyms are shown — coming-soon entries are filtered out
    val activeGyms: List<Gym> = gyms.filter { !it.comingSoon }

    // Favourite gym read from DataClient on init; null = not yet loaded or not set
    private val _favouriteGym = MutableStateFlow<String?>(null)
    val favouriteGym: StateFlow<String?> = _favouriteGym.asStateFlow()

    // True once the DataClient read has completed (even if result is null)
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    // Gym the user is currently browsing routes for
    private val _selectedGym = MutableStateFlow<String?>(null)
    val selectedGym: StateFlow<String?> = _selectedGym.asStateFlow()

    // Route the user tapped in RoutePickerScreen
    private val _selectedRoute = MutableStateFlow<RouteColor?>(null)
    val selectedRoute: StateFlow<RouteColor?> = _selectedRoute.asStateFlow()

    // Result of the most recent log attempt
    private val _logResult = MutableStateFlow(LogResult.NONE)
    val logResult: StateFlow<LogResult> = _logResult.asStateFlow()

    init {
        // Read favourite gym from the phone's DataLayer immediately on launch
        viewModelScope.launch {
            val fav = readFavouriteGym(getApplication())
            _favouriteGym.value = fav
            // Pre-select favourite gym so the route picker is shown directly
            if (!fav.isNullOrEmpty()) _selectedGym.value = fav
            _isLoaded.value = true
        }
    }

    fun selectGym(gymName: String) {
        _selectedGym.value = gymName
    }

    fun selectRoute(route: RouteColor) {
        _selectedRoute.value = route
    }

    // Called from ClimbTypeScreen — sends the climb to the phone
    fun logClimb(climbType: String) {
        val gymName   = _selectedGym.value ?: return
        val route     = _selectedRoute.value ?: return
        viewModelScope.launch {
            val ok = sendClimbToPhone(
                context   = getApplication(),
                gymName   = gymName,
                routeName = route.name,
                climbType = climbType
            )
            _logResult.value = if (ok) LogResult.SUCCESS else LogResult.FAILED
        }
    }

    // Reset after confirmation so the user can log another climb in the same session
    fun resetForNewClimb() {
        _selectedRoute.value = null
        _logResult.value     = LogResult.NONE
        // Keep selectedGym so the user lands back on their gym's route picker
    }
}
