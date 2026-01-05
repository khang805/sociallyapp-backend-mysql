package com.example.socially

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class CommentData(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

class CommentAdapter(private val comments: MutableList<CommentData>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val usernameText: TextView = itemView.findViewById(R.id.commentUsername)
        val commentText: TextView = itemView.findViewById(R.id.commentText)
        val timestampText: TextView = itemView.findViewById(R.id.commentTimestamp)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CommentViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.usernameText.text = comment.username
        holder.commentText.text = comment.text
        holder.timestampText.text = java.text.SimpleDateFormat(
            "MMM dd, HH:mm",
            java.util.Locale.getDefault()
        ).format(java.util.Date(comment.timestamp))
    }

    override fun getItemCount(): Int = comments.size
}

class Comment : AppCompatActivity() {
    private lateinit var commentRecyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var commentEditText: EditText
    private lateinit var sendCommentButton: Button
    private lateinit var likeButton: ImageButton
    private lateinit var likeCountText: TextView
    private lateinit var sessionManager: SessionManager
    private val comments = mutableListOf<CommentData>()
    private var postId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)
        
        postId = intent.getIntExtra("postId", 0)

        if (postId == 0) {
            Toast.makeText(this, "Invalid post", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        commentRecyclerView = findViewById(R.id.commentRecyclerView)
        commentEditText = findViewById(R.id.commentEditText)
        sendCommentButton = findViewById(R.id.sendCommentButton)
        likeButton = findViewById(R.id.likeButton)
        likeCountText = findViewById(R.id.likeCountText)

        // Setup RecyclerView
        commentAdapter = CommentAdapter(comments)
        commentRecyclerView.layoutManager = LinearLayoutManager(this)
        commentRecyclerView.adapter = commentAdapter

        // Set click listeners
        sendCommentButton.setOnClickListener { sendComment() }
        likeButton.setOnClickListener { toggleLike() }

        val backBtn = findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener {
            finish()
        }
    }

    private fun sendComment() {
        val text = commentEditText.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CommentRequest(
            post_id = postId,
            user_id = sessionManager.getUserId(),
            comment_text = text
        )

        RetrofitClient.apiService.addComment(request).enqueue(object : Callback<CommentResponse> {
            override fun onResponse(call: Call<CommentResponse>, response: Response<CommentResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    commentEditText.text.clear()
                    Toast.makeText(this@Comment, "Comment added successfully!", Toast.LENGTH_SHORT).show()
                    
                    // Add comment to list locally
                    val newComment = CommentData(
                        id = response.body()?.comment_id.toString(),
                        postId = postId.toString(),
                        userId = sessionManager.getUserId().toString(),
                        username = sessionManager.getUsername(),
                        text = text,
                        timestamp = System.currentTimeMillis()
                    )
                    comments.add(newComment)
                    commentAdapter.notifyItemInserted(comments.size - 1)
                } else {
                    Toast.makeText(this@Comment, "Failed to add comment", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CommentResponse>, t: Throwable) {
                Toast.makeText(this@Comment, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun toggleLike() {
        val request = LikeRequest(
            post_id = postId,
            user_id = sessionManager.getUserId()
        )

        RetrofitClient.apiService.toggleLike(request).enqueue(object : Callback<LikeResponse> {
            override fun onResponse(call: Call<LikeResponse>, response: Response<LikeResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val likeResponse = response.body()!!
                    likeCountText.text = "${likeResponse.likes_count} likes"
                    
                    if (likeResponse.action == "liked") {
                        likeButton.setImageResource(R.drawable.ic_heart_filled)
                        Toast.makeText(this@Comment, "Liked!", Toast.LENGTH_SHORT).show()
                    } else {
                        likeButton.setImageResource(R.drawable.ic_heart_outline)
                        Toast.makeText(this@Comment, "Unliked", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<LikeResponse>, t: Throwable) {
                Toast.makeText(this@Comment, "Failed to toggle like", Toast.LENGTH_SHORT).show()
            }
        })
    }
}