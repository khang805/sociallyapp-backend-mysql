package com.example.socially

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Profile Activity
 * Uses SessionManager + REST API (No Firebase)
 */
class Profile : AppCompatActivity() {

    private lateinit var profilePicture: CircleImageView
    private lateinit var sessionManager: SessionManager
    private val UPDATE_PROFILE_PIC_CODE = 3000

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)

        // Check if logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, Login_Page::class.java))
            finish()
            return
        }

        profilePicture = findViewById(R.id.profile_picture)

        // Load user profile via API
        loadUserProfile()

        // Profile picture click - Update picture
        profilePicture.setOnClickListener {
            val intent = Intent(this, UpdateProfilePictureActivity::class.java)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, UPDATE_PROFILE_PIC_CODE)
        }

        // Followers count click - View followers list
        findViewById<RelativeLayout>(R.id.followers_stat).setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java)
            intent.putExtra("userId", sessionManager.getCurrentUserId())
            intent.putExtra("tab", "followers")
            startActivity(intent)
        }

        // Following count click - View following list
        findViewById<RelativeLayout>(R.id.following_stat).setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java)
            intent.putExtra("userId", sessionManager.getCurrentUserId())
            intent.putExtra("tab", "following")
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this, Main_feed::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, Explore_page::class.java))
        }

        findViewById<ImageView>(R.id.nav_heart).setOnClickListener {
            startActivity(Intent(this, Notification_You::class.java))
        }

        findViewById<RelativeLayout>(R.id.edit_page).setOnClickListener {
            startActivity(Intent(this, Edit_profile::class.java))
        }

        findViewById<RelativeLayout>(R.id.friends_highlight).setOnClickListener {
            startActivity(Intent(this, Highlight::class.java))
        }

        // Logout functionality
        findViewById<TextView>(R.id.logout_text)?.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        val userId = sessionManager.getUserId()

        // Set user offline before logging out
        val statusRequest = StatusRequest(user_id = userId, is_online = false)
        RetrofitClient.apiService.updateStatus(statusRequest).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                // Status updated
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                // Ignore error
            }
        })

        // Clear session (Replaces Firebase Auth signOut)
        sessionManager.clearSession()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Navigate to login
        val intent = Intent(this, Startup_Screen::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Update online status
        updateUserStatus()
    }

    override fun onPause() {
        super.onPause()
        // Don't set offline on pause, only on logout or app close
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        @Suppress("DEPRECATION")
        if (requestCode == UPDATE_PROFILE_PIC_CODE && resultCode == RESULT_OK) {
            // Reload profile picture
            loadUserProfile()
        }
    }

    private fun loadUserProfile() {
        val userId = sessionManager.getUserId()

        // Call API to get user profile (Replaces Firebase Database)
        RetrofitClient.apiService.getUserProfile(userId).enqueue(object : Callback<UserProfileResponse> {
            override fun onResponse(call: Call<UserProfileResponse>, response: Response<UserProfileResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val user = response.body()!!.user

                    // Load profile picture using Picasso
                    if (!user.profile_picture.isNullOrEmpty()) {
                        Picasso.get()
                            .load(user.profile_picture)
                            .placeholder(R.drawable.profile)
                            .error(R.drawable.profile)
                            .into(profilePicture)
                    }

                    // Load followers/following counts
                    findViewById<TextView>(R.id.followers_number)?.text = user.followers_count.toString()
                    findViewById<TextView>(R.id.following_number)?.text = user.following_count.toString()
                    findViewById<TextView>(R.id.posts_number)?.text = user.posts_count.toString()

                    // Load username
                    findViewById<TextView>(R.id.username_text)?.text = user.username

                    // Load bio if available
                    if (!user.bio.isNullOrEmpty()) {
                        findViewById<TextView>(R.id.display_name)?.text = user.bio
                    }
                } else {
                    Toast.makeText(this@Profile, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserProfileResponse>, t: Throwable) {
                Toast.makeText(this@Profile, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUserStatus() {
        val userId = sessionManager.getUserId()
        val request = StatusRequest(user_id = userId, is_online = true)
        RetrofitClient.apiService.updateStatus(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {}
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
        })
    }
}
