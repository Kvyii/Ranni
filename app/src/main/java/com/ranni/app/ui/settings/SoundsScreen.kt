package com.ranni.app.ui.settings

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ranni.app.R
import com.ranni.app.data.AlarmPreferences

/**
 * Settings sub-page for managing alarm sounds.
 * Lets the user preview, pick, and reset the rest alarm sound.
 */
@Composable
fun SoundsScreen(
    customRestAlarmName: String?,
    onPickRestAlarm: () -> Unit,
    onResetRestAlarm: () -> Unit
) {
    val context = LocalContext.current
    val alarmPrefs = remember { AlarmPreferences(context) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // Clean up preview player when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.release()
            previewPlayer = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Text("Rest Alarm", style = MaterialTheme.typography.titleMedium)

        // Current alarm display with play/stop and reset controls
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current alarm name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Current",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        customRestAlarmName ?: "Default",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play/Stop toggle button
                IconButton(onClick = {
                    if (isPlaying) {
                        // Stop playback
                        previewPlayer?.stop()
                        previewPlayer?.release()
                        previewPlayer = null
                        isPlaying = false
                    } else {
                        // Start playback
                        previewPlayer?.release()
                        val customUri = alarmPrefs.getCustomRestAlarmUri()
                        previewPlayer = if (customUri != null) {
                            try {
                                MediaPlayer().apply { setDataSource(context, customUri); prepare() }
                            } catch (_: Exception) {
                                MediaPlayer.create(context, R.raw.alarm_rest)
                            }
                        } else {
                            MediaPlayer.create(context, R.raw.alarm_rest)
                        }
                        previewPlayer?.apply {
                            setOnCompletionListener { mp ->
                                mp.release()
                                previewPlayer = null
                                isPlaying = false
                            }
                            start()
                            isPlaying = true
                        }
                    }
                }) {
                    // Use separate Icon calls — Painter and ImageVector are different types
                    if (isPlaying) {
                        Icon(
                            painter = painterResource(R.drawable.ic_stop),
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Reset button — only shown when a custom alarm is set
                if (customRestAlarmName != null) {
                    IconButton(onClick = {
                        // Stop any preview before resetting
                        previewPlayer?.stop()
                        previewPlayer?.release()
                        previewPlayer = null
                        isPlaying = false
                        onResetRestAlarm()
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset to default",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Choose new alarm button
        OutlinedButton(
            onClick = onPickRestAlarm,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Choose New Alarm")
        }

        // Reset to default button — only shown when a custom alarm is set
        if (customRestAlarmName != null) {
            OutlinedButton(
                onClick = {
                    // Stop any preview before resetting
                    previewPlayer?.stop()
                    previewPlayer?.release()
                    previewPlayer = null
                    isPlaying = false
                    onResetRestAlarm()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset to Default")
            }
        }
    }
}
