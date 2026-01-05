package com.example.socially

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Story : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_story)
        val close_button=findViewById<ImageView>(R.id.close_button)
        close_button.setOnClickListener {
           val Intent= Intent(this, Main_feed::class.java)
            startActivity(Intent)
            finish()
        }


    }
}