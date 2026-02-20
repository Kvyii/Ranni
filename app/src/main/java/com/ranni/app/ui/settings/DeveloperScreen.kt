package com.ranni.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ranni.app.data.db.AppDatabase
import com.ranni.app.data.db.clearDatabase
import com.ranni.app.data.db.seedDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeveloperScreen(db: AppDatabase) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Debug tools for development builds only.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                scope.launch {
                    // Run DB write on background thread
                    withContext(Dispatchers.IO) { seedDatabase(db) }
                    Toast.makeText(context, "Test data seeded", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Seed test data")
        }

        OutlinedButton(
            onClick = {
                scope.launch {
                    // Run DB write on background thread
                    withContext(Dispatchers.IO) { clearDatabase(db) }
                    Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Clear all data")
        }
    }
}
