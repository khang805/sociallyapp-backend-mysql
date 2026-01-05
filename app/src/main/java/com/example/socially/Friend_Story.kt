package com.example.socially

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Friend_Story : AppCompatActivity() {
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_friend_story)
 val close_button=findViewById<ImageView>(R.id.close_button)
        close_button.setOnClickListener {
            val intent = Intent(this, Main_feed::class.java)
            startActivity(intent)   // <-- missing line
            finish()
        }

    }
}