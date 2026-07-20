package com.ranni.wear.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text

// Shown when no favourite gym is set.
// Displays all active gyms as tappable chips in a ScalingLazyColumn — the Wear-native
// equivalent of LazyColumn with a perspective-scaling scroll effect.
@Composable
fun GymPickerScreen(
    viewModel: WearViewModel,
    onGymSelected: (String) -> Unit
) {
    ScalingLazyColumn(
        modifier            = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Pick gym")
        }
        items(viewModel.activeGyms) { gym ->
            Chip(
                modifier = Modifier.fillMaxWidth(0.9f),
                onClick  = { onGymSelected(gym.name) },
                label    = { Text(gym.label) },
                colors   = ChipDefaults.primaryChipColors()
            )
        }
    }
}
