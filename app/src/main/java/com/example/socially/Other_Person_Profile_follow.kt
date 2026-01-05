package com.example.socially

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Other_Person_Profile_follow : AppCompatActivity() {
    private lateinit var followManager: FollowSystemManager
    private lateinit var followButton: Button
    private var targetUserId: String = ""
    private var isFollowing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_other_person_profile_follow)

        followManager = FollowSystemManager()
        targetUserId = intent.getStringExtra("userId") ?: ""
        followButton = findViewById(R.id.follow_button)

        // Check if already following
        checkFollowStatus()

        // Navigation listeners - Don't use finish() to prevent navigation issues
        val nav_home = findViewById<ImageView>(R.id.nav_home)
        nav_home.setOnClickListener {
            try {
                val intent = Intent(this, Main_feed::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
            }
        }
        
        val nav_search = findViewById<ImageView>(R.id.nav_search)
        nav_search.setOnClickListener {
            try {
                val intent = Intent(this, Explore_page::class.java)
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
            }
        }
        
        val nav_heart = findViewById<ImageView>(R.id.nav_heart)
        nav_heart.setOnClickListener {
            try {
                val intent = Intent(this, Notification_You::class.java)
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
            }
        }
        
        val nav_profile = findViewById<ImageView>(R.id.nav_profile)
        nav_profile.setOnClickListener {
            try {
                val intent = Intent(this, Profile::class.java)
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
            }
        }
        
        val back_icon = findViewById<ImageView>(R.id.back_icon)
        back_icon.setOnClickListener {
            // Use finish() for back button to go back to previous screen
            finish()
        }

        // Follow button click listener
        followButton.setOnClickListener {
            if (isFollowing) {
                unfollowUser()
            } else {
                sendFollowRequest()
            }
        }
    }

    private fun checkFollowStatus() {
        // Check if already following by getting followers list
        followManager.getFriends(this) { friends ->
            isFollowing = friends.any { it.userId == targetUserId }
            runOnUiThread { updateFollowButton() }
        }
    }

    private fun updateFollowButton() {
        @Suppress("SetTextI18n")
        if (isFollowing) {
            followButton.text = "Following"
            followButton.isEnabled = true
        } else {
            @Suppress("SetTextI18n")
            followButton.text = "Follow"
            followButton.isEnabled = true
        }
    }

    private fun sendFollowRequest() {
        val userId = targetUserId.toIntOrNull() ?: return
        followManager.sendFollowRequest(this, userId) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this@Other_Person_Profile_follow, "Follow request sent", Toast.LENGTH_SHORT).show()
                    @Suppress("SetTextI18n")
                    followButton.text = "Request Sent"
                    followButton.isEnabled = false
                } else {
                    Toast.makeText(this@Other_Person_Profile_follow, "Failed to send follow request", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun unfollowUser() {
        // Currently no unfollow API endpoint - show message
        Toast.makeText(this, "Unfollow feature coming soon", Toast.LENGTH_SHORT).show()
    }
}