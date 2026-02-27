package com.ranni.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ranni.app.data.model.gyms
import com.ranni.app.ui.components.ClimbDot

@Composable
fun ScoresScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scoring info — plain, multipliers highlighted in accent colour
        val accentColor = MaterialTheme.colorScheme.secondaryContainer
        val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Information", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = subtleColor)) { append("• New climbs are scored at ") }
                    withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("1.0×") }
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = subtleColor)) { append("• Flashes are scored at ") }
                    withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("1.25×") }
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = subtleColor)) { append("• Repeats are scored at ") }
                    withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("0.75×") }
                },
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(4.dp))

        gyms.forEach { gym ->
            if (gym.comingSoon) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
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
                var expanded by remember { mutableStateOf(false) }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(gym.name, style = MaterialTheme.typography.titleMedium)
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column {
                                gym.routes.forEach { route ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Color swatch — hollow or filled based on gym, via ClimbDot
                                        ClimbDot(gymName = gym.name, routeName = route.name, size = 28.dp, strokeWidth = 2.dp, showFilledBorder = true)
                                        // Route name — takes up available space
                                        Text(
                                            route.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Grade — fixed width so all grades align
                                        Text(
                                            route.grade,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(64.dp),
                                            textAlign = TextAlign.End
                                        )
                                        // Points — right-aligned with monospace for consistent digit width
                                        Text(
                                            "${route.score} pts".padStart(8),
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(72.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
