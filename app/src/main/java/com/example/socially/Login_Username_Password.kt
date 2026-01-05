package com.example.socially

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Login Activity
 * Handles user authentication via REST API + SessionManager (No Firebase Auth)
 */
class Login_Username_Password : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login_username_password)

        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)

        val emailField = findViewById<EditText>(R.id.username)
        val passwordField = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<TextView>(R.id.loginButton)
        val backArrow = findViewById<ImageView>(R.id.backArrow)
        val signUp = findViewById<TextView>(R.id.signUp)

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        backArrow.setOnClickListener {
            startActivity(Intent(this, Login_Page::class.java))
            finish()
        }

        signUp.setOnClickListener {
            startActivity(Intent(this, Sign_up::class.java))
            finish()
        }
    }

    private fun loginUser(email: String, password: String) {
        val request = LoginRequest(
            email = email,
            password = password,
            fcm_token = sessionManager.getFcmToken()
        )

        RetrofitClient.apiService.loginUser(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val data = response.body()!!
                    
                    // Save user session (Replaces Firebase Auth)
                    sessionManager.saveUserSession(
                        userId = data.user_id!!,
                        username = data.username!!,
                        email = data.email!!,
                        authToken = data.token ?: "",
                        profileImage = data.profile_picture
                    )
                    
                    // Update online status
                    updateUserStatus(data.user_id, true)

                    Toast.makeText(this@Login_Username_Password, 
                        "Login successful!", Toast.LENGTH_SHORT).show()
                    
                    // Navigate to Main Feed
                    val intent = Intent(this@Login_Username_Password, Main_feed::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val msg = response.body()?.message ?: "Login failed"
                    Toast.makeText(this@Login_Username_Password,
                        "Login failed: $msg",
                        Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@Login_Username_Password,
                    "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
    
    @Suppress("ALWAYS_TRUE")
    private fun updateUserStatus(userId: Int, isOnline: Boolean) {
        val request = StatusRequest(user_id = userId, is_online = isOnline)
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
