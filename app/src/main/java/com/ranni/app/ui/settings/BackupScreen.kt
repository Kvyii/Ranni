package com.ranni.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ranni.app.data.db.AppDatabase
import com.ranni.app.data.db.clearDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupScreen(
    db: AppDatabase,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onWipeComplete: () -> Unit
) {
    // Controls visibility of the wipe confirmation dialog
    var showWipeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Backup section ────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Backup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Save a copy of all your data to a file you choose.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export backup")
            }
        }

        HorizontalDivider()

        // ── Restore section ───────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Restore", style = MaterialTheme.typography.titleMedium)
            Text(
                "Replace all current data with a previously exported backup file.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import backup")
            }
        }

        HorizontalDivider()

        // ── Wipe section ──────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Danger Zone", style = MaterialTheme.typography.titleMedium)
            Text(
                "Permanently delete all data. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { showWipeDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Wipe all data")
            }
        }
    }

    // ── Wipe confirmation dialog ──────────────────────────────────────────
    if (showWipeDialog) {
        WipeConfirmationDialog(
            db = db,
            onDismiss = { showWipeDialog = false },
            onWipeComplete = {
                showWipeDialog = false
                onWipeComplete()
            }
        )
    }
}

@Composable
private fun WipeConfirmationDialog(
    db: AppDatabase,
    onDismiss: () -> Unit,
    onWipeComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wipe all data?") },
        text = {
            Text(
                "This will permanently delete all climbs, sessions, exercises, injuries, and settings. " +
                "This cannot be undone."
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { clearDatabase(db) }
                        onWipeComplete()
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Wipe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
