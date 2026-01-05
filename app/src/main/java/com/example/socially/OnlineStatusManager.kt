package com.example.socially

// FIREBASE REMOVED - This manager is no longer needed
// Online status is now handled via REST API: update_status.php

import android.content.Context
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * OnlineStatusManager - REST API Version
 * Replaces Firebase Presence system
 */
@Suppress("UNUSED")
class OnlineStatusManager(context: Context) {

    private val sessionManager = SessionManager(context)
    
    init {
        RetrofitClient.init(sessionManager)
    }
    
    /**
     * Set user online
     */
    @Suppress("UNUSED")
    fun setOnline() {
        updateStatus(true)
    }
    
    /**
     * Set user offline
     */
    @Suppress("UNUSED")
    fun setOffline() {
        updateStatus(false)
    }
    
    /**
     * Update user status via REST API (Replaces Firebase)
     */
    private fun updateStatus(isOnline: Boolean) {
        if (!sessionManager.isLoggedIn()) return
        
        val request = StatusRequest(
            user_id = sessionManager.getUserId(),
            is_online = isOnline
        )
        
        RetrofitClient.apiService.updateStatus(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                // Status updated
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                // Ignore error
            }
        })
    }
}