package com.example.socially

import android.content.Context
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Screenshot Detector - REST API Version
 */
class ScreenshotDetector(private val context: Context, private val chatUserId: Int) {
    private val sessionManager = SessionManager(context)

    init {
        RetrofitClient.init(sessionManager)
    }

    fun logScreenshot() {
        val request = ScreenshotRequest(
            chat_user_id = chatUserId,
            screenshot_taker_id = sessionManager.getUserId()
        )

        RetrofitClient.apiService.logScreenshot(request).enqueue(object : Callback<ScreenshotResponse> {
            override fun onResponse(call: Call<ScreenshotResponse>, response: Response<ScreenshotResponse>) {
                // Screenshot logged
            }

            override fun onFailure(call: Call<ScreenshotResponse>, t: Throwable) {
                // Ignore error
            }
        })
    }
}
