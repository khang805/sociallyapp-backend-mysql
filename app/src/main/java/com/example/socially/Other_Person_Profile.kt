package com.example.socially

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Other_Person_Profile : AppCompatActivity() {
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.other_person_profile)
    val nav_home=findViewById<ImageView>(R.id.nav_home)
        nav_home.setOnClickListener {
            val intent = Intent(this, Main_feed::class.java)
            startActivity(intent)   // <-- missing line
            finish()
        }
        val nav_search=findViewById<ImageView>(R.id.nav_search)
        nav_search.setOnClickListener {
            val intent = Intent(this, Explore_page::class.java)
            startActivity(intent)   // <-- missing line
            finish()
        }
        val nav_heart=findViewById<ImageView>(R.id.nav_heart)
        nav_heart.setOnClickListener {
            val intent = Intent(this, Notification_You::class.java)
            startActivity(intent)   // <-- missing line
            finish()
        }
        val nav_profile=findViewById<ImageView>(R.id.nav_profile)
        nav_profile.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)   // <-- missing line
            finish()
        }
        val back_icon=findViewById<ImageView>(R.id.back_icon)
        back_icon.setOnClickListener {
            val intent = Intent(this, Main_feed::class.java)
            startActivity(intent)   // <-- missing line
            finish()
        }

    }



}