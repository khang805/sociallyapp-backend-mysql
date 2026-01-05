package com.example.socially.db

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0, // Local ID for SQLite
    val serverId: Int? = null,       // ID from MySQL (null if not synced yet)
    val senderId: Int,
    val receiverId: Int,
    val messageText: String?,
    val mediaUrl: String?,
    val messageType: String = "text",
    val isVanish: Boolean = false,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val editedAt: String? = null,
    val createdAt: String,           // Store as String (yyyy-MM-dd HH:mm:ss)

    // Sync Status: 0 = Synced, 1 = Pending Upload (Written while offline)
    val syncStatus: Int = 0
)

