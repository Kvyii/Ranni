package com.ranni.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ranni.app.data.SharedPrefsGymOrderPreferences
import com.ranni.app.data.db.AppDatabase
import com.ranni.app.data.repository.ClimbRepository
import com.ranni.app.data.repository.ExerciseRepository
import com.ranni.app.data.repository.MetricsRepository
import com.ranni.app.data.repository.InjuryRepository
import com.ranni.app.data.repository.SessionRepository
import com.ranni.app.ui.exercises.EditExerciseScreen
import com.ranni.app.ui.exercises.EditExerciseViewModel
import com.ranni.app.ui.exercises.ExerciseListScreen
import com.ranni.app.ui.exercises.ExerciseListViewModel
import com.ranni.app.ui.history.HistoryScreen
import com.ranni.app.ui.history.HistoryViewModel
import com.ranni.app.ui.session.SessionScreen
import com.ranni.app.ui.session.SessionViewModel

@Composable
fun RanniNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val db = remember { AppDatabase.getInstance(context) }
    val exerciseRepo = remember { ExerciseRepository(db.exerciseDao()) }
    val sessionRepo = remember { SessionRepository(db.sessionLogDao()) }
    val climbRepo = remember { ClimbRepository(db.climbLogDao()) }
    val metricsRepo = remember { MetricsRepository(db.metricsConfigDao()) }
    val injuryRepo = remember { InjuryRepository(db.injuryLogDao()) }

    NavHost(navController = navController, startDestination = "exercises") {
        composable("exercises") {
            val vm = remember { ExerciseListViewModel(exerciseRepo) }
            ExerciseListScreen(
                viewModel = vm,
                onAddExercise = { navController.navigate("edit") },
                onEditExercise = { id -> navController.navigate("edit/$id") },
                onStartSession = { id -> navController.navigate("session/$id") }
            )
        }
        composable(
            "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val id = backStack.arguments?.getLong("id")
            val vm = remember { EditExerciseViewModel(exerciseRepo) }
            EditExerciseScreen(vm, id, onBack = { navController.popBackStack() })
        }
        composable("edit") {
            val vm = remember { EditExerciseViewModel(exerciseRepo) }
            EditExerciseScreen(vm, null, onBack = { navController.popBackStack() })
        }
        composable(
            "session/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val id = backStack.arguments!!.getLong("id")
            val vm = remember { SessionViewModel(exerciseRepo, sessionRepo, context) }
            SessionScreen(vm, id, onBack = { navController.popBackStack() })
        }
        composable("history") {
            val vm = remember { HistoryViewModel(sessionRepo, climbRepo, metricsRepo, injuryRepo, SharedPrefsGymOrderPreferences(context)) }
            HistoryScreen(vm)
        }
    }
}
