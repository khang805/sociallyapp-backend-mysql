package com.example.socially

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.socially.api.RetrofitClient
import com.example.socially.api.StoriesResponse
import com.example.socially.api.StoryData
import com.example.socially.auth.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URL

class StoryViewerActivity : AppCompatActivity() {
    
    private lateinit var storyImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var closeButton: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var sessionManager: SessionManager
    private var currentStoryIndex = 0
    private var stories = listOf<StoryData>()
    private val BASE_URL = "http://10.0.2.2/Assignment-3/"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_viewer)
        
        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)
        
        storyImageView = findViewById(R.id.storyImage)
        usernameTextView = findViewById(R.id.storyUsername)
        closeButton = findViewById(R.id.closeButton)
        progressBar = findViewById(R.id.storyProgressBar)
        
        val userId = intent.getIntExtra("user_id", -1)
        val username = intent.getStringExtra("username") ?: "User"
        
        usernameTextView.text = username
        
        closeButton.setOnClickListener {
            finish()
        }
        
        storyImageView.setOnClickListener {
            // Go to next story or close
            currentStoryIndex++
            if (currentStoryIndex < stories.size) {
                displayStory(stories[currentStoryIndex])
            } else {
                finish()
            }
        }
        
        if (userId != -1) {
            loadUserStories(userId, username)
        } else {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun loadUserStories(userId: Int, username: String) {
        android.util.Log.d("StoryViewer", "Loading stories for user: $userId ($username)")
        
        RetrofitClient.apiService.getStories(sessionManager.getUserId())
            .enqueue(object : Callback<StoriesResponse> {
                override fun onResponse(call: Call<StoriesResponse>, response: Response<StoriesResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val storyGroups = response.body()?.stories ?: emptyList()
                        android.util.Log.d("StoryViewer", "Received ${storyGroups.size} story groups")
                        
                        val userStoryGroup = storyGroups.find { it.user_id == userId }
                        
                        if (userStoryGroup != null && userStoryGroup.stories.isNotEmpty()) {
                            stories = userStoryGroup.stories
                            android.util.Log.d("StoryViewer", "Found ${stories.size} stories for user $username")
                            stories.forEach { story ->
                                android.util.Log.d("StoryViewer", "Story: id=${story.id}, url=${story.media_url}")
                            }
                            displayStory(stories[0])
                        } else {
                            android.util.Log.e("StoryViewer", "No stories found for user $userId")
                            Toast.makeText(this@StoryViewerActivity, "No stories available", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        android.util.Log.e("StoryViewer", "Failed to load stories: ${response.code()}")
                        Toast.makeText(this@StoryViewerActivity, "Failed to load stories", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                
                override fun onFailure(call: Call<StoriesResponse>, t: Throwable) {
                    android.util.Log.e("StoryViewer", "Error loading stories: ${t.message}", t)
                    Toast.makeText(this@StoryViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }
    
    private fun displayStory(story: StoryData) {
        val imageUrl = if (story.media_url.startsWith("http")) {
            story.media_url
        } else {
            BASE_URL + story.media_url
        }
        
        android.util.Log.d("StoryViewer", "Loading story image from: $imageUrl")
        
        progressBar.visibility = android.view.View.VISIBLE
        storyImageView.visibility = android.view.View.GONE
        
        // Load image in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(imageUrl)
                val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        storyImageView.setImageBitmap(bitmap)
                        storyImageView.visibility = android.view.View.VISIBLE
                        progressBar.visibility = android.view.View.GONE
                        android.util.Log.d("StoryViewer", "Story image loaded successfully")
                    } else {
                        progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this@StoryViewerActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                        android.util.Log.e("StoryViewer", "Bitmap is null")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this@StoryViewerActivity, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("StoryViewer", "Error loading image: ${e.message}", e)
                }
            }
        }
    }
}
