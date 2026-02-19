package com.ranni.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ranni.app.data.db.AppDatabase
import com.ranni.app.data.repository.ExerciseRepository
import com.ranni.app.data.repository.SessionRepository
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

@Composable
fun MainScaffold() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val exerciseRepo = remember { ExerciseRepository(db.exerciseDao()) }
    val sessionRepo = remember { SessionRepository(db.sessionLogDao()) }

    var selectedTab by remember { mutableIntStateOf(0) }
    // navigation state for exercise list sub-screens
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.ExerciseList) }

    Scaffold(
        bottomBar = {
            // Only show bottom bar on top-level screens
            if (screenState is ScreenState.ExerciseList || screenState is ScreenState.History) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; screenState = ScreenState.ExerciseList },
                        icon = { Icon(Icons.Default.List, null) },
                        label = { Text("Exercises") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; screenState = ScreenState.History },
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
                is ScreenState.History -> {
                    val vm = remember { HistoryViewModel(sessionRepo) }
                    HistoryScreen(vm)
                }
            }
        }
    }
}

sealed class ScreenState {
    object ExerciseList : ScreenState()
    data class EditExercise(val exerciseId: Long?) : ScreenState()
    data class Session(val exerciseId: Long) : ScreenState()
    object History : ScreenState()
}
