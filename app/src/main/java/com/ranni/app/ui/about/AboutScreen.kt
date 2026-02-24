package com.ranni.app.ui.about

import android.widget.ImageView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.ranni.app.R

// Keyword → colour mappings applied by ChangelogText
private val changelogKeywordColors: Map<String, Color> = mapOf(
    "WARNING" to Color.Red
)

/**
 * Renders changelog body text with keyword highlighting.
 * Any word listed in [changelogKeywordColors] is coloured automatically.
 */
@Composable
private fun ChangelogText(text: String) {
    Text(
        text = buildAnnotatedString {
            // Walk through the text looking for any registered keyword
            var cursor = 0
            while (cursor < text.length) {
                // Find the earliest keyword match from the current cursor position
                val match = changelogKeywordColors.entries
                    .mapNotNull { (kw, color) ->
                        val idx = text.indexOf(kw, cursor)
                        if (idx >= 0) Triple(idx, kw, color) else null
                    }
                    .minByOrNull { it.first }

                if (match == null) {
                    // No more keywords — append the rest plain
                    append(text.substring(cursor))
                    break
                }
                val (idx, kw, color) = match
                // Append plain text before the keyword
                append(text.substring(cursor, idx))
                // Append the keyword in its designated colour
                withStyle(SpanStyle(color = color)) { append(kw) }
                cursor = idx + kw.length
            }
        },
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun SettingsScreen(
    onNavigateScores: () -> Unit,
    onNavigateMetrics: () -> Unit,
    onNavigateSounds: () -> Unit,
    onNavigateHelp: () -> Unit,
    onNavigateAbout: () -> Unit,
    onNavigateTheme: () -> Unit,
    onNavigateDev: () -> Unit = {},
    showDevTools: Boolean = false
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsRow("Scores", onClick = onNavigateScores)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SettingsRow("Preferences", onClick = onNavigateMetrics)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SettingsRow("Sounds", onClick = onNavigateSounds)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        // Theme picker — lets the user swap the app colour scheme
        SettingsRow("Theme", onClick = onNavigateTheme)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        // Help page with FAQ
        SettingsRow("Help", onClick = onNavigateHelp)
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
fun HelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // App logo header — same layout as About page
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnimatedRanniLogo(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(200.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text("FAQ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider()

        // FAQ entries
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Q: How do I use this app?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("A: Ask w_kvib", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
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
            AnimatedRanniLogo(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(200.dp)
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
            Text("v1.2.7.1", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("25/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • Added comparison option to settings > metrics to show +/- deltas vs the preceding period of equal length (e.g. if you select 1 month, it compares to the previous month; if you select 3 months, it compares to the previous 3 months). Only shown when there is sufficient data (2× the selected period) and period is not Lifetime.
                • Added median setting for History > Progress to reflect only best 50% of climbs
                • Fixed graph bug where plot starts before first available data point
                • Fixed histograph bug where hollow type climbs do not render both bars
                • Fixed a bug with Progress graph truncating when points do not occur on the start of week
                • Fixed a bug with Progress graph not starting at 0
                • Added 3 Gundam themes
                """.trimIndent())
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.2.6.2", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("24/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • Fixed contrast issue with dots and dark / light themes
                • Fixed issue where users couldn't scroll themes
                """.trimIndent())
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.2.5", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("23/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • Added 11 new themes
                """.trimIndent())
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.2.4", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("23/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • Increase History > Progress graph size
                • Removed separation lines in history > calendar UI
                • Introduced smoothing in history > progress graph
                """.trimIndent())
        }


        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.2.3", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("23/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • Fix clipping bug in calendar
                • Fix sliding animation jitter from daily view back to calendar
                """.trimIndent())
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.2.2", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("23/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • Fix non persistent history filter bug
                • Update calendar UI to slide to daily items when selected.
                • Increase calendar max dots to 8 from 4
                • Update 9D Orange route to V3 - V5 from V3 - V4
                """.trimIndent())
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.2.1", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("23/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • Fix clipping UI bug
                """.trimIndent())
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.2.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("22/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • Added Stats page
                • Added ability to filter repeat climbs from display
                • Repeated climbs are now displayed faded in Calendar
                • Added a favourites feature to keep gyms expanded
                • Added done button to exercises
                • Animated Ranni logo
                • Rejigged the UI a bit more
                """.trimIndent())
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.1.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("21/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • Big UI rehaul. Added Themes
                • Changed default theme from Android Dark to Ranni Dark
                • Added outdoor gyms with YDS and V grading
                • Bumped DB schema to v13 with one time migration. All older DBs will need to be updated to work with 1.4.0+
                • WARNING: DB will be deprecated by v1.1.0+. Users below 1.0.3+ will lose their data if upgrading to v1.4.0+
                """.trimIndent())
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.0.3", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("21/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • Fix alarm bug
                • Update icons
                • Added database migration to preserve data between versions
                • Redesigned 'About' page
                • Added EasterEgg
                • Added injury tracking
                • Added reordering for gyms and exercises
                • Bumped DB schema to v12
                """.trimIndent())
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.0.2", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("20/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • More UI fixes
                • Gym dots separation
                • Added multipliers for flash and repeat attempts
                • Updated visuals for custom category
                • Fixed text indentations for scores
                • Added a new logo for the app
                """.trimIndent())
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.0.1", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("20/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("• Small UI fixes")
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("v1.0.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            // Release date
            Text("19/02/2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ChangelogText("""
                • First release! My first android app
                • Climb tab — log routes at 9 Degrees by colour and grade
                • Exercise sessions with set/rest timers and alarm sounds
                • History calendar showing completed sessions by day
                """.trimIndent())
        }

        } // end changelog column
    }
}

/**
 * Animated Ranni logo using the View-system AnimatedVectorDrawableCompat,
 * which correctly respects repeatCount="infinite" in the animator XMLs.
 * The Compose AVD renderer (rememberAnimatedVectorPainter) does not support
 * infinite repeat, so we embed an ImageView via AndroidView instead.
 */
@Composable
fun AnimatedRanniLogo(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                // Load the animated vector and start it immediately
                val avd = AnimatedVectorDrawableCompat.create(context, R.drawable.ranni_transp3_animated)
                setImageDrawable(avd)
                avd?.start()
            }
        },
        modifier = modifier
    )
}
