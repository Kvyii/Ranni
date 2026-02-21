package com.ranni.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.app.data.model.MetricsConfig
import com.ranni.app.data.repository.MetricsRepository
import com.ranni.app.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MetricsViewModel(private val repo: MetricsRepository) : ViewModel() {
    val config: StateFlow<MetricsConfig> = repo.getConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetricsConfig())

    fun updateMonths(months: Int) {
        val c = config.value
        viewModelScope.launch { repo.updateConfig(c.copy(months = months)) }
    }

    fun updateTopK(topK: Int) {
        val c = config.value
        viewModelScope.launch { repo.updateConfig(c.copy(topK = topK)) }
    }

    fun updateTimeline(timelineMonths: Int) {
        val c = config.value
        viewModelScope.launch { repo.updateConfig(c.copy(timelineMonths = timelineMonths)) }
    }

    fun updateShowClimbDots(show: Boolean) {
        val c = config.value
        viewModelScope.launch { repo.updateConfig(c.copy(showClimbDots = show)) }
    }

    fun updateShowExerciseDots(show: Boolean) {
        val c = config.value
        viewModelScope.launch { repo.updateConfig(c.copy(showExerciseDots = show)) }
    }

    // Persists the selected UI theme name so it survives app restarts.
    fun updateTheme(theme: AppTheme) {
        val c = config.value
        viewModelScope.launch { repo.updateConfig(c.copy(uiTheme = theme.name)) }
    }
}
