package com.ranni.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ranni.app.data.db.AppDatabase
import com.ranni.app.data.repository.ClimbRepository
import com.ranni.app.data.repository.ExerciseRepository
import com.ranni.app.data.repository.SessionRepository
import com.ranni.app.ui.about.AboutScreen
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

    var selectedTab by remember { mutableIntStateOf(0) }
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.Climb) }

    val isTopLevel = screenState is ScreenState.ExerciseList
            || screenState is ScreenState.Climb
            || screenState is ScreenState.History

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = {
                            screenState = when (screenState) {
                                is ScreenState.About -> ScreenState.ExerciseList
                                is ScreenState.EditExercise -> ScreenState.ExerciseList
                                is ScreenState.Session -> ScreenState.ExerciseList
                                else -> ScreenState.ExerciseList
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isTopLevel) {
                        IconButton(onClick = { screenState = ScreenState.About }) {
                            Icon(Icons.Default.Info, contentDescription = "About")
                        }
                    }
                }
            )
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
                    val vm = remember { HistoryViewModel(sessionRepo) }
                    HistoryScreen(vm)
                }
                is ScreenState.About -> {
                    AboutScreen()
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
}
