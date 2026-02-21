package com.ranni.app.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ranni.app.R

@Composable
fun SettingsScreen(
    onNavigateScores: () -> Unit,
    onNavigateMetrics: () -> Unit,
    onNavigateSounds: () -> Unit,
    onNavigateAbout: () -> Unit,
    onNavigateDev: () -> Unit = {},
    showDevTools: Boolean = false
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsRow("Scores", onClick = onNavigateScores)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SettingsRow("Configure Metrics", onClick = onNavigateMetrics)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SettingsRow("Sounds", onClick = onNavigateSounds)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SettingsRow("About", onClick = onNavigateAbout)
        if (showDevTools) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsRow("Developer", onClick = onNavigateDev)
        }
    }
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AboutContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // App logo and app info with no gap between them
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Crop top-aligned to trim the transparent padding at the bottom of the PNG
            Image(
                painter = painterResource(R.drawable.ranni_transp),
                contentDescription = "Ranni logo",
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(170.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text("Ranni.app", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("By w_kvib", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }

        HorizontalDivider()

        // Changelog section — scrolls with the rest of the page
        Text("Changelog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        // Extra spacing between each version entry
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.3.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("21/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(
                """
                • Added injury tracking
                • Added reordering for gyms and exercises
                • Bumped DB schema to v12
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.2.1", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("21/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(
                """
                • Added database migration to preserve data between versions
                • Redesigned 'About' page
                • Added EasterEgg
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.2.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("21/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(
                """
                • Fix alarm bug
                • Update icons
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.1.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("20/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(
                """
                • Added multipliers for flash and repeat attempts
                • Updated visuals for custom category
                • Fixed text indentations for scores
                • New logo
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.0.2", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("20/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(
                """
                • More UI fixes.
                • Gym dots separation.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.0.1", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("20/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(
                "• Small UI fixes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.0.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("19/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(
                """
                • Climb tab — log routes at 9 Degrees by colour and grade
                • Exercise sessions with set/rest timers and alarm sounds
                • History calendar showing completed sessions by day
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        } // end changelog column
    }
}
