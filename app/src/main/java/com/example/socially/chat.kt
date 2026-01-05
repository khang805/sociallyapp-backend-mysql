package com.example.socially

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socially.api.EditMessageRequest
import com.example.socially.api.RetrofitClient
import com.example.socially.auth.SessionManager
import com.example.socially.db.AppDatabase
import com.example.socially.db.MessageEntity
import com.example.socially.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.socially.api.PollMessagesResponse
import retrofit2.awaitResponse

/**
 * Chat Activity - Partial implementation using polling-based REST API
 * - Polls new messages using RetrofitClient.apiService.pollNewMessages()
 * - Sends messages using RetrofitClient.apiService.sendMessage()
 * - Polls user online status using RetrofitClient.apiService.getUserStatus()
 *
 * This activity is implemented as a lightweight, migration-friendly replacement
 * for the previous Firebase-based chat. It uses local Room DB and existing
 * repositories/workers for offline support.
 */
class chat : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase
    private lateinit var chatRepo: ChatRepository

    // UI
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var chatUsername: TextView
    private lateinit var statusText: TextView
    private lateinit var onlineIndicator: View
    private lateinit var sendButton: ImageView
    private lateinit var messageEditText: EditText
    private lateinit var videoCallBtn: ImageView

    // Polling jobs
    private var messagesPollJob: Job? = null
    private var statusPollJob: Job? = null

    // Track last received message id for incremental polling
    private var lastMessageId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_screen)

        sessionManager = SessionManager(this)
        db = AppDatabase.getDatabase(this)
        RetrofitClient.init(sessionManager)
        chatRepo = ChatRepository(this)

        // Wire views
        messageRecyclerView = findViewById(R.id.messageRecyclerView)
        chatUsername = findViewById(R.id.chat_username)
        statusText = findViewById(R.id.status_text)
        onlineIndicator = findViewById(R.id.online_indicator)
        sendButton = findViewById(R.id.sendButton)
        messageEditText = findViewById(R.id.messageEditText)
        videoCallBtn = findViewById(R.id.video_call_btn)

        messageRecyclerView.layoutManager = LinearLayoutManager(this)

        // Get other user info
        val otherUserId = intent.getIntExtra("other_user_id", -1)
        val otherUsername = intent.getStringExtra("other_username") ?: "Chat User"
        chatUsername.text = otherUsername

        // Adapter with long-click callback
        val adapter = SimpleMessageAdapter(onMessageLongClick = { messageEntity ->
            // Only allow edit/delete for messages that have a serverId and were sent within 5 minutes
            lifecycleScope.launch(Dispatchers.Main) {
                handleMessageLongPress(messageEntity)
            }
        })

        messageRecyclerView.adapter = adapter

        // Load cached messages into adapter
        lifecycleScope.launch {
            val myId = sessionManager.getUserId()
            if (myId == -1) {
                Toast.makeText(this@chat, "Not logged in", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            if (otherUserId != -1) {
                val messages = chatRepo.getMessages(myId, otherUserId)
                adapter.submitList(messages.filter { !it.isDeleted })
                // Find last message id if available (serverId)
                lastMessageId = messages.maxOfOrNull { it.serverId ?: 0 } ?: 0
            }
        }

        // Send button
        sendButton.setOnClickListener {
            val text = messageEditText.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val myId = sessionManager.getUserId()
            if (myId == -1 || otherUserId == -1) {
                Toast.makeText(this, "Invalid chat participants", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Use repository to save locally and send if online
            lifecycleScope.launch {
                try {
                    chatRepo.sendMessage(myId, otherUserId, text)
                    messageEditText.setText("")
                    // Refresh local list (quick reload)
                    val messages = chatRepo.getMessages(myId, otherUserId)
                    adapter.submitList(messages.filter { !it.isDeleted })
                    // scroll to bottom
                    messageRecyclerView.scrollToPosition(messages.size - 1)
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Send failed: ${e.message}")
                    Toast.makeText(this@chat, "Send failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Video call button
        videoCallBtn.setOnClickListener {
            if (otherUserId != -1) {
                val intent = Intent(this, call::class.java)
                intent.putExtra("other_user_id", otherUserId)
                intent.putExtra("other_username", otherUsername)
                intent.putExtra("channel_name", "channel_${sessionManager.getUserId()}_$otherUserId")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Invalid user for call", Toast.LENGTH_SHORT).show()
            }
        }

        // Start polling for messages and status
        startPolling(otherUserId, adapter)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }

    private fun startPolling(otherUserId: Int, adapter: SimpleMessageAdapter) {
        // Poll messages every 2 seconds
        messagesPollJob = lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val myId = sessionManager.getUserId()
                    if (myId == -1 || otherUserId == -1) break

                    val call = RetrofitClient.apiService.pollNewMessages(myId, otherUserId, lastMessageId)
                    val response = call.awaitResponse()

                    if (response.isSuccessful) {
                        val body: PollMessagesResponse? = response.body()
                        if (body != null && body.status == "success") {
                            val newMessages = body.messages
                            if (newMessages.isNotEmpty()) {
                                // Insert into local DB and update UI
                                withContext(Dispatchers.IO) {
                                    val dao = db.messageDao()
                                    for (msg in newMessages) {
                                        // Avoid duplicates: check serverId
                                        val existing = msg.id
                                        val local = dao.getMessageById(existing)
                                        if (local == null) {
                                            val entity = MessageEntity(
                                                 serverId = msg.id,
                                                 senderId = msg.sender_id,
                                                 receiverId = msg.receiver_id,
                                                 messageText = msg.message_text,
                                                 mediaUrl = msg.media_url,
                                                 messageType = msg.media_type,
                                                 isVanish = msg.is_vanish_mode == 1,  // Convert Int to Boolean
                                                 isEdited = msg.is_edited == 1,        // Convert Int to Boolean
                                                 isDeleted = false,
                                                 editedAt = null,
                                                 createdAt = msg.created_at,
                                                 syncStatus = 0
                                             )
                                             dao.insertMessage(entity)
                                         }
                                     }

                                    // Update lastMessageId
                                    lastMessageId = maxOf(lastMessageId, newMessages.maxOfOrNull { it.id } ?: lastMessageId)
                                }

                                // Update UI from main thread
                                withContext(Dispatchers.Main) {
                                    val messages = chatRepo.getMessages(sessionManager.getUserId(), otherUserId)
                                    adapter.submitList(messages.filter { !it.isDeleted })
                                    messageRecyclerView.scrollToPosition(messages.size - 1)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Polling messages failed: ${e.message}")
                }

                delay(2000)
            }
        }

        // Poll user status every 5 seconds
        statusPollJob = lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    if (otherUserId == -1) break
                    val call = RetrofitClient.apiService.getUserStatus(otherUserId)
                    val response = call.awaitResponse()
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.status == "success") {
                            val isOnline = body.user.is_online  // is_online is Boolean from get_user_status.php
                            val statusTextStr = if (isOnline) "Online" else "Offline"

                            withContext(Dispatchers.Main) {
                                statusText.text = statusTextStr
                                onlineIndicator.setBackgroundResource(if (isOnline) R.drawable.online_indicator_green else R.drawable.online_indicator_gray)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Polling status failed: ${e.message}")
                }

                delay(5000)
            }
        }
    }

    private fun stopPolling() {
        messagesPollJob?.cancel()
        statusPollJob?.cancel()
    }

    private fun handleMessageLongPress(messageEntity: MessageEntity) {
         // If message has no serverId, or isDeleted, do not allow edit/delete
         if (messageEntity.serverId == null || messageEntity.isDeleted) return

         // Allow edit/delete only within 5 minutes of message creation
         try {
             val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
             val createdDate = fmt.parse(messageEntity.createdAt)
             val allowedWindowMs = 5 * 60 * 1000 // 5 minutes
             if (createdDate != null) {
                 val delta = System.currentTimeMillis() - createdDate.time
                 if (delta > allowedWindowMs) {
                     Toast.makeText(this, "You can only edit/delete messages within 5 minutes.", Toast.LENGTH_SHORT).show()
                     return
                 }
             }
         } catch (e: Exception) {
             // If parsing fails, fall back to allowing action for safety
             Log.w("ChatActivity", "Could not parse message createdAt: ${e.message}")
         }

         val options = arrayOf("Edit", "Delete", "Cancel")
         AlertDialog.Builder(this)
             .setTitle("Message options")
             .setItems(options) { dialog, which ->
                 when (which) {
                     0 -> showEditDialog(messageEntity)
                     1 -> confirmDelete(messageEntity)
                     else -> dialog.dismiss()
                 }
             }
             .show()
     }

     private fun showEditDialog(messageEntity: MessageEntity) {
         val input = EditText(this)
         input.setText(messageEntity.messageText ?: "")

         AlertDialog.Builder(this)
             .setTitle("Edit message")
             .setView(input)
             .setPositiveButton("Save") { dialog, _ ->
                 val newText = input.text.toString().trim()
                 if (newText.isNotEmpty()) {
                     lifecycleScope.launch(Dispatchers.IO) {
                         try {
                             val sid = messageEntity.serverId ?: return@launch
                             val editRequest = EditMessageRequest(message_id = sid, new_text = newText)
                             val resp = RetrofitClient.apiService.editMessage(editRequest).awaitResponse()
                             if (resp.isSuccessful && resp.body()?.status == "success") {
                                 // Update local DB
                                 val editedAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                                 db.messageDao().markAsEdited(sid, newText, editedAt)

                                 // Refresh list on main thread
                                 withContext(Dispatchers.Main) {
                                     val messages = chatRepo.getMessages(sessionManager.getUserId(), messageEntity.receiverId)
                                     (messageRecyclerView.adapter as? SimpleMessageAdapter)?.submitList(messages.filter { !it.isDeleted })
                                 }
                             }
                         } catch (e: Exception) {
                             Log.e("ChatActivity", "Edit failed: ${e.message}")
                         }
                     }
                 }
                 dialog.dismiss()
             }
             .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
             .show()
     }

     private fun confirmDelete(messageEntity: MessageEntity) {
         AlertDialog.Builder(this)
             .setTitle("Delete message")
             .setMessage("Are you sure you want to delete this message?")
             .setPositiveButton("Delete") { dialog, _ ->
                 lifecycleScope.launch(Dispatchers.IO) {
                     try {
                         val sid = messageEntity.serverId ?: return@launch
                         val delReq = com.example.socially.api.DeleteMessageRequest(sid)
                         val resp = RetrofitClient.apiService.deleteMessage(delReq).awaitResponse()
                         if (resp.isSuccessful && resp.body()?.status == "success") {
                             db.messageDao().markAsDeleted(sid)
                             withContext(Dispatchers.Main) {
                                 val messages = chatRepo.getMessages(sessionManager.getUserId(), messageEntity.receiverId)
                                 (messageRecyclerView.adapter as? SimpleMessageAdapter)?.submitList(messages.filter { !it.isDeleted })
                             }
                         }
                     } catch (e: Exception) {
                         Log.e("ChatActivity", "Delete failed: ${e.message}")
                     }
                 }
                 dialog.dismiss()
             }
             .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
             .show()
     }
 }

