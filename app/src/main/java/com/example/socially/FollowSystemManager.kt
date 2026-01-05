package com.example.socially

import android.content.Context
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * FollowSystemManager - REST API Version
 */
class FollowSystemManager {

    data class Friend(val userId: String, val username: String)

    @Suppress("UNUSED")
    fun getFriends(context: Context, callback: (List<Friend>) -> Unit) {
        val sessionManager = SessionManager(context)
        RetrofitClient.init(sessionManager)

        RetrofitClient.apiService.getFollowers(sessionManager.getUserId()).enqueue(object : Callback<FollowersResponse> {
            override fun onResponse(call: Call<FollowersResponse>, response: Response<FollowersResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val followersResponse = response.body()!!
                    val friendsList = mutableListOf<Friend>()
                    
                    // Add followers
                    followersResponse.followers.forEach { user ->
                        friendsList.add(Friend(user.id.toString(), user.username))
                    }
                    
                    // Add following
                    followersResponse.following.forEach { user ->
                        if (friendsList.none { it.userId == user.id.toString() }) {
                            friendsList.add(Friend(user.id.toString(), user.username))
                        }
                    }
                    
                    callback(friendsList)
                } else {
                    callback(emptyList())
                }
            }

            override fun onFailure(call: Call<FollowersResponse>, t: Throwable) {
                callback(emptyList())
            }
        })
    }

    fun sendFollowRequest(context: Context, targetUserId: Int, callback: (Boolean) -> Unit) {
        val sessionManager = SessionManager(context)
        RetrofitClient.init(sessionManager)

        val request = FollowRequest(
            sender_id = sessionManager.getUserId(),
            receiver_id = targetUserId
        )

        RetrofitClient.apiService.sendFollowRequest(request).enqueue(object : Callback<FollowResponse> {
            override fun onResponse(call: Call<FollowResponse>, response: Response<FollowResponse>) {
                callback(response.isSuccessful && response.body()?.status == "success")
            }

            override fun onFailure(call: Call<FollowResponse>, t: Throwable) {
                callback(false)
            }
        })
    }
}