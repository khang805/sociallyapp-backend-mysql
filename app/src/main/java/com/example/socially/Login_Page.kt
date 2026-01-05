package com.example.socially

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.socially.auth.SessionManager
import com.example.socially.api.RetrofitClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class Login_Page : AppCompatActivity() {

    private val client = OkHttpClient()
    private val LOGIN_URL = "http://10.0.2.2/Assignment-3/backend/api/login.php"
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val usernameText = findViewById<TextView>(R.id.username)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpLink = findViewById<TextView>(R.id.signUpLink)
        val switchAccount = findViewById<TextView>(R.id.switchAccount)

        // Get saved username from SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val savedUsername = sharedPref.getString("last_username", null)
        val savedPassword = sharedPref.getString("last_password", null)

        if (savedUsername != null) {
            usernameText.text = savedUsername
        } else {
            usernameText.text = "No saved account"
            loginButton.isEnabled = false
        }

        loginButton.setOnClickListener {
            if (savedUsername != null && savedPassword != null) {
                loginUser(savedUsername, savedPassword)
            } else {
                Toast.makeText(this, "No saved account found", Toast.LENGTH_SHORT).show()
            }
        }

        signUpLink.setOnClickListener {
            val intent = Intent(this@Login_Page, Sign_up::class.java)
            startActivity(intent)
            finish()
        }

        switchAccount.setOnClickListener {
            val intent = Intent(this@Login_Page, Login_Username_Password::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loginUser(username: String, password: String) {
        // initialize session manager and retrofit
        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)

        val json = JSONObject()
        json.put("username", username)
        json.put("password", password)
        json.put("fcm_token", "test_token")

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            json.toString()
        )

        val request = Request.Builder()
            .url(LOGIN_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Login_Page, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    try {
                        val jsonRes = JSONObject(responseBody!!)
                        if (jsonRes.getString("status") == "success") {
                            Toast.makeText(this@Login_Page, "Login successful", Toast.LENGTH_SHORT).show()
                            // Save session securely using SessionManager
                            try {
                                val userId = jsonRes.optInt("user_id", -1)
                                val email = jsonRes.optString("email", "")
                                val profileImage = jsonRes.optString("profile_picture", null)
                                val authToken = jsonRes.optString("auth_token", null)
                                val uname = jsonRes.optString("username", username)

                                if (userId > 0 && authToken != null) {
                                    sessionManager.saveUserSession(
                                        userId = userId,
                                        username = uname,
                                        email = email,
                                        authToken = authToken,
                                        profileImage = profileImage
                                    )
                                }
                            } catch (e: Exception) {
                                // ignore session save errors but continue
                            }

                            val intent = Intent(this@Login_Page, Main_feed::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@Login_Page, jsonRes.getString("message"), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@Login_Page, "Invalid Response", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}