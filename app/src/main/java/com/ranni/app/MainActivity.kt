package com.ranni.app

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.res.painterResource
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.ranni.app.data.repository.InjuryRepository
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
import com.ranni.app.ui.settings.ScoresScreen
import com.ranni.app.ui.settings.MetricsScreen
import com.ranni.app.ui.settings.DeveloperScreen
import com.ranni.app.ui.settings.MetricsViewModel
import com.ranni.app.ui.settings.SoundsScreen
import com.ranni.app.data.AlarmPreferences
import com.ranni.app.data.GymOrderPreferences
import com.ranni.app.ui.theme.RanniTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Hold the system splash screen (with app icon) for at least 3 seconds
        val startTime = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            SystemClock.uptimeMillis() - startTime < 2000L
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RanniTheme {
                MainScaffold()
            }
        }
    }
}

@Composable
fun MainScaffold() {
    val context = LocalContext.current
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.Loading) }

    // DB reference, initialized during splash loading
    var db by remember { mutableStateOf<AppDatabase?>(null) }

    if (screenState is ScreenState.Loading) {
        // Initialize DB while the system splash screen is shown
        LaunchedEffect(Unit) {
            db = async(Dispatchers.IO) { AppDatabase.getInstance(context) }.await()
            screenState = ScreenState.Climb
        }
    } else {
        // DB is guaranteed non-null after loading completes
        MainContent(
            db = db!!,
            context = context,
            screenState = screenState,
            onScreenStateChange = { screenState = it }
        )
    }
}

/**
 * Main app content with bottom nav, top bar, and all screen routing.
 * Shown after the splash/loading screen completes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    db: AppDatabase,
    context: android.content.Context,
    screenState: ScreenState,
    onScreenStateChange: (ScreenState) -> Unit
) {
    val exerciseRepo = remember { ExerciseRepository(db.exerciseDao()) }
    val sessionRepo = remember { SessionRepository(db.sessionLogDao()) }
    val climbRepo = remember { ClimbRepository(db.climbLogDao()) }
    val metricsRepo = remember { MetricsRepository(db.metricsConfigDao()) }
    val injuryRepo = remember { InjuryRepository(db.injuryLogDao()) }
    val alarmPrefs = remember { AlarmPreferences(context) }
    val gymOrderPrefs = remember { GymOrderPreferences(context) }

    // Track the display name of the custom rest alarm (null = default)
    var customRestAlarmName by remember { mutableStateOf(getAlarmDisplayName(context, alarmPrefs)) }

    // SAF file picker for choosing a custom alarm sound
    val alarmPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // Check file size — reject files larger than 20 MB
            val sizeBytes = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (_: Exception) { 0L }

            if (sizeBytes > MAX_ALARM_FILE_BYTES) {
                Toast.makeText(context, "File too large (max 20 MB)", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }

            // Take persistable permission so the URI survives app restarts
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            alarmPrefs.setCustomRestAlarmUri(uri)
            customRestAlarmName = getAlarmDisplayName(context, alarmPrefs)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    val isTopLevel = screenState is ScreenState.ExerciseList
            || screenState is ScreenState.Climb
            || screenState is ScreenState.History

    // Handle system back button — mirror the toolbar back arrow behavior
    BackHandler(enabled = !isTopLevel) {
        onScreenStateChange(when (screenState) {
            is ScreenState.SettingsScores,
            is ScreenState.SettingsMetrics,
            is ScreenState.SettingsSounds,
            is ScreenState.SettingsAbout,
            is ScreenState.SettingsDev -> ScreenState.About

            is ScreenState.About -> when (selectedTab) {
                0 -> ScreenState.Climb
                1 -> ScreenState.ExerciseList
                2 -> ScreenState.History
                else -> ScreenState.ExerciseList
            }

            is ScreenState.EditExercise,
            is ScreenState.Session -> ScreenState.ExerciseList

            else -> ScreenState.ExerciseList
        })
    }

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
                    IconButton(onClick = { onScreenStateChange(ScreenState.About) }) {
                        Icon(Icons.Default.Settings, contentDescription = "About", modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        Text(when (screenState) {
                            is ScreenState.About -> "Settings"
                            is ScreenState.SettingsScores -> "Scores"
                            is ScreenState.SettingsMetrics -> "Configure Metrics"
                            is ScreenState.SettingsSounds -> "Sounds"
                            is ScreenState.SettingsAbout -> "About"
                            is ScreenState.SettingsDev -> "Developer"
                            else -> ""
                        })
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            onScreenStateChange(when (screenState) {
                                is ScreenState.About -> when (selectedTab) {
                                    0 -> ScreenState.Climb
                                    1 -> ScreenState.ExerciseList
                                    2 -> ScreenState.History
                                    else -> ScreenState.ExerciseList
                                }
                                is ScreenState.SettingsScores,
                                is ScreenState.SettingsMetrics,
                                is ScreenState.SettingsSounds,
                                is ScreenState.SettingsAbout,
                                is ScreenState.SettingsDev -> ScreenState.About
                                else -> ScreenState.ExerciseList
                            })
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
                        onClick = { selectedTab = 0; onScreenStateChange(ScreenState.Climb) },
                        icon = { Icon(painterResource(R.drawable.shoe_icon), null, modifier = Modifier.size(24.dp)) },
                        label = { Text("Climb") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; onScreenStateChange(ScreenState.ExerciseList) },
                        icon = { Icon(Icons.Default.List, null) },
                        label = { Text("Exercises") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2; onScreenStateChange(ScreenState.History) },
                        icon = { Icon(Icons.Default.DateRange, null) },
                        label = { Text("History") }
                    )
                }
            }
        }
    ) { padding ->
        // Animated screen transitions: slide for depth changes, crossfade for same-level
        AnimatedContent(
            targetState = screenState,
            modifier = Modifier.fillMaxSize().padding(padding),
            transitionSpec = {
                val fromDepth = initialState.depth
                val toDepth = targetState.depth
                if (fromDepth == toDepth) {
                    // Same-level navigation (tab switches): crossfade
                    fadeIn() togetherWith fadeOut()
                } else if (toDepth > fromDepth) {
                    // Going deeper: slide in from right
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    // Going back: slide in from left
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            label = "screenTransition"
        ) { target ->
            when (val s = target) {
                is ScreenState.ExerciseList -> {
                    val vm = remember { ExerciseListViewModel(exerciseRepo) }
                    ExerciseListScreen(
                        viewModel = vm,
                        onAddExercise = { onScreenStateChange(ScreenState.EditExercise(null)) },
                        onEditExercise = { id -> onScreenStateChange(ScreenState.EditExercise(id)) },
                        onStartSession = { id -> onScreenStateChange(ScreenState.Session(id)) }
                    )
                }
                is ScreenState.EditExercise -> {
                    val vm = remember(s.exerciseId) { EditExerciseViewModel(exerciseRepo) }
                    EditExerciseScreen(
                        viewModel = vm,
                        exerciseId = s.exerciseId,
                        onBack = { onScreenStateChange(ScreenState.ExerciseList) }
                    )
                }
                is ScreenState.Session -> {
                    val vm = remember(s.exerciseId) { SessionViewModel(exerciseRepo, sessionRepo, context) }
                    SessionScreen(
                        viewModel = vm,
                        exerciseId = s.exerciseId,
                        onBack = { onScreenStateChange(ScreenState.ExerciseList) }
                    )
                }
                is ScreenState.Climb -> {
                    val vm = remember { ClimbViewModel(climbRepo, injuryRepo, gymOrderPrefs) }
                    ClimbScreen(vm)
                }
                is ScreenState.History -> {
                    val vm = remember { HistoryViewModel(sessionRepo, climbRepo, metricsRepo, injuryRepo) }
                    HistoryScreen(vm)
                }
                is ScreenState.About -> {
                    SettingsScreen(
                        onNavigateScores = { onScreenStateChange(ScreenState.SettingsScores) },
                        onNavigateMetrics = { onScreenStateChange(ScreenState.SettingsMetrics) },
                        onNavigateSounds = { onScreenStateChange(ScreenState.SettingsSounds) },
                        onNavigateAbout = { onScreenStateChange(ScreenState.SettingsAbout) },
                        onNavigateDev = { onScreenStateChange(ScreenState.SettingsDev) },
                        showDevTools = BuildConfig.SHOW_DEV_TOOLS
                    )
                }
                is ScreenState.SettingsSounds -> {
                    SoundsScreen(
                        customRestAlarmName = customRestAlarmName,
                        onPickRestAlarm = { alarmPickerLauncher.launch(arrayOf("audio/*")) },
                        onResetRestAlarm = {
                            alarmPrefs.setCustomRestAlarmUri(null)
                            customRestAlarmName = null
                        }
                    )
                }
                is ScreenState.SettingsScores -> {
                    ScoresScreen()
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
                is ScreenState.Loading -> { /* Handled in MainScaffold */ }
            }
        }
    }
}

/** Maximum allowed file size for custom alarm sounds (20 MB). */
private const val MAX_ALARM_FILE_BYTES = 20L * 1024 * 1024

/**
 * Resolves the display name for the currently saved custom alarm URI.
 * Returns null if no custom alarm is set, or the file name from the content provider.
 */
private fun getAlarmDisplayName(context: android.content.Context, prefs: AlarmPreferences): String? {
    val uri = prefs.getCustomRestAlarmUri() ?: return null
    return try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    } catch (_: Exception) {
        // URI may no longer be accessible
        null
    }
}

sealed class ScreenState {
    object Loading : ScreenState()
    object ExerciseList : ScreenState()
    data class EditExercise(val exerciseId: Long?) : ScreenState()
    data class Session(val exerciseId: Long) : ScreenState()
    object Climb : ScreenState()
    object History : ScreenState()
    object About : ScreenState()
    object SettingsScores : ScreenState()
    object SettingsMetrics : ScreenState()
    object SettingsSounds : ScreenState()
    object SettingsAbout : ScreenState()
    object SettingsDev : ScreenState()

    // Navigation depth used to determine slide direction for transitions
    val depth: Int get() = when (this) {
        is Loading, is Climb, is ExerciseList, is History -> 0
        is About, is EditExercise, is Session -> 1
        is SettingsScores, is SettingsMetrics, is SettingsSounds, is SettingsAbout, is SettingsDev -> 2
    }
}
