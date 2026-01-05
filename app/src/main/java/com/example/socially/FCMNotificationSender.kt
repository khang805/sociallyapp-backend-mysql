package com.example.socially

/**
 * FCM Notification Sender - Uses SessionManager
 * Sends notifications via backend API
 */
@Suppress("UNUSED")
class FCMNotificationSender {

    @Suppress("UNUSED_PARAMETER")
    fun sendNotification(targetUserId: Int, title: String, body: String) {
        // FCM notifications are sent via backend when actions occur
        // The backend handles FCM token lookup and notification sending
        // This class is kept for compatibility but actual sending happens server-side
        
        // Example: When you like a post, the backend API automatically sends
        // a notification to the post owner using their FCM token
    }
}
