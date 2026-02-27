package com.ranni.app.data.db

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Exports the Room database to a user-chosen URI via the Storage Access Framework.
 * Performs a WAL checkpoint first to flush any pending writes into the main DB file.
 */
suspend fun exportDatabase(context: Context, db: AppDatabase, destUri: Uri): Result<Unit> =
    withContext(Dispatchers.IO) {
        runCatching {
            // Flush WAL writes into the main database file before copying
            db.query("PRAGMA wal_checkpoint(FULL)", null).close()

            val dbFile = context.getDatabasePath("ranni_db")
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                dbFile.inputStream().use { it.copyTo(out) }
            } ?: error("Could not open output stream for export URI")
            Unit
        }
    }

/**
 * Restores the Room database from a user-chosen URI via the Storage Access Framework.
 * Closes the current DB instance, overwrites the DB file, then reopens the singleton.
 */
suspend fun importDatabase(context: Context, db: AppDatabase, sourceUri: Uri): Result<Unit> =
    withContext(Dispatchers.IO) {
        runCatching {
            // Close the Room connection so the file can be safely replaced
            db.close()
            // Reset the singleton so the next getInstance() call re-opens from the new file
            AppDatabase.resetInstance()

            val dbFile: File = context.getDatabasePath("ranni_db")
            // Also remove WAL and SHM sidecar files to avoid stale state
            File("${dbFile.path}-wal").delete()
            File("${dbFile.path}-shm").delete()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dbFile.outputStream().use { input.copyTo(it) }
            } ?: error("Could not open input stream for import URI")

            // Reopen the singleton with the restored file
            AppDatabase.getInstance(context)
            Unit
        }
    }
