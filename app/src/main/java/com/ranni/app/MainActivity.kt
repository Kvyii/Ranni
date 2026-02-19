package com.ranni.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ranni.app.data.db.AppDatabase
import com.ranni.app.data.repository.ClimbRepository
import com.ranni.app.data.repository.ExerciseRepository
import com.ranni.app.data.repository.MetricsRepository
import com.ranni.app.data.repository.SessionRepository
import com.ranni.app.ui.about.AboutContent
import com.ranni.app.ui.about.SettingsScreen
import com.ranni.app.ui.climb.ClimbScreen
import com.ranni.app.ui.climb.ClimbViewModel
import com.ranni.app.ui.exercises.EditExerciseScreen
import com.ranni.app.ui.exercises.EditExerciseViewModel
import com.ranni.app.ui.exercises.ExerciseListScreen
import com.ranni.app.ui.exercises.ExerciseListViewModel
import com.ranni.app.ui.history.HistoryScreen
import com.ranni.app.ui.history.HistoryViewModel
import com.ranni.app.ui.session.SessionScreen
import com.ranni.app.ui.session.SessionViewModel
import com.ranni.app.ui.settings.InfoScreen
import com.ranni.app.ui.settings.MetricsScreen
import com.ranni.app.ui.settings.DeveloperScreen
import com.ranni.app.ui.settings.MetricsViewModel
import com.ranni.app.ui.theme.RanniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RanniTheme {
                MainScaffold()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val exerciseRepo = remember { ExerciseRepository(db.exerciseDao()) }
    val sessionRepo = remember { SessionRepository(db.sessionLogDao()) }
    val climbRepo = remember { ClimbRepository(db.climbLogDao()) }
    val metricsRepo = remember { MetricsRepository(db.metricsConfigDao()) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.Climb) }

    val isTopLevel = screenState is ScreenState.ExerciseList
            || screenState is ScreenState.Climb
            || screenState is ScreenState.History

    Scaffold(
        topBar = {
            if (isTopLevel) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { screenState = ScreenState.About }) {
                        Icon(Icons.Default.Settings, contentDescription = "About", modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        Text(when (screenState) {
                            is ScreenState.About -> "Settings"
                            is ScreenState.SettingsInfo -> "Info"
                            is ScreenState.SettingsMetrics -> "Configure Metrics"
                            is ScreenState.SettingsAbout -> "About"
                            is ScreenState.SettingsDev -> "Developer"
                            else -> ""
                        })
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            screenState = when (screenState) {
                                is ScreenState.About -> when (selectedTab) {
                                    0 -> ScreenState.Climb
                                    1 -> ScreenState.ExerciseList
                                    2 -> ScreenState.History
                                    else -> ScreenState.ExerciseList
                                }
                                is ScreenState.SettingsInfo,
                                is ScreenState.SettingsMetrics,
                                is ScreenState.SettingsAbout,
                                is ScreenState.SettingsDev -> ScreenState.About
                                else -> ScreenState.ExerciseList
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; screenState = ScreenState.Climb },
                        icon = { Icon(Icons.Default.Star, null) },
                        label = { Text("Climb") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; screenState = ScreenState.ExerciseList },
                        icon = { Icon(Icons.Default.List, null) },
                        label = { Text("Exercises") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2; screenState = ScreenState.History },
                        icon = { Icon(Icons.Default.DateRange, null) },
                        label = { Text("History") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = screenState) {
                is ScreenState.ExerciseList -> {
                    val vm = remember { ExerciseListViewModel(exerciseRepo) }
                    ExerciseListScreen(
                        viewModel = vm,
                        onAddExercise = { screenState = ScreenState.EditExercise(null) },
                        onEditExercise = { id -> screenState = ScreenState.EditExercise(id) },
                        onStartSession = { id -> screenState = ScreenState.Session(id) }
                    )
                }
                is ScreenState.EditExercise -> {
                    val vm = remember(s.exerciseId) { EditExerciseViewModel(exerciseRepo) }
                    EditExerciseScreen(
                        viewModel = vm,
                        exerciseId = s.exerciseId,
                        onBack = { screenState = ScreenState.ExerciseList }
                    )
                }
                is ScreenState.Session -> {
                    val vm = remember(s.exerciseId) { SessionViewModel(exerciseRepo, sessionRepo, context) }
                    SessionScreen(
                        viewModel = vm,
                        exerciseId = s.exerciseId,
                        onBack = { screenState = ScreenState.ExerciseList }
                    )
                }
                is ScreenState.Climb -> {
                    val vm = remember { ClimbViewModel(climbRepo) }
                    ClimbScreen(vm)
                }
                is ScreenState.History -> {
                    val vm = remember { HistoryViewModel(sessionRepo, climbRepo, metricsRepo) }
                    HistoryScreen(vm)
                }
                is ScreenState.About -> {
                    SettingsScreen(
                        onNavigateInfo = { screenState = ScreenState.SettingsInfo },
                        onNavigateMetrics = { screenState = ScreenState.SettingsMetrics },
                        onNavigateAbout = { screenState = ScreenState.SettingsAbout },
                        onNavigateDev = { screenState = ScreenState.SettingsDev },
                        showDevTools = BuildConfig.SHOW_DEV_TOOLS
                    )
                }
                is ScreenState.SettingsInfo -> {
                    InfoScreen()
                }
                is ScreenState.SettingsMetrics -> {
                    val vm = remember { MetricsViewModel(metricsRepo) }
                    MetricsScreen(vm)
                }
                is ScreenState.SettingsAbout -> {
                    AboutContent()
                }
                is ScreenState.SettingsDev -> {
                    DeveloperScreen(db)
                }
            }
        }
    }
}

sealed class ScreenState {
    object ExerciseList : ScreenState()
    data class EditExercise(val exerciseId: Long?) : ScreenState()
    data class Session(val exerciseId: Long) : ScreenState()
    object Climb : ScreenState()
    object History : ScreenState()
    object About : ScreenState()
    object SettingsInfo : ScreenState()
    object SettingsMetrics : ScreenState()
    object SettingsAbout : ScreenState()
    object SettingsDev : ScreenState()
}
