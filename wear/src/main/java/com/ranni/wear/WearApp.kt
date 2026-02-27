package com.ranni.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.ranni.wear.ui.ClimbTypeScreen
import com.ranni.wear.ui.ConfirmationScreen
import com.ranni.wear.ui.GymPickerScreen
import com.ranni.wear.ui.LogResult
import com.ranni.wear.ui.RoutePickerScreen
import com.ranni.wear.ui.WearViewModel

private const val NAV_GYM_PICKER   = "gym_picker"
private const val NAV_ROUTE_PICKER = "route_picker"
private const val NAV_CLIMB_TYPE   = "climb_type"
private const val NAV_CONFIRMATION = "confirmation"

@Composable
fun WearApp(viewModel: WearViewModel) {
    val isLoaded     by viewModel.isLoaded.collectAsState()
    val favouriteGym by viewModel.favouriteGym.collectAsState()
    val logResult    by viewModel.logResult.collectAsState()

    // Lock the start destination once loading completes — never changes after that,
    // so nav stack references remain stable across recompositions.
    var startDest by remember { mutableStateOf(NAV_GYM_PICKER) }
    LaunchedEffect(isLoaded) {
        if (isLoaded) {
            startDest = if (!favouriteGym.isNullOrEmpty()) NAV_ROUTE_PICKER else NAV_GYM_PICKER
        }
    }

    val navController = rememberSwipeDismissableNavController()

    // Navigate to confirmation exactly once when a result arrives, then ignore until reset
    LaunchedEffect(logResult) {
        if (logResult != LogResult.NONE) {
            navController.navigate(NAV_CONFIRMATION) {
                popUpTo(startDest) { inclusive = true }
            }
        }
    }

    SwipeDismissableNavHost(
        navController    = navController,
        startDestination = startDest
    ) {
        composable(NAV_GYM_PICKER) {
            GymPickerScreen(
                viewModel    = viewModel,
                onGymSelected = { gymName ->
                    viewModel.selectGym(gymName)
                    navController.navigate(NAV_ROUTE_PICKER)
                }
            )
        }

        composable(NAV_ROUTE_PICKER) {
            RoutePickerScreen(
                viewModel      = viewModel,
                onRouteSelected = { route ->
                    viewModel.selectRoute(route)
                    navController.navigate(NAV_CLIMB_TYPE)
                }
            )
        }

        composable(NAV_CLIMB_TYPE) {
            ClimbTypeScreen(
                // Navigation to confirmation is driven by the logResult LaunchedEffect above
                onTypeSelected = { climbType -> viewModel.logClimb(climbType) }
            )
        }

        composable(NAV_CONFIRMATION) {
            ConfirmationScreen(
                logResult = logResult,
                onDismiss = {
                    // Reset result first so the LaunchedEffect won't re-navigate
                    viewModel.resetForNewClimb()
                    // Navigate back to the route picker (or gym picker) as a fresh destination
                    navController.navigate(startDest) {
                        popUpTo(NAV_CONFIRMATION) { inclusive = true }
                    }
                }
            )
        }
    }
}
