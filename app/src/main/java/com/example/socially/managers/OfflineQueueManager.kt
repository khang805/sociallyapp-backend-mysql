package com.example.socially.managers

import android.content.Context
import android.util.Log
import com.example.socially.db.AppDatabase
import com.example.socially.db.OfflineQueueEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OfflineQueueManager - Manages queuing and processing of offline actions
 * Handles: post uploads, story uploads, messages, likes, comments, follow requests
 */
class OfflineQueueManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val offlineDao = db.offlineQueueDao()
    private val gson = Gson()

    /**
     * Queue a post upload action
     */
    suspend fun queuePostUpload(
        userId: Int,
        caption: String?,
        mediaBase64: String
    ) = withContext(Dispatchers.IO) {
        try {
            val actionData = gson.toJson(mapOf(
                "user_id" to userId,
                "caption" to (caption ?: ""),
                "media_base64" to mediaBase64
            ))
            val entity = OfflineQueueEntity(
                userId = userId,
                actionType = "post",
                actionData = actionData,
                timestamp = System.currentTimeMillis(),
                status = "pending"
            )
            val id = offlineDao.insert(entity)
            Log.d("OfflineQueueManager", "Post upload queued with ID: $id")
            id
        } catch (e: Exception) {
            Log.e("OfflineQueueManager", "Error queueing post: ${e.message}")
            -1L
        }
    }

    /**
     * Queue a story upload action
     */
    suspend fun queueStoryUpload(
        userId: Int,
        mediaBase64: String,
        mediaType: String = "image"
    ) = withContext(Dispatchers.IO) {
        try {
            val actionData = gson.toJson(mapOf(
                "user_id" to userId,
                "media_base64" to mediaBase64,
                "media_type" to mediaType
            ))
            val entity = OfflineQueueEntity(
                userId = userId,
                actionType = "story",
                actionData = actionData,
                timestamp = System.currentTimeMillis(),
                status = "pending"
            )
            val id = offlineDao.insert(entity)
            Log.d("OfflineQueueManager", "Story upload queued with ID: $id")
            id
        } catch (e: Exception) {
            Log.e("OfflineQueueManager", "Error queueing story: ${e.message}")
            -1L
        }
    }

    /**
     * Queue a message
     */
    suspend fun queueMessage(
        userId: Int,
        senderId: Int,
        receiverId: Int,
        messageText: String?,
        mediaBase64: String? = null,
        mediaType: String = "text",
        isVanishMode: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            val actionData = gson.toJson(mapOf(
                "user_id" to userId,
                "sender_id" to senderId,
                "receiver_id" to receiverId,
                "message_text" to (messageText ?: ""),
                "media_base64" to (mediaBase64 ?: ""),
                "media_type" to mediaType,
                "is_vanish_mode" to isVanishMode
            ))
            val entity = OfflineQueueEntity(
                userId = userId,
                actionType = "message",
                actionData = actionData,
                timestamp = System.currentTimeMillis(),
                status = "pending"
            )
            val id = offlineDao.insert(entity)
            Log.d("OfflineQueueManager", "Message queued with ID: $id")
            id
        } catch (e: Exception) {
            Log.e("OfflineQueueManager", "Error queueing message: ${e.message}")
            -1L
        }
    }

    /**
     * Queue a like action
     */
    suspend fun queueLike(
        userId: Int,
        postId: Int
    ) = withContext(Dispatchers.IO) {
        try {
            val actionData = gson.toJson(mapOf(
                "user_id" to userId,
                "post_id" to postId
            ))
            val entity = OfflineQueueEntity(
                userId = userId,
                actionType = "like",
                actionData = actionData,
                timestamp = System.currentTimeMillis(),
                status = "pending"
            )
            val id = offlineDao.insert(entity)
            Log.d("OfflineQueueManager", "Like action queued with ID: $id")
            id
        } catch (e: Exception) {
            Log.e("OfflineQueueManager", "Error queueing like: ${e.message}")
            -1L
        }
    }

    /**
     * Queue a comment action
     */
    suspend fun queueComment(
        userId: Int,
        postId: Int,
        commentText: String
    ) = withContext(Dispatchers.IO) {
        try {
            val actionData = gson.toJson(mapOf(
                "user_id" to userId,
                "post_id" to postId,
                "comment_text" to commentText
            ))
            val entity = OfflineQueueEntity(
                userId = userId,
                actionType = "comment",
                actionData = actionData,
                timestamp = System.currentTimeMillis(),
                status = "pending"
            )
            val id = offlineDao.insert(entity)
            Log.d("OfflineQueueManager", "Comment action queued with ID: $id")
            id
        } catch (e: Exception) {
            Log.e("OfflineQueueManager", "Error queueing comment: ${e.message}")
            -1L
        }
    }

    /**
     * Queue a follow request
     */
    suspend fun queueFollowRequest(
        userId: Int,
        senderId: Int,
        receiverId: Int
    ) = withContext(Dispatchers.IO) {
        try {
            val actionData = gson.toJson(mapOf(
                "user_id" to userId,
                "sender_id" to senderId,
                "receiver_id" to receiverId
            ))
            val entity = OfflineQueueEntity(
                userId = userId,
                actionType = "follow_request",
                actionData = actionData,
                timestamp = System.currentTimeMillis(),
                status = "pending"
            )
            val id = offlineDao.insert(entity)
            Log.d("OfflineQueueManager", "Follow request queued with ID: $id")
            id
        } catch (e: Exception) {
            Log.e("OfflineQueueManager", "Error queueing follow request: ${e.message}")
            -1L
        }
    }

    /**
     * Get all pending actions
     */
    suspend fun getPendingActions() = withContext(Dispatchers.IO) {
        try {
            offlineDao.getPendingActions()
        } catch (e: Exception) {
            Log.e("OfflineQueueManager", "Error getting pending actions: ${e.message}")
            emptyList()
        }
    }

    /**
     * Mark action as completed
     */
    suspend fun markAsCompleted(actionId: Int) = withContext(Dispatchers.IO) {
        try {
            offlineDao.updateStatus(actionId, "completed")
            Log.d("OfflineQueueManager", "Action $actionId marked as completed")
        } catch (e: Exception) {
            Log.e("OfflineQueueManager", "Error marking action as completed: ${e.message}")
        }
    }

    /**
     * Mark action as failed
     */
    suspend fun markAsFailed(actionId: Int) = withContext(Dispatchers.IO) {
        try {
            offlineDao.updateStatus(actionId, "failed")
            offlineDao.incrementRetryCount(actionId)
            Log.d("OfflineQueueManager", "Action $actionId marked as failed")
        } catch (e: Exception) {
            Log.e("OfflineQueueManager", "Error marking action as failed: ${e.message}")
        }
    }

    /**
     * Clear completed actions
     */
    suspend fun clearCompleted() = withContext(Dispatchers.IO) {
        try {
            offlineDao.deleteCompleted()
            Log.d("OfflineQueueManager", "Cleared completed actions")
        } catch (e: Exception) {
            Log.e("OfflineQueueManager", "Error clearing completed: ${e.message}")
        }
    }

    /**
     * Get action count by status
     */
    suspend fun getQueueStats() = withContext(Dispatchers.IO) {
        try {
            val pending = offlineDao.getPendingActions().size
            mapOf("pending" to pending, "syncing" to 0)
        } catch (e: Exception) {
            Log.e("OfflineQueueManager", "Error getting queue stats: ${e.message}")
            mapOf()
        }
    }
}

