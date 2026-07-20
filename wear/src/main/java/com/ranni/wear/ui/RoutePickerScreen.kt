package com.ranni.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.ranni.wear.data.RouteColor

// Shows the routes for the currently selected gym.
// Each chip shows a coloured dot (the route's actual colour) and the route name only.
@Composable
fun RoutePickerScreen(
    viewModel: WearViewModel,
    onRouteSelected: (RouteColor) -> Unit
) {
    val gymName by viewModel.selectedGym.collectAsState()
    val gym     = viewModel.activeGyms.firstOrNull { it.name == gymName }

    // Defensive: shouldn't happen after a gym has been selected
    if (gym == null) return

    ScalingLazyColumn(
        modifier            = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(gym.label)
        }
        items(gym.routes) { route ->
            Chip(
                modifier = Modifier.fillMaxWidth(0.9f),
                onClick  = { onRouteSelected(route) },
                label    = { Text(route.name) },
                // Coloured circle matching the route's gym colour
                icon     = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(route.color)
                    )
                },
                colors   = ChipDefaults.primaryChipColors()
            )
        }
    }
}
