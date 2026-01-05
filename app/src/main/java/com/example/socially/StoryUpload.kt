package com.example.socially

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import com.example.socially.db.AppDatabase
import com.example.socially.db.StoryEntity
import com.example.socially.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

/**
 * Story Upload Activity
 * Uploads stories via REST API with offline support
 */
class StoryUpload : AppCompatActivity() {
    
    private lateinit var storyImage: ImageView
    private lateinit var sendStoryButton: ImageView
    private lateinit var cancelButton: ImageView
    private var selectedImageUri: Uri? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var database: AppDatabase
    
    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_story_upload)
        
        sessionManager = SessionManager(this)
        database = AppDatabase.getDatabase(this)
        
        storyImage = findViewById(R.id.storyImage)
        sendStoryButton = findViewById(R.id.sendStoryButton)
        cancelButton = findViewById(R.id.cancelButton)
        
        // Load image from intent if available
        val imageUri = intent.getStringExtra("imageUri")
        if (imageUri != null) {
            selectedImageUri = imageUri.toUri()
            loadImageFromUri(selectedImageUri!!)
        }
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        sendStoryButton.setOnClickListener {
            if (selectedImageUri != null) {
                uploadStory()
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }
        
        cancelButton.setOnClickListener {
            finish()
        }
        
        storyImage.setOnClickListener {
            openImagePicker()
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        @Suppress("DEPRECATION")
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    
    private fun loadImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            storyImage.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun uploadStory() {
        selectedImageUri?.let { uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val encodedImage = bitmapToBase64(bitmap)
                
                val request = UploadStoryRequest(
                    user_id = sessionManager.getUserId(),
                    media_base64 = encodedImage,
                    media_type = "image"
                )
                
                if (NetworkUtils.isOnline(this)) {
                    // Upload directly if online
                    uploadToServer(request)
                } else {
                    // Queue for later if offline
                    queueStoryForUpload(request)
                }
                
            } catch (e: Exception) {
                Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun uploadToServer(request: UploadStoryRequest) {
        RetrofitClient.apiService.uploadStory(request).enqueue(object : Callback<UploadStoryResponse> {
            override fun onResponse(call: Call<UploadStoryResponse>, response: Response<UploadStoryResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val data = response.body()!!
                    Toast.makeText(this@StoryUpload, "Story uploaded successfully!", Toast.LENGTH_SHORT).show()
                    
                    // Save to local database for offline viewing
                    data.story_id?.let { storyId ->
                        saveStoryLocally(storyId, data.media_url ?: "", data.expires_at ?: "")
                    }
                    
                    finish()
                } else {
                    Toast.makeText(this@StoryUpload, "Failed to upload story", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<UploadStoryResponse>, t: Throwable) {
                Toast.makeText(this@StoryUpload, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                
                // Queue for offline upload
                queueStoryForUpload(request)
            }
        })
    }
    
    private fun queueStoryForUpload(request: UploadStoryRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val actionData = mapOf(
                    "user_id" to request.user_id,
                    "media_base64" to request.media_base64,
                    "media_type" to request.media_type
                )
                
                val queueEntity = com.example.socially.db.OfflineQueueEntity(
                    userId = sessionManager.getUserId(),
                    actionType = "story",
                    actionData = com.google.gson.Gson().toJson(actionData),
                    timestamp = System.currentTimeMillis()
                )
                
                database.offlineQueueDao().insert(queueEntity)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StoryUpload, 
                        "Story queued. Will upload when online.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                
                // Schedule sync
                NetworkUtils.scheduleOfflineSync(this@StoryUpload)
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StoryUpload, 
                        "Error queuing story: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun saveStoryLocally(storyId: Int, mediaUrl: String, expiresAt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val storyEntity = StoryEntity(
                id = storyId,
                userId = sessionManager.getUserId(),
                username = sessionManager.getUsername(),
                profilePicture = null,
                mediaUrl = mediaUrl,
                mediaType = "image",
                createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date()),
                expiresAt = expiresAt,
                isSynced = true
            )
            database.storyDao().insertAll(listOf(storyEntity))
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        @Suppress("DEPRECATION")
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let { loadImageFromUri(it) }
        }
    }
}
