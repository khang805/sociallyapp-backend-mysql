package com.example.socially

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Highlight : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_highlight)
        val close_button=findViewById<ImageView>(R.id.close_button)
        close_button.setOnClickListener {
            val intent= Intent(this, Profile::class.java)
            startActivity(intent)
            finish()
        }

    }
}