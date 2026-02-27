package com.ranni.wear.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay

// Displays "Logged!" on success or "Failed — check phone" on error.
// Auto-dismisses after 2 seconds by calling onDismiss, which resets the ViewModel
// and pops the nav back to the route picker so the user can log another climb.
@Composable
fun ConfirmationScreen(logResult: LogResult, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2_000L)
        onDismiss()
    }
    Box(
        modifier            = Modifier.fillMaxSize(),
        contentAlignment    = Alignment.Center
    ) {
        Text(
            if (logResult == LogResult.SUCCESS) "Logged!" else "Failed\ncheck phone"
        )
    }
}
