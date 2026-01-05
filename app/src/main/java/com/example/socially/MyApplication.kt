package com.example.socially

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.socially.api.RetrofitClient
import com.example.socially.api.StatusRequest
import com.example.socially.api.GenericResponse
import com.example.socially.auth.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Picasso + OkHttp for caching
import com.squareup.picasso.Picasso
import com.squareup.picasso.OkHttp3Downloader
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Application class - Handles app-wide initialization and lifecycle
 */
class MyApplication : Application() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // Initialize Retrofit with SessionManager
        RetrofitClient.init(sessionManager)

        // Initialize Picasso with OkHttp downloader and disk cache for offline image caching
        try {
            val cacheSize = 50L * 1024L * 1024L // 50 MB
            val cacheDir = File(cacheDir, "picasso-cache")
            val cache = Cache(cacheDir, cacheSize)

            val okHttp = OkHttpClient.Builder()
                .cache(cache)
                .build()

            val downloader = OkHttp3Downloader(okHttp)
            val picasso = Picasso.Builder(this)
                .downloader(downloader)
                .build()
            Picasso.setSingletonInstance(picasso)
        } catch (_: Exception) {
            // Ignore and fallback to default Picasso
        }

        // Setup app lifecycle observer for online/offline status
        setupLifecycleObserver()

        // Schedule periodic offline sync
        com.example.socially.utils.NetworkUtils.schedulePeriodicSync(this)
    }

    /**
     * Monitor app foreground/background state for online status updates
     */
    private fun setupLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        // App moved to foreground
                        updateOnlineStatus(true)
                    }
                    Lifecycle.Event.ON_STOP -> {
                        // App moved to background
                        updateOnlineStatus(false)
                    }
                    else -> {}
                }
            }
        })
    }

    /**
     * Update user's online/offline status on the server
     */
    private fun updateOnlineStatus(isOnline: Boolean) {
        if (!sessionManager.isLoggedIn()) return

        val userId = sessionManager.getUserId()
        if (userId <= 0) return

        val request = StatusRequest(userId, isOnline)

        RetrofitClient.apiService.updateStatus(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                // Status updated successfully
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                // Failed to update status, not critical
            }
        })
    }
}
