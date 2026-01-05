package com.example.socially

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Edit Profile Activity
 */
class Edit_profile : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var bioEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)

        bioEditText = findViewById(R.id.bio_input)

        // Load current profile
        loadProfile()

        // Done button (save)
        findViewById<TextView>(R.id.done_btn)?.setOnClickListener {
            saveProfile()
        }
        
        // Cancel button
        findViewById<TextView>(R.id.cancel_btn)?.setOnClickListener {
            finish()
        }
    }

    private fun loadProfile() {
        RetrofitClient.apiService.getUserProfile(sessionManager.getUserId())
            .enqueue(object : Callback<UserProfileResponse> {
                override fun onResponse(call: Call<UserProfileResponse>, response: Response<UserProfileResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val user = response.body()!!.user
                        
                        // Populate form fields with user data
                        bioEditText.setText(user.bio ?: "")
                    }
                }

                override fun onFailure(call: Call<UserProfileResponse>, t: Throwable) {
                    Toast.makeText(this@Edit_profile, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveProfile() {
        // Get form data and update profile
        val bio = bioEditText.text.toString()
        
        val request = UpdateProfileRequest(
            user_id = sessionManager.getUserId(),
            bio = bio
        )

        RetrofitClient.apiService.updateProfile(request).enqueue(object : Callback<UpdateProfileResponse> {
            override fun onResponse(call: Call<UpdateProfileResponse>, response: Response<UpdateProfileResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@Edit_profile, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@Edit_profile, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UpdateProfileResponse>, t: Throwable) {
                Toast.makeText(this@Edit_profile, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
