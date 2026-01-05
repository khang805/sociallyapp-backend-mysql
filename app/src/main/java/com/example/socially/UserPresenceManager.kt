package com.example.socially

// FIREBASE REMOVED - This manager is no longer needed
// User presence is now handled via REST API: get_user_status.php

/**
 * UserPresenceManager - STUB
 * Firebase Presence system removed
 * Use OnlineStatusManager for REST API based status
 */
class UserPresenceManager {
    
    fun setupPresence() {
        // No longer needed - using REST API
    }
    
    fun setUserOffline() {
        // Use OnlineStatusManager.setOffline() instead
    }
    
    fun setUserOnline() {
        // Use OnlineStatusManager.setOnline() instead
    }
}
