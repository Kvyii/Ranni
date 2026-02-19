package com.ranni.app.ui.exercises

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseScreen(
    viewModel: EditExerciseViewModel,
    exerciseId: Long?,
    onBack: () -> Unit
) {
    LaunchedEffect(exerciseId) {
        exerciseId?.let { viewModel.loadExercise(it) }
    }

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (exerciseId == null) "New Exercise" else "Edit Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.update(state.copy(name = it)) },
                label = { Text("Exercise Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.sets,
                    onValueChange = { viewModel.update(state.copy(sets = it)) },
                    label = { Text("Sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.reps,
                    onValueChange = { viewModel.update(state.copy(reps = it)) },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Timed sets", modifier = Modifier.weight(1f))
                Switch(
                    checked = state.hasSetDuration,
                    onCheckedChange = { viewModel.update(state.copy(hasSetDuration = it)) }
                )
            }
            if (state.hasSetDuration) {
                OutlinedTextField(
                    value = state.setDurationSeconds,
                    onValueChange = { viewModel.update(state.copy(setDurationSeconds = it)) },
                    label = { Text("Set Duration (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = state.restDurationSeconds,
                onValueChange = { viewModel.update(state.copy(restDurationSeconds = it)) },
                label = { Text("Rest Duration (seconds)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.save(onBack) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.name.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}
