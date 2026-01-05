package com.example.socially.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.socially.Main_feed
import com.example.socially.R
import com.example.socially.api.RetrofitClient
import com.example.socially.api.StatusRequest
import com.example.socially.auth.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Firebase Cloud Messaging Service
 * Handles push notifications for messages, follow requests, and screenshot alerts
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "social_media_notifications"
        private const val CHANNEL_NAME = "Social Media Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Save FCM token to SessionManager
        val sessionManager = SessionManager(this)
        sessionManager.saveFcmToken(token)
        
        // If user is logged in, send token to server
        if (sessionManager.isLoggedIn()) {
            updateFcmTokenOnServer(token, sessionManager.getUserId())
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "New Notification"
            val body = notification.body ?: ""
            val type = remoteMessage.data["type"] ?: "general"
            
            showNotification(title, body, type)
        }
        
        // Handle data payload
        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"] ?: "New Notification"
            val body = remoteMessage.data["body"] ?: ""
            val type = remoteMessage.data["type"] ?: "general"
            
            showNotification(title, body, type)
        }
    }

    private fun showNotification(title: String, message: String, type: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for messages, follows, and alerts"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent based on notification type
        val intent = when (type) {
            "message" -> Intent(this, Main_feed::class.java).apply {
                putExtra("navigate_to", "messages")
            }
            "follow_request" -> Intent(this, Main_feed::class.java).apply {
                putExtra("navigate_to", "notifications")
            }
            "screenshot" -> Intent(this, Main_feed::class.java).apply {
                putExtra("navigate_to", "screenshot_alerts")
            }
            else -> Intent(this, Main_feed::class.java)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Using default icon
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Update FCM token on server
     */
    private fun updateFcmTokenOnServer(token: String, userId: Int) {
        // You can create a dedicated endpoint for this or include in status updates
        val request = StatusRequest(userId, true)
        
        RetrofitClient.apiService.updateStatus(request).enqueue(object : Callback<com.example.socially.api.GenericResponse> {
            override fun onResponse(
                call: Call<com.example.socially.api.GenericResponse>,
                response: Response<com.example.socially.api.GenericResponse>
            ) {
                // Token updated successfully
            }

            override fun onFailure(call: Call<com.example.socially.api.GenericResponse>, t: Throwable) {
                // Failed to update token, will retry on next app launch
            }
        })
    }
}
