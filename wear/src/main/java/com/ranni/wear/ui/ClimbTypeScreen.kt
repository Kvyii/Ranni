package com.ranni.wear.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text

// Three chips for NEW, FLASH, REPEAT — consistent with gym and route picker screens.
@Composable
fun ClimbTypeScreen(onTypeSelected: (String) -> Unit) {
    ScalingLazyColumn(
        modifier            = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Text("Climb type") }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(0.9f),
                onClick  = { onTypeSelected("NEW") },
                label    = { Text("New  1x") },
                colors   = ChipDefaults.primaryChipColors()
            )
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(0.9f),
                onClick  = { onTypeSelected("FLASH") },
                label    = { Text("Flash  1.25x") },
                colors   = ChipDefaults.primaryChipColors()
            )
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(0.9f),
                onClick  = { onTypeSelected("REPEAT") },
                label    = { Text("Repeat  0.75x") },
                colors   = ChipDefaults.primaryChipColors()
            )
        }
    }
}
