package com.ranni.app.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    exerciseId: Long,
    onBack: () -> Unit
) {
    // Load the exercise when the screen is shown
    LaunchedEffect(exerciseId) { viewModel.load(exerciseId) }

    // Stop any running timers/alarms when leaving the screen (covers system back, rotation, etc.)
    DisposableEffect(viewModel) {
        onDispose { viewModel.stopAlarms() }
    }

    val phase by viewModel.phase.collectAsState()
    val exerciseName by viewModel.exerciseName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exerciseName) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.stopAlarms(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val p = phase) {
                is SessionPhase.Loading -> CircularProgressIndicator()

                is SessionPhase.SetReady -> PhaseLayout(
                    subtitle = "Set ${p.currentSet} of ${p.totalSets}",
                    mainText = "Ready",
                    buttonText = "Start Set",
                    onButton = { viewModel.startSet() }
                )

                is SessionPhase.SetActive -> {
                    if (p.remainingSeconds != null) {
                        PhaseLayout(
                            subtitle = "Set ${p.currentSet} of ${p.totalSets}",
                            mainText = formatTime(p.remainingSeconds),
                            buttonText = null,
                            onButton = {}
                        )
                    } else {
                        PhaseLayout(
                            subtitle = "Set ${p.currentSet} of ${p.totalSets}",
                            mainText = "Go",
                            buttonText = "Done — Start Rest",
                            onButton = { viewModel.finishSet() }
                        )
                    }
                }

                is SessionPhase.RestReady -> SetCompleteLayout(
                    currentSet = p.currentSet,
                    onStartRest = { viewModel.beginRest() }
                )

                is SessionPhase.RestActive -> PhaseLayout(
                    subtitle = "Rest",
                    mainText = formatTime(p.remainingSeconds),
                    buttonText = null,
                    onButton = {}
                )

                is SessionPhase.Complete -> CompleteScreen { viewModel.stopAlarms(); onBack() }
            }
        }
    }
}

@Composable
private fun PhaseLayout(
    subtitle: String,
    mainText: String,
    buttonText: String?,
    onButton: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text(subtitle, style = MaterialTheme.typography.titleMedium)
        Text(mainText, fontSize = 56.sp, fontWeight = FontWeight.Bold)
        if (buttonText != null) {
            Button(onClick = onButton, modifier = Modifier.fillMaxWidth()) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun SetCompleteLayout(currentSet: Int, onStartRest: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            "Set $currentSet Complete",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Rest",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStartRest, modifier = Modifier.fillMaxWidth()) {
            Text("Start Rest")
        }
    }
}

@Composable
private fun CompleteScreen(onDone: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Exercise Complete!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s.toString().padStart(2, '0')}s" else "${s}s"
}
