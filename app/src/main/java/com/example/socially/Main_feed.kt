package com.example.socially

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import com.example.socially.repository.PostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Main Feed Activity - REST API Version (No Firebase)
 */
class Main_feed : AppCompatActivity() {

    private val CAMERA_POST_REQUEST_CODE = 101
    private val GALLERY_POST_REQUEST_CODE = 102
    private val CAMERA_STORY_REQUEST_CODE = 201
    private val GALLERY_STORY_REQUEST_CODE = 202

    private lateinit var cameraIcon: ImageView
    private lateinit var capturedImage: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var emptyStateLayout: LinearLayout
    private val postsList = mutableListOf<Post>()
    private lateinit var sessionManager: SessionManager
    private lateinit var storiesContainer: LinearLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_feed)

        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)

        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Login_Page::class.java))
            finish()
            return
        }

        cameraIcon = findViewById(R.id.camera)
        capturedImage = findViewById(R.id.captured_image)
        emptyStateLayout = findViewById(R.id.emptyStateFeed)
        storiesContainer = findViewById(R.id.story_setup)

        setupRecyclerView()
        loadPostsFromAPI()
        loadStoriesFromAPI()

        cameraIcon.setOnClickListener { showImagePickerDialog(isForStory = false) }
        findViewById<ImageView>(R.id.top_share).setOnClickListener { startActivity(Intent(this, dm_page::class.java)) }
        findViewById<ImageView>(R.id.nav_reel).setOnClickListener { showImagePickerDialog(isForStory = true) }
        findViewById<ImageView>(R.id.nav_home).setOnClickListener { loadPostsFromAPI() }
        findViewById<ImageView>(R.id.nav_search).setOnClickListener { startActivity(Intent(this, Explore_page::class.java)) }
        findViewById<ImageView>(R.id.nav_heart).setOnClickListener { startActivity(Intent(this, Notification_You::class.java)) }
        findViewById<ImageView>(R.id.nav_profile).setOnClickListener { startActivity(Intent(this, Profile::class.java)) }

        updateOnlineStatus(true)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewPosts)
        postAdapter = PostAdapter(
            postList = postsList,
            onLikeClicked = { position -> toggleLike(position) },
            onSaveClicked = { position ->
                val post = postsList[position]
                post.isSaved = !post.isSaved
                postAdapter.notifyItemChanged(position)
                Toast.makeText(this, if (post.isSaved) "Post saved" else "Post unsaved", Toast.LENGTH_SHORT).show()
            },
            onCommentClicked = { position ->
                val post = postsList[position]
                val intent = Intent(this, Comment::class.java)
                intent.putExtra("postId", post.postId.toIntOrNull() ?: 0)
                startActivity(intent)
            },
            onShareClicked = { position ->
                Toast.makeText(this, "Share feature coming soon", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = postAdapter
    }

    private fun loadPostsFromAPI() {
        val userId = sessionManager.getUserId()
        android.util.Log.d("Main_feed", "Loading posts for user: $userId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = PostRepository(this@Main_feed)
                repo.refreshPostsFromServer(userId)
                val cached = repo.getCachedPosts()
                
                android.util.Log.d("Main_feed", "Retrieved ${cached.size} cached posts from local database")
                
                launch(Dispatchers.Main) {
                    val BASE_URL = "http://10.0.2.2/Assignment-3/"
                    
                    postsList.clear()
                    cached.forEach { e ->
                        android.util.Log.d("Main_feed", "Post ${e.id}: user=${e.username}, imageUrl=${e.imageUrl}, likes=${e.likesCount}")
                        
                        // Convert relative URLs to absolute URLs
                        val fullImageUrl = if (e.imageUrl.startsWith("http")) {
                            e.imageUrl
                        } else if (e.imageUrl.isNotEmpty()) {
                            BASE_URL + e.imageUrl
                        } else {
                            ""
                        }
                        
                        val fullAvatarUrl = if (e.profilePicture != null && e.profilePicture.startsWith("http")) {
                            e.profilePicture
                        } else if (e.profilePicture != null && e.profilePicture.isNotEmpty()) {
                            BASE_URL + e.profilePicture
                        } else {
                            ""
                        }
                        
                        android.util.Log.d("Main_feed", "Full image URL: $fullImageUrl")
                        
                        val post = Post(
                            postId = e.id.toString(),
                            userId = e.userId.toString(),
                            username = e.username,
                            postImageUrl = fullImageUrl,
                            avatarUrl = fullAvatarUrl,
                            likes = e.likesCount,
                            caption = e.caption ?: "",
                            timestamp = 0L,
                            likedBy = mutableMapOf(),
                            comments = e.commentsCount
                        )
                        post.isLiked = e.isLiked
                        postsList.add(post)
                    }
                    
                    android.util.Log.d("Main_feed", "Displaying ${postsList.size} posts in RecyclerView")
                    postAdapter.notifyDataSetChanged()
                    updateEmptyState()
                    
                    if (postsList.isEmpty()) {
                        Toast.makeText(this@Main_feed, "No posts to display. Follow users or create a post!", Toast.LENGTH_LONG).show()
                    } else {
                        android.util.Log.d("Main_feed", "Feed updated successfully with ${postsList.size} posts")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Main_feed", "Error loading posts: ${e.message}", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(this@Main_feed, "Failed to load posts: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleLike(position: Int) {
        val post = postsList[position]
        val postId = post.postId.toIntOrNull() ?: return
        val request = LikeRequest(post_id = postId, user_id = sessionManager.getUserId())
        RetrofitClient.apiService.toggleLike(request).enqueue(object : Callback<LikeResponse> {
            override fun onResponse(call: Call<LikeResponse>, response: Response<LikeResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val likeResponse = response.body()!!
                    post.isLiked = likeResponse.action == "liked"
                    post.likes = likeResponse.likes_count
                    post.likesCount = likeResponse.likes_count
                    postAdapter.notifyItemChanged(position)
                }
            }
            override fun onFailure(call: Call<LikeResponse>, t: Throwable) {
                Toast.makeText(this@Main_feed, "Failed to toggle like", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateEmptyState() {
        if (postsList.isEmpty()) {
            recyclerView.visibility = android.view.View.GONE
            emptyStateLayout.visibility = android.view.View.VISIBLE
        } else {
            recyclerView.visibility = android.view.View.VISIBLE
            emptyStateLayout.visibility = android.view.View.GONE
        }
    }

    private fun showImagePickerDialog(isForStory: Boolean) {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(this)
            .setTitle(if (isForStory) "Upload Story" else "Upload Post")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera(if (isForStory) CAMERA_STORY_REQUEST_CODE else CAMERA_POST_REQUEST_CODE)
                    1 -> openGallery(if (isForStory) GALLERY_STORY_REQUEST_CODE else GALLERY_POST_REQUEST_CODE)
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun openCamera(requestCode: Int) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, requestCode)
    }

    private fun openGallery(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_POST_REQUEST_CODE -> {
                    val photo = data?.extras?.get("data") as? Bitmap
                    photo?.let { capturedImage.setImageBitmap(it); uploadPost(bitmapToBase64(it)) }
                }
                GALLERY_POST_REQUEST_CODE -> {
                    val selectedImageUri = data?.data
                    selectedImageUri?.let { capturedImage.setImageURI(it); uploadPost(bitmapToBase64(uriToBitmap(it))) }
                }
                CAMERA_STORY_REQUEST_CODE -> {
                    val photo = data?.extras?.get("data") as? Bitmap
                    photo?.let { uploadStory(bitmapToBase64(it)) }
                }
                GALLERY_STORY_REQUEST_CODE -> {
                    val selectedImageUri = data?.data
                    selectedImageUri?.let { uploadStory(bitmapToBase64(uriToBitmap(it))) }
                }
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    private fun uploadPost(imageBase64: String) {
        Toast.makeText(this, "Uploading post...", Toast.LENGTH_SHORT).show()
        val request = UploadPostRequest(user_id = sessionManager.getUserId(), caption = "", image_base64 = imageBase64)
        RetrofitClient.apiService.uploadPost(request).enqueue(object : Callback<UploadPostResponse> {
            override fun onResponse(call: Call<UploadPostResponse>, response: Response<UploadPostResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@Main_feed, "Post uploaded successfully!", Toast.LENGTH_SHORT).show()
                    loadPostsFromAPI()
                } else {
                    Toast.makeText(this@Main_feed, "Failed to upload post", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UploadPostResponse>, t: Throwable) {
                Toast.makeText(this@Main_feed, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadStory(imageBase64: String) {
        Toast.makeText(this, "Uploading story...", Toast.LENGTH_SHORT).show()
        val request = UploadStoryRequest(user_id = sessionManager.getUserId(), media_base64 = imageBase64, media_type = "image")
        RetrofitClient.apiService.uploadStory(request).enqueue(object : Callback<UploadStoryResponse> {
            override fun onResponse(call: Call<UploadStoryResponse>, response: Response<UploadStoryResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@Main_feed, "Story uploaded successfully!", Toast.LENGTH_SHORT).show()
                    loadStoriesFromAPI() // Reload stories to show the new one
                } else {
                    Toast.makeText(this@Main_feed, "Failed to upload story", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UploadStoryResponse>, t: Throwable) {
                Toast.makeText(this@Main_feed, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadStoriesFromAPI() {
        val userId = sessionManager.getUserId()
        android.util.Log.d("Main_feed", "Loading stories for user: $userId")
        
        RetrofitClient.apiService.getStories(userId).enqueue(object : Callback<StoriesResponse> {
            override fun onResponse(call: Call<StoriesResponse>, response: Response<StoriesResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val storyGroups = response.body()?.stories ?: emptyList()
                    android.util.Log.d("Main_feed", "Received ${storyGroups.size} story groups from API")
                    displayStories(storyGroups)
                } else {
                    android.util.Log.e("Main_feed", "Failed to load stories: ${response.code()} - ${response.message()}")
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("Main_feed", "Error body: $errorBody")
                }
            }
            override fun onFailure(call: Call<StoriesResponse>, t: Throwable) {
                android.util.Log.e("Main_feed", "Error loading stories: ${t.message}", t)
            }
        })
    }

    private fun displayStories(storyGroups: List<StoryGroup>) {
        storiesContainer.removeAllViews()
        
        android.util.Log.d("Main_feed", "Displaying ${storyGroups.size} story groups")
        
        if (storyGroups.isEmpty()) {
            android.util.Log.d("Main_feed", "No stories to display")
            return
        }
        
        storyGroups.forEachIndexed { index, group ->
            android.util.Log.d("Main_feed", "Story $index: user=${group.username}, stories_count=${group.stories.size}")
            
            val storyView = layoutInflater.inflate(R.layout.story_item, storiesContainer, false)
            val profileImage = storyView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.story_profile_image)
            val username = storyView.findViewById<android.widget.TextView>(R.id.story_username)
            
            // Set username - show "Your Story" if it's the current user
            if (group.user_id == sessionManager.getUserId()) {
                username.text = "Your Story"
            } else {
                username.text = group.username
            }
            
            // Load profile image if available
            if (!group.profile_picture.isNullOrEmpty()) {
                // You can use Glide or Picasso here to load the image
                // For now, using a placeholder
            }
            
            storyView.setOnClickListener {
                val intent = Intent(this, StoryViewerActivity::class.java)
                intent.putExtra("user_id", group.user_id)
                intent.putExtra("username", group.username)
                startActivity(intent)
            }
            
            storiesContainer.addView(storyView)
        }
        
        android.util.Log.d("Main_feed", "Added ${storiesContainer.childCount} story views to container")
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        val request = StatusRequest(user_id = sessionManager.getUserId(), is_online = isOnline)
        RetrofitClient.apiService.updateStatus(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {}
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
        })
    }

    override fun onResume() {
        super.onResume()
        loadPostsFromAPI()
        loadStoriesFromAPI()
        updateOnlineStatus(true)
    }

    override fun onPause() {
        super.onPause()
    }
}
