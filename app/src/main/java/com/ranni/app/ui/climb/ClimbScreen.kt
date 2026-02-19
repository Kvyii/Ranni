package com.ranni.app.ui.climb

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranni.app.data.model.RouteColor
import com.ranni.app.data.model.gyms

@Composable
fun ClimbScreen(viewModel: ClimbViewModel) {
    var expandedGym by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<RouteColor?>(null) }
    var showLiarDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (showLiarDialog) {
        AlertDialog(
            onDismissRequest = { showLiarDialog = false },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Liar!!", fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLiarDialog = false }) {
                    Text("Okay I lied")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gyms.forEach { gym ->
                if (gym.comingSoon) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(gym.name, style = MaterialTheme.typography.titleMedium)
                            Text("Coming soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    val isExpanded = expandedGym == gym.name
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedGym = if (isExpanded) null else gym.name }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(gym.name, style = MaterialTheme.typography.titleMedium)
                                Icon(
                                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column {
                                    gym.routes.forEach { rc ->
                                        ColorRow(
                                            routeColor = rc,
                                            isSelected = selectedColor == rc,
                                            onClick = {
                                                val wasSelected = selectedColor == rc
                                                selectedColor = if (wasSelected) null else rc
                                                if (rc.grade == "V12" && !wasSelected) showLiarDialog = true
                                            }
                                        )
                                    }
                                }
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
