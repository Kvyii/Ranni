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

    // "Liar" dialog for V12 routes
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

    // Log confirmation dialog shown when a route is tapped
    if (selectedColor != null) {
        val color = selectedColor!!
        AlertDialog(
            onDismissRequest = { selectedColor = null },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Log climb?", style = MaterialTheme.typography.titleMedium)
                }
            },
            confirmButton = {
                // Full-width centered confirm button
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = {
                        viewModel.logClimb(color.name, color.score)
                        selectedColor = null
                    }) {
                        Text("Log")
                    }
                }
            }
        )
    }

    Scaffold(
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
            // Header text prompting the user to pick a climb
            Text(
                "Select climb",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

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
                                    .clickable {
                                        expandedGym = if (isExpanded) null else gym.name
                                        // Clear route selection when switching gyms
                                        selectedColor = null
                                    }
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
                                            onClick = {
                                                // Show liar dialog for V12, otherwise show log confirmation
                                                if (rc.grade == "V12") showLiarDialog = true
                                                else selectedColor = rc
                                            }
                                        )
                                    }
                                }
                            }
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Color swatch circle
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
            modifier = Modifier.weight(1f)
        )
        Text(
            routeColor.grade,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
