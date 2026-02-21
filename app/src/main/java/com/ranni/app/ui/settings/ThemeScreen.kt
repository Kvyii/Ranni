package com.ranni.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ranni.app.ui.theme.AppTheme

/**
 * Settings sub-page that lets the user pick a named UI colour theme.
 * Selection is persisted via [MetricsViewModel.updateTheme] and takes
 * effect immediately because [RanniTheme] in MainScaffold collects the
 * same config Flow.
 */
@Composable
fun ThemeScreen(viewModel: MetricsViewModel) {
    val config by viewModel.config.collectAsState()
    // Resolve the current theme safely, defaulting to ORIGINAL on unknown values
    val selected = try { AppTheme.valueOf(config.uiTheme) } catch (_: IllegalArgumentException) { AppTheme.ORIGINAL }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTheme.entries.forEachIndexed { index, theme ->
            ThemeRow(
                theme = theme,
                isSelected = theme == selected,
                onSelect = { viewModel.updateTheme(theme) }
            )
            // Divider between rows, but not after the last one
            if (index < AppTheme.entries.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

/**
 * A single row representing one theme preset.
 * Shows the theme display name, a strip of key colour swatches, and a
 * checkmark when this theme is currently active.
 */
@Composable
private fun ThemeRow(
    theme: AppTheme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Theme display name
            Text(theme.displayName, style = MaterialTheme.typography.bodyLarge)
            // Small swatch strip showing primary / background / surface colours
            ThemeSwatchStrip(theme)
        }

        // Checkmark shown for the active theme
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Three small colour dots showing the theme's primary, background,
 * and surface colours at a glance.
 */
@Composable
private fun ThemeSwatchStrip(theme: AppTheme) {
    val scheme = theme.colorScheme
    val swatches = listOf(
        scheme.primary,
        scheme.background,
        scheme.surface,
        scheme.onSurfaceVariant
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        swatches.forEach { color ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
                    // Subtle border so light swatches are visible on light backgrounds
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    }
}
