package com.example.socially.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SessionManager - Secure session management using EncryptedSharedPreferences
 * Replaces Firebase Authentication with server-side session management
 */
class SessionManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "user_session_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_PROFILE_IMAGE = "profile_image"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    /**
     * Save user session after successful login
     */
    fun saveUserSession(
        userId: Int,
        username: String,
        email: String,
        authToken: String? = null,
        profileImage: String? = null
    ) {
        sharedPreferences.edit().apply {
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putString(KEY_AUTH_TOKEN, authToken)
            putString(KEY_PROFILE_IMAGE, profileImage)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Update FCM token
     */
    fun saveFcmToken(token: String) {
        sharedPreferences.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    /**
     * Get current user ID (replaces Firebase uid)
     */
    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }

    /**
     * Get current user ID as String (Firebase-compatible format)
     */
    fun getCurrentUserId(): String {
        return getUserId().toString()
    }

    /**
     * Get username
     */
    fun getUsername(): String {
        return sharedPreferences.getString(KEY_USERNAME, "") ?: ""
    }

    /**
     * Get email
     */
    fun getEmail(): String {
        return sharedPreferences.getString(KEY_EMAIL, "") ?: ""
    }

    /**
     * Get auth token (for API requests)
     */
    fun getAuthToken(): String {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, "") ?: ""
    }

    /**
     * Get FCM token
     */
    fun getFcmToken(): String {
        return sharedPreferences.getString(KEY_FCM_TOKEN, "") ?: ""
    }

    /**
     * Get profile image
     */
    fun getProfileImage(): String {
        return sharedPreferences.getString(KEY_PROFILE_IMAGE, "") ?: ""
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && getUserId() != -1
    }

    /**
     * Update profile image
     */
    fun updateProfileImage(imageUrl: String) {
        sharedPreferences.edit().putString(KEY_PROFILE_IMAGE, imageUrl).apply()
    }

    /**
     * Update username
     */
    fun updateUsername(username: String) {
        sharedPreferences.edit().putString(KEY_USERNAME, username).apply()
    }

    /**
     * Clear session (logout)
     */
    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * Get all session data as map (for debugging)
     */
    fun debugSessionData(): Map<String, Any> {
        return mapOf(
            "userId" to getUserId(),
            "username" to getUsername(),
            "email" to getEmail(),
            "isLoggedIn" to isLoggedIn(),
            "hasToken" to (getAuthToken().isNotEmpty())
        )
    }
}
