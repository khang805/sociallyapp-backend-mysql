package com.example.socially.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline Queue Entity
 * Stores user actions when device is offline for later sync
 */
@Entity(tableName = "offline_queue")
data class OfflineQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val actionType: String, // "message", "post", "story", "like", "comment"
    val actionData: String, // JSON string of the action data
    val timestamp: Long,
    val status: String = "pending", // "pending", "syncing", "completed", "failed"
    val retryCount: Int = 0
)

/**
 * Post Entity for offline storage
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: Int,
    val userId: Int,
    val username: String,
    val profilePicture: String?,
    val caption: String?,
    val imageUrl: String,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean,
    val createdAt: String,
    val isSynced: Boolean = true
)

/**
 * Story Entity for offline storage
 */
@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey
    val id: Int,
    val userId: Int,
    val username: String,
    val profilePicture: String?,
    val mediaUrl: String,
    val mediaType: String,
    val createdAt: String,
    val expiresAt: String,
    val isSynced: Boolean = true
)
