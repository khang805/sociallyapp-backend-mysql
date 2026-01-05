package com.example.socially.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


@Dao
interface MessageDao {
    // Save a message (used when receiving or sending)
    @Insert
    suspend fun insertMessage(message: MessageEntity)

    // Get all messages for a specific chat (Sender OR Receiver matches)
    @Query("SELECT * FROM messages WHERE (senderId = :myId AND receiverId = :otherId) OR (senderId = :otherId AND receiverId = :myId) ORDER BY createdAt ASC")
    suspend fun getChatMessages(myId: Int, otherId: Int): List<MessageEntity>

    // Get all unsynced messages (SyncStatus = 1)
    @Query("SELECT * FROM messages WHERE syncStatus = 1")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    // Update message (e.g., after it's successfully uploaded to MySQL)
    @Update
    suspend fun updateMessage(message: MessageEntity)

    // Get message by ID
    @Query("SELECT * FROM messages WHERE serverId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Int): MessageEntity?

    // Mark message as edited
    @Query("UPDATE messages SET isEdited = 1, editedAt = :editedAt, messageText = :newText WHERE serverId = :messageId")
    suspend fun markAsEdited(messageId: Int, newText: String, editedAt: String)

    // Mark message as deleted
    @Query("UPDATE messages SET isDeleted = 1 WHERE serverId = :messageId")
    suspend fun markAsDeleted(messageId: Int)

    // Get non-deleted messages for a chat
    @Query("SELECT * FROM messages WHERE ((senderId = :myId AND receiverId = :otherId) OR (senderId = :otherId AND receiverId = :myId)) AND isDeleted = 0 ORDER BY createdAt ASC")
    suspend fun getActiveChatMessages(myId: Int, otherId: Int): List<MessageEntity>

    // Delete all messages for a user pair (if needed for privacy)
    @Query("DELETE FROM messages WHERE (senderId = :userId1 AND receiverId = :userId2) OR (senderId = :userId2 AND receiverId = :userId1)")
    suspend fun deleteAllMessagesForPair(userId1: Int, userId2: Int)
}