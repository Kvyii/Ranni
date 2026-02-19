package com.ranni.app.ui.climb

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private data class RouteColor(val name: String, val grade: String, val color: Color, val score: Int)

private val gymColors = listOf(
    RouteColor("Green",  "VB",      Color(0xFF60B555),   75),
    RouteColor("Blue",   "V0",      Color(0xFF4279C7),  100),
    RouteColor("Teal",   "V1 - V2", Color(0xFF42B5C7),  150),
    RouteColor("Pink",   "V2 - V3", Color(0xFFDB72CD),  250),
    RouteColor("Orange", "V3 - V4", Color(0xFFDB8272),  450),
    RouteColor("Black",  "V5 - V6", Color(0xFF050101),  700),
    RouteColor("Purple", "V6 - V8", Color(0xFF6A3CBA), 1000),
    RouteColor("White",  "V7+",     Color(0xFFEDEDED), 1300),
)

@Composable
fun ClimbScreen(viewModel: ClimbViewModel) {
    var gymExpanded by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf<RouteColor?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gym menu
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { gymExpanded = !gymExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("9 Degrees", style = MaterialTheme.typography.titleMedium)
                        Icon(
                            if (gymExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = gymExpanded) {
                        Column {
                            gymColors.forEach { rc ->
                                ColorRow(
                                    routeColor = rc,
                                    isSelected = selectedColor == rc,
                                    onClick = {
                                        selectedColor = if (selectedColor == rc) null else rc
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Confirm button — only visible when a color is selected
            AnimatedVisibility(visible = selectedColor != null) {
                val color = selectedColor
                if (color != null) {
                    var confirming by remember(color) { mutableStateOf(false) }
                    Button(
                        onClick = {
                            confirming = true
                            viewModel.logClimb(color.name, color.score)
                            selectedColor = null
                        },
                        enabled = !confirming,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log")
                    }

                    LaunchedEffect(confirming) {
                        if (confirming) {
                            snackbarHostState.showSnackbar("${color.name} logged")
                            confirming = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorRow(
    routeColor: RouteColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(routeColor.color)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
        Text(
            routeColor.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
        Text(
            routeColor.grade,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
