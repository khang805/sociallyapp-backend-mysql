package com.example.socially

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Followers List Activity - Shows followers and following
 */
class FollowersListActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var followersTab: TextView
    private lateinit var followingTab: TextView
    private val users = mutableListOf<UserData>()
    private lateinit var adapter: ConversationAdapter
    private var currentTab = "followers"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers_list)

        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)

        val userId = intent.getStringExtra("userId")?.toIntOrNull() ?: sessionManager.getUserId()
        val tab = intent.getStringExtra("tab") ?: "followers"
        currentTab = tab

        recyclerView = findViewById(R.id.usersRecyclerView)
        followersTab = findViewById(R.id.followersTab)
        followingTab = findViewById(R.id.followingTab)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Setup adapter
        adapter = ConversationAdapter { user ->
            // Handle user click - open profile
            // For now, just show a toast
            Toast.makeText(this, "Clicked: ${user.username}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter

        // Back button
        findViewById<ImageView>(R.id.back_arrow).setOnClickListener {
            finish()
        }

        // Tab switching
        followersTab.setOnClickListener {
            currentTab = "followers"
            updateTabUI()
            loadFollowers(userId, "followers")
        }

        followingTab.setOnClickListener {
            currentTab = "following"
            updateTabUI()
            loadFollowers(userId, "following")
        }

        loadFollowers(userId, tab)
    }

    private fun updateTabUI() {
        if (currentTab == "followers") {
            followersTab.setTextColor(resources.getColor(android.R.color.black, null))
            followersTab.setTypeface(null, android.graphics.Typeface.BOLD)
            followingTab.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            followingTab.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            followersTab.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            followersTab.setTypeface(null, android.graphics.Typeface.NORMAL)
            followingTab.setTextColor(resources.getColor(android.R.color.black, null))
            followingTab.setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    private fun loadFollowers(userId: Int, tab: String) {
        RetrofitClient.apiService.getFollowers(userId).enqueue(object : Callback<FollowersResponse> {
            override fun onResponse(call: Call<FollowersResponse>, response: Response<FollowersResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    users.clear()
                    
                    if (tab == "followers") {
                        users.addAll(response.body()!!.followers)
                    } else {
                        users.addAll(response.body()!!.following)
                    }
                    
                    adapter.submitList(users.toList())

                    if (users.isEmpty()) {
                        Toast.makeText(this@FollowersListActivity, "No $tab found", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@FollowersListActivity, "${users.size} $tab loaded", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@FollowersListActivity, "Failed to load $tab: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<FollowersResponse>, t: Throwable) {
                Toast.makeText(this@FollowersListActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
