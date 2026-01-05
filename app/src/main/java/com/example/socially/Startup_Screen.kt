package com.example.socially

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.socially.auth.SessionManager

/**
 * Splash Screen Activity
 * - Displays for 5 seconds as per requirements
 * - Checks user authentication status
 * - Routes to appropriate screen (Login, Profile Setup, or Home)
 */
class Startup_Screen : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.startup_screen)

        sessionManager = SessionManager(this)

        // If the user is already logged in, skip the full splash delay and
        // immediately navigate to the appropriate screen (home or profile setup).
        if (sessionManager.isLoggedIn()) {
            checkAuthStatus()
            return
        }

        // Display splash screen for 5 seconds for unauthenticated users
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthStatus()
        }, 5000) // 5 seconds
    }

    private fun checkAuthStatus() {
        if (sessionManager.isLoggedIn()) {
            // Always send logged-in users to Main Feed for a consistent experience
            navigateToMainFeed()
        } else {
            // User not logged in - go to login
            navigateToLogin()
        }
    }

    private fun navigateToMainFeed() {
        val intent = Intent(this, Main_feed::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToProfileSetup() {
        val intent = Intent(this, Edit_profile::class.java)
        intent.putExtra("is_first_time", true)
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, Login_Page::class.java)
        startActivity(intent)
        finish()
    }
}