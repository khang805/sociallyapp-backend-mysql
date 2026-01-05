package com.example.socially

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Notification_following : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification_following)
        val tab_you=findViewById<TextView>(R.id.tab_you)
        tab_you.setOnClickListener {
            val intent= Intent(this, Notification_You::class.java)
            startActivity(intent)
            finish()
        }
        val nav_home=findViewById<ImageView>(R.id.nav_home)
        nav_home.setOnClickListener {
            val intent= Intent(this, Main_feed::class.java)
            startActivity(intent)
            finish()
        }
        val nav_search=findViewById<ImageView>(R.id.nav_search)
        nav_search.setOnClickListener {
            val intent= Intent(this, Explore_page::class.java)
            startActivity(intent)
            finish()
        }
        val nav_profile=findViewById<ImageView>(R.id.nav_profile)
        nav_profile.setOnClickListener {
            val intent= Intent(this, Profile::class.java)
            startActivity(intent)
            finish()
        }


    }
}