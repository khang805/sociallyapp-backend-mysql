package com.example.socially

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


class Explore_page : AppCompatActivity() {
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_explore)
    val nav_home=findViewById<ImageView>(R.id.nav_home)
        nav_home.setOnClickListener {
            val intent = Intent(this, Main_feed::class.java)
            startActivity(intent)
            finish()
        }
        val search_bar_layout=findViewById<LinearLayout>(R.id.search_bar_layout)
        search_bar_layout.setOnClickListener {
            val intent = Intent(this, Search_page::class.java)
            startActivity(intent)
            finish()
        }
        val nav_profile=findViewById<ImageView>(R.id.nav_profile)
        nav_profile.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
            finish()
        }
        val nav_heart=findViewById<ImageView>(R.id.nav_heart)
        nav_heart.setOnClickListener {
            val intent = Intent(this, Notification_You::class.java)
            startActivity(intent)
            finish()
        }


    }
}