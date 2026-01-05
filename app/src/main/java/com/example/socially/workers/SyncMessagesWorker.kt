package com.example.socially.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.socially.api.MessageRequest
import com.example.socially.api.RetrofitClient
import com.example.socially.auth.SessionManager
import com.example.socially.db.AppDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker to sync unsynced messages when device comes online
 * Also syncs other offline queued actions: posts, stories, likes, comments
 */
class SyncMessagesWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val sessionManager = SessionManager(applicationContext)
            
            // Initialize RetrofitClient if not already done
            RetrofitClient.init(sessionManager)
            
            Log.d("SyncMessagesWorker", "Starting sync of offline messages and actions")

            // Sync unsynced messages (old system)
            syncUnsyncedMessages(db, sessionManager)

            // Sync offline queue actions (new system)
            syncOfflineQueueActions(db, sessionManager)

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncMessagesWorker", "Sync failed: ${e.message}", e)
            Result.retry() // WorkManager will retry automatically later
        }
    }

    /**
     * Sync unsynced messages from MessageEntity table
     */
    private suspend fun syncUnsyncedMessages(db: AppDatabase, sessionManager: SessionManager) {
        try {
            val unsyncedMessages = db.messageDao().getUnsyncedMessages()

            if (unsyncedMessages.isEmpty()) {
                Log.d("SyncMessagesWorker", "No unsynced messages")
                return
            }

            Log.d("SyncMessagesWorker", "Syncing ${unsyncedMessages.size} messages")

            for (msg in unsyncedMessages) {
                msg.messageText?.let { text ->
                    val request = MessageRequest(
                        sender_id = msg.senderId,
                        receiver_id = msg.receiverId,
                        message_text = text,
                        media_base64 = null,
                        media_type = "text",
                        is_vanish_mode = msg.isVanish
                    )
                    
                    val response = RetrofitClient.apiService.sendMessage(request).execute()

                    if (response.isSuccessful && response.body()?.status == "success") {
                        val updatedMsg = msg.copy(
                            syncStatus = 0,
                            serverId = response.body()?.message_id
                        )
                        db.messageDao().updateMessage(updatedMsg)
                        Log.d("SyncMessagesWorker", "Message ${msg.localId} synced successfully")
                    } else {
                        Log.e("SyncMessagesWorker", "Failed to sync message ${msg.localId}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SyncMessagesWorker", "Error syncing unsynced messages: ${e.message}")
        }
    }

    /**
     * Sync offline queue actions (posts, stories, likes, comments, messages, follow requests)
     */
    private suspend fun syncOfflineQueueActions(db: AppDatabase, sessionManager: SessionManager) {
        try {
            val pendingActions = db.offlineQueueDao().getPendingActions()

            if (pendingActions.isEmpty()) {
                Log.d("SyncMessagesWorker", "No pending offline actions")
                return
            }

            Log.d("SyncMessagesWorker", "Syncing ${pendingActions.size} offline actions")

            for (action in pendingActions) {
                val success = when (action.actionType) {
                    "message" -> syncOfflineMessage(action)
                    "post" -> syncOfflinePost(action)
                    "story" -> syncOfflineStory(action)
                    "like" -> syncOfflineLike(action)
                    "comment" -> syncOfflineComment(action)
                    "follow_request" -> syncOfflineFollowRequest(action)
                    else -> {
                        Log.w("SyncMessagesWorker", "Unknown action type: ${action.actionType}")
                        false
                    }
                }

                if (success) {
                    db.offlineQueueDao().updateStatus(action.id, "completed")
                    Log.d("SyncMessagesWorker", "Action ${action.id} (${action.actionType}) synced successfully")
                } else {
                    db.offlineQueueDao().incrementRetryCount(action.id)
                    Log.w("SyncMessagesWorker", "Action ${action.id} (${action.actionType}) sync failed, will retry")
                }
            }

            // Clean up completed actions
            db.offlineQueueDao().deleteCompleted()
        } catch (e: Exception) {
            Log.e("SyncMessagesWorker", "Error syncing offline queue: ${e.message}")
        }
    }

    private suspend fun syncOfflineMessage(action: com.example.socially.db.OfflineQueueEntity): Boolean {
        return try {
            val data = gson.fromJson(action.actionData, Map::class.java)
            val request = MessageRequest(
                sender_id = (data["sender_id"] as? Number)?.toInt() ?: 0,
                receiver_id = (data["receiver_id"] as? Number)?.toInt() ?: 0,
                message_text = data["message_text"] as? String,
                media_base64 = (data["media_base64"] as? String)?.takeIf { it.isNotEmpty() },
                media_type = data["media_type"] as? String ?: "text",
                is_vanish_mode = data["is_vanish_mode"] as? Boolean ?: false
            )
            val response = RetrofitClient.apiService.sendMessage(request).execute()
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            Log.e("SyncMessagesWorker", "Error syncing offline message: ${e.message}")
            false
        }
    }

    private suspend fun syncOfflinePost(action: com.example.socially.db.OfflineQueueEntity): Boolean {
        return try {
            val data = gson.fromJson(action.actionData, Map::class.java)
            val request = com.example.socially.api.UploadPostRequest(
                user_id = (data["user_id"] as? Number)?.toInt() ?: 0,
                caption = data["caption"] as? String,
                image_base64 = data["image_base64"] as? String ?: ""
            )
            val response = RetrofitClient.apiService.uploadPost(request).execute()
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            Log.e("SyncMessagesWorker", "Error syncing offline post: ${e.message}")
            false
        }
    }

    private suspend fun syncOfflineStory(action: com.example.socially.db.OfflineQueueEntity): Boolean {
        return try {
            val data = gson.fromJson(action.actionData, Map::class.java)
            val request = com.example.socially.api.UploadStoryRequest(
                user_id = (data["user_id"] as? Number)?.toInt() ?: 0,
                media_base64 = data["media_base64"] as? String ?: "",
                media_type = data["media_type"] as? String ?: "image"
            )
            val response = RetrofitClient.apiService.uploadStory(request).execute()
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            Log.e("SyncMessagesWorker", "Error syncing offline story: ${e.message}")
            false
        }
    }

    private suspend fun syncOfflineLike(action: com.example.socially.db.OfflineQueueEntity): Boolean {
        return try {
            val data = gson.fromJson(action.actionData, Map::class.java)
            val request = com.example.socially.api.LikeRequest(
                post_id = (data["post_id"] as? Number)?.toInt() ?: 0,
                user_id = (data["user_id"] as? Number)?.toInt() ?: 0
            )
            val response = RetrofitClient.apiService.toggleLike(request).execute()
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            Log.e("SyncMessagesWorker", "Error syncing offline like: ${e.message}")
            false
        }
    }

    private suspend fun syncOfflineComment(action: com.example.socially.db.OfflineQueueEntity): Boolean {
        return try {
            val data = gson.fromJson(action.actionData, Map::class.java)
            val request = com.example.socially.api.CommentRequest(
                post_id = (data["post_id"] as? Number)?.toInt() ?: 0,
                user_id = (data["user_id"] as? Number)?.toInt() ?: 0,
                comment_text = data["comment_text"] as? String ?: ""
            )
            val response = RetrofitClient.apiService.addComment(request).execute()
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            Log.e("SyncMessagesWorker", "Error syncing offline comment: ${e.message}")
            false
        }
    }

    private suspend fun syncOfflineFollowRequest(action: com.example.socially.db.OfflineQueueEntity): Boolean {
        return try {
            val data = gson.fromJson(action.actionData, Map::class.java)
            val request = com.example.socially.api.FollowRequest(
                sender_id = (data["sender_id"] as? Number)?.toInt() ?: 0,
                receiver_id = (data["receiver_id"] as? Number)?.toInt() ?: 0
            )
            val response = RetrofitClient.apiService.sendFollowRequest(request).execute()
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            Log.e("SyncMessagesWorker", "Error syncing offline follow request: ${e.message}")
            false
        }
    }
}