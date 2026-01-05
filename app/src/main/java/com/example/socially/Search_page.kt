package com.example.socially

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class User(
    val userId: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val image: String = ""
)

// Helper to safely get string resources with a fallback to avoid runtime ResourceNotFound exceptions
private fun safeGetString(ctx: Context, resId: Int, fallback: String): String {
    return try {
        ctx.getString(resId)
    } catch (e: Resources.NotFoundException) {
        fallback
    }
}

class UserAdapter(
    private val users: MutableList<User>,
    private val currentUserId: Int,
    private val onUserClick: (User) -> Unit
) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    // Keep track of which user ids we already follow and which we've requested during this session
    private var followingIds: Set<Int> = emptySet()
    private val requestedIds: MutableSet<Int> = mutableSetOf()

    fun updateFollowingIds(ids: Set<Int>) {
        followingIds = ids
        notifyDataSetChanged()
    }

    fun markRequested(userId: Int) {
        requestedIds.add(userId)
        notifyDataSetChanged()
    }

    class UserViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val usernameText: TextView = itemView.findViewById(R.id.userUsername)
        val nameText: TextView = itemView.findViewById(R.id.userFullName)
        val profileImage: ImageView = itemView.findViewById(R.id.userProfileImage)
        val sendRequestButton: Button = itemView.findViewById(R.id.sendRequestButton)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): UserViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.user_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.usernameText.text = user.username
        holder.nameText.text = "${user.firstName} ${user.lastName}"

        // Load profile image if available (base64) otherwise default
        if (user.image.isNotEmpty()) {
            try {
                val imageBytes = android.util.Base64.decode(user.image, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (ignored: Exception) {
                holder.profileImage.setImageResource(R.drawable.profile)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.profile)
        }

        // Click to view profile
        holder.itemView.setOnClickListener { onUserClick(user) }

        // Determine button state
        val receiverId = user.userId.toIntOrNull() ?: -1
        val ctx = holder.itemView.context

        fun setButtonTextAndState(text: String, enabled: Boolean) {
            holder.itemView.post {
                holder.sendRequestButton.text = text
                holder.sendRequestButton.isEnabled = enabled
            }
        }

        when {
            receiverId == -1 -> setButtonTextAndState(safeGetString(ctx, R.string.send_request, "Send Request"), false)
            receiverId == currentUserId -> setButtonTextAndState(safeGetString(ctx, R.string.you_text, "You"), false)
            followingIds.contains(receiverId) -> setButtonTextAndState(safeGetString(ctx, R.string.following_text, "Following"), false)
            requestedIds.contains(receiverId) -> setButtonTextAndState(safeGetString(ctx, R.string.requested_text, "Requested"), false)
            else -> setButtonTextAndState(safeGetString(ctx, R.string.send_request, "Send Request"), true)
        }

        // Send follow request button
        holder.sendRequestButton.setOnClickListener {
            // If already disabled by another check, ignore
            if (!holder.sendRequestButton.isEnabled) return@setOnClickListener

            setButtonTextAndState(safeGetString(ctx, R.string.sending_text, "Sending..."), false)

            val senderId = currentUserId
            if (senderId == -1 || receiverId == -1) {
                holder.itemView.post { Toast.makeText(ctx, "Unable to send request", Toast.LENGTH_SHORT).show() }
                setButtonTextAndState(safeGetString(ctx, R.string.send_request, "Send Request"), true)
                return@setOnClickListener
            }

            val req = FollowRequest(sender_id = senderId, receiver_id = receiverId)
            try {
                RetrofitClient.apiService.sendFollowRequest(req).enqueue(object : Callback<FollowResponse> {
                    override fun onResponse(call: Call<FollowResponse>, response: Response<FollowResponse>) {
                        holder.itemView.post {
                            try {
                                if (response.isSuccessful && response.body()?.status == "success") {
                                    Toast.makeText(ctx, "Request sent", Toast.LENGTH_SHORT).show()
                                    requestedIds.add(receiverId)
                                    holder.sendRequestButton.text = safeGetString(ctx, R.string.requested_text, "Requested")
                                    holder.sendRequestButton.isEnabled = false
                                } else {
                                    Toast.makeText(ctx, "Failed to send request", Toast.LENGTH_SHORT).show()
                                    holder.sendRequestButton.text = safeGetString(ctx, R.string.send_request, "Send Request")
                                    holder.sendRequestButton.isEnabled = true
                                }
                            } catch (e: Exception) {
                                Log.e("UserAdapter", "Error updating UI after sendFollowRequest: ${e.message}", e)
                            }
                        }
                    }

                    override fun onFailure(call: Call<FollowResponse>, t: Throwable) {
                        holder.itemView.post {
                            Toast.makeText(ctx, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                            holder.sendRequestButton.text = safeGetString(ctx, R.string.send_request, "Send Request")
                            holder.sendRequestButton.isEnabled = true
                        }
                    }
                })
            } catch (e: Exception) {
                holder.itemView.post {
                    Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    holder.sendRequestButton.text = safeGetString(ctx, R.string.send_request, "Send Request")
                    holder.sendRequestButton.isEnabled = true
                }
            }
        }
    }

    override fun getItemCount(): Int = users.size
}

/**
 * Search Page - User search with filters
 * Uses REST API instead of Firebase
 */
class Search_page : AppCompatActivity() {
    private lateinit var searchEditText: EditText
    private lateinit var clearButton: ImageView
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var sessionManager: SessionManager
    private val users = mutableListOf<User>()

    // Keep the following ids that current user already follows
    private var followingIds: Set<Int> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.search_page)

        try {
            sessionManager = SessionManager(this)
            RetrofitClient.init(sessionManager)

            // Initialize views
            searchEditText = findViewById(R.id.searchEditText)
            clearButton = findViewById(R.id.clearButton)
            usersRecyclerView = findViewById(R.id.usersRecyclerView)

            // Setup RecyclerView
            val currentUserId = sessionManager.getUserId()
            userAdapter = UserAdapter(users, currentUserId) { user ->
                try {
                    val intent = Intent(this, Other_Person_Profile_follow::class.java)
                    intent.putExtra("userId", user.userId)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("Search_page", "Failed to open profile: ${e.message}", e)
                    Toast.makeText(this, "Could not open profile", Toast.LENGTH_SHORT).show()
                }
            }
            usersRecyclerView.layoutManager = LinearLayoutManager(this)
            usersRecyclerView.adapter = userAdapter

            // Fetch current following list so we can disable the follow button for already-followed users
            val current = sessionManager.getUserId()
            if (current > 0) {
                RetrofitClient.apiService.getFollowers(current).enqueue(object : Callback<FollowersResponse> {
                    override fun onResponse(call: Call<FollowersResponse>, response: Response<FollowersResponse>) {
                        try {
                            if (response.isSuccessful) {
                                val body = response.body()
                                val following = body?.following ?: emptyList()
                                followingIds = following.map { it.id }.toSet()
                                runOnUiThread { userAdapter.updateFollowingIds(followingIds) }
                            } else {
                                try {
                                    val err = response.errorBody()?.string()
                                    Log.w("Search_page", "getFollowers non-success: code=${response.code()}, body=$err")
                                } catch (e: Exception) {
                                    Log.w("Search_page", "getFollowers non-success: code=${response.code()}, error reading body: ${e.message}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Search_page", "Error parsing followers response: ${e.message}", e)
                        }
                    }

                    override fun onFailure(call: Call<FollowersResponse>, t: Throwable) {
                        Log.w("Search_page", "getFollowers failed: ${t.message}")
                    }
                })
            }

            // Set click listeners
            clearButton.setOnClickListener {
                try {
                    val intent = Intent(this, Explore_page::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Log.e("Search_page", "Navigation failed: ${e.message}", e)
                }
            }

            // Search on text change
            searchEditText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    try {
                        if ((s?.toString()?.length ?: 0) >= 2) {
                            searchUsers(s.toString())
                        } else {
                            users.clear()
                            userAdapter.notifyDataSetChanged()
                        }
                    } catch (e: Exception) {
                        Log.e("Search_page", "Text change handling failed: ${e.message}", e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("Search_page", "onCreate failed: ${e.message}", e)
            Toast.makeText(this, "Search initialization failed", Toast.LENGTH_SHORT).show()
            // Avoid crashing - close activity gracefully
            finish()
        }
    }

    private fun searchUsers(query: String) {
        if (query.isEmpty()) return

        try {
            val current = sessionManager.getUserId()
            RetrofitClient.apiService.searchUsers(
                query = query,
                userId = current,
                filter = "all"
            ).enqueue(object : Callback<SearchResponse> {
                override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                    try {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            val found = mutableListOf<User>()
                            response.body()?.users?.forEach { userData ->
                                found.add(User(
                                    userId = userData.id.toString(),
                                    username = userData.username,
                                    firstName = userData.username,
                                    lastName = "",
                                    image = userData.profile_picture ?: ""
                                ))
                            }
                            runOnUiThread {
                                users.clear()
                                users.addAll(found)
                                userAdapter.notifyDataSetChanged()
                            }
                        } else {
                            // Log detailed error body for debugging
                            try {
                                val errBody = response.errorBody()?.string()
                                Log.w("Search_page", "searchUsers non-success response: code=${response.code()}, body=$errBody")
                            } catch (e: Exception) {
                                Log.w("Search_page", "searchUsers non-success response: code=${response.code()}, could not read errorBody: ${e.message}")
                            }

                            runOnUiThread { Toast.makeText(this@Search_page, "No users found", Toast.LENGTH_SHORT).show() }
                        }
                    } catch (e: Exception) {
                        Log.e("Search_page", "Error parsing search response: ${e.message}", e)
                        runOnUiThread { Toast.makeText(this@Search_page, "Search parsing error", Toast.LENGTH_SHORT).show() }
                    }
                }

                override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                    Log.w("Search_page", "searchUsers failed: ${t.message}")
                    runOnUiThread { Toast.makeText(this@Search_page, "Search failed: ${t.message}", Toast.LENGTH_SHORT).show() }
                }
            })
        } catch (e: Exception) {
            Log.e("Search_page", "searchUsers call failed: ${e.message}", e)
            runOnUiThread { Toast.makeText(this@Search_page, "Search error: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }
}