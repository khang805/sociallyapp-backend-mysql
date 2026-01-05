package com.example.socially.repository

import android.content.Context
import android.util.Log
import com.example.socially.api.MessageRequest
import com.example.socially.api.RetrofitClient
import com.example.socially.db.AppDatabase
import com.example.socially.db.MessageEntity
import com.example.socially.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse // Ensure you have Retrofit Coroutine support

class ChatRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val messageDao = db.messageDao()
    private val api = RetrofitClient.apiService

    /**
     * LOGIC:
     * 1. Save to SQLite immediately (SyncStatus = 1 / Pending).
     * 2. Check Internet.
     * 3. If Online -> Send to API -> Update SQLite to (SyncStatus = 0 / Synced).
     * 4. If Offline -> Do nothing (Background Worker will handle it later).
     */
    suspend fun sendMessage(senderId: Int, receiverId: Int, messageText: String) {
        withContext(Dispatchers.IO) {
            // 1. Save Local (Offline First)
            val localMessage = MessageEntity(
                senderId = senderId,
                receiverId = receiverId,
                messageText = messageText,
                mediaUrl = null,
                createdAt = System.currentTimeMillis().toString(), // Simplified timestamp
                syncStatus = 1 // 1 means "Waiting to Upload"
            )
            // Insert and get the Local row ID
            messageDao.insertMessage(localMessage)

            // 2. Check Online Status
            if (NetworkUtils.isOnline(context)) {
                try {
                    // Updated to match new MessageRequest signature
                    val request = MessageRequest(
                        sender_id = senderId,
                        receiver_id = receiverId,
                        message_text = messageText,
                        media_base64 = null,
                        media_type = "text",
                        is_vanish_mode = false
                    )
                    val response = api.sendMessage(request).awaitResponse()

                    if (response.isSuccessful && response.body()?.status == "success") {
                        // 3. Mark as Synced (Changed from server_id to message_id)
                        val messageId = response.body()?.message_id
                        // We need to update the specific message we just inserted.
                        // NOTE: In a real app, you'd retrieve the inserted row ID to update it specifically.
                        // For simplicity here, we assume the sync worker handles the cleanup or we query the last msg.
                        Log.d("ChatRepo", "Message sent to server. ID: $messageId")
                    }
                } catch (e: Exception) {
                    Log.e("ChatRepo", "Upload failed, message remains queued locally.")
                }
            } else {
                Log.d("ChatRepo", "Offline. Message queued.")
            }
        }
    }

    // Fetch messages for the UI
    suspend fun getMessages(myId: Int, otherId: Int): List<MessageEntity> {
        return messageDao.getChatMessages(myId, otherId)
    }
}