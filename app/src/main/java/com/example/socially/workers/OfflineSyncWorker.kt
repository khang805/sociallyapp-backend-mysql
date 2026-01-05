package com.example.socially.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.socially.api.RetrofitClient
import com.example.socially.api.SyncRequest
import com.example.socially.auth.SessionManager
import com.example.socially.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background Worker for syncing offline actions
 * Automatically retries when device comes online
 */
class OfflineSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "OfflineSyncWorker"
    private val database = AppDatabase.getDatabase(context)
    private val sessionManager = SessionManager(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting offline sync...")

        try {
            if (!sessionManager.isLoggedIn()) {
                Log.d(TAG, "User not logged in, skipping sync")
                return@withContext Result.success()
            }

            val userId = sessionManager.getUserId()
            val pendingActions = database.offlineQueueDao().getPendingActions()

            if (pendingActions.isEmpty()) {
                Log.d(TAG, "No pending actions to sync")
                return@withContext Result.success()
            }

            Log.d(TAG, "Found ${pendingActions.size} pending actions")

            // Use server-side sync endpoint
            val syncRequest = SyncRequest(userId)
            val response = RetrofitClient.apiService.syncOfflineQueue(syncRequest).execute()

            if (response.isSuccessful && response.body()?.status == "success") {
                Log.d(TAG, "Server sync successful")
                
                // Mark all as completed
                pendingActions.forEach { action ->
                    database.offlineQueueDao().updateStatus(action.id, "completed")
                }
                
                // Clean up completed actions
                database.offlineQueueDao().deleteCompleted()
                
                Result.success()
            } else {
                Log.e(TAG, "Server sync failed: ${response.message()}")
                
                // Increment retry count for failed actions
                pendingActions.forEach { action ->
                    database.offlineQueueDao().incrementRetryCount(action.id)
                }
                
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            Result.retry()
        }
    }
}
