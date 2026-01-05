package com.example.socially.utils

import retrofit2.Response
import org.json.JSONObject

object ApiUtils {
    /**
     * Extracts a readable error message from a Retrofit Response's errorBody.
     * If the body is JSON and contains a "message" field, that value is returned.
     * Otherwise returns the raw error body or a fallback containing the HTTP code.
     */
    fun extractErrorMessage(response: Response<*>): String {
        return try {
            val errBody = response.errorBody()?.string()
            if (!errBody.isNullOrEmpty()) {
                try {
                    val json = JSONObject(errBody)
                    json.optString("message", "HTTP ${response.code()} error")
                } catch (e: Exception) {
                    // Not JSON — return raw body trimmed
                    errBody.trim()
                }
            } else {
                "HTTP ${response.code()} error"
            }
        } catch (e: Exception) {
            "Unknown error: ${e.message}"
        }
    }
}

