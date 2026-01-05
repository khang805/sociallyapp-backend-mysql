package com.example.socially

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Follow Requests Activity - Shows pending follow requests
 */
class FollowRequestsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private val followRequests = mutableListOf<FollowRequestData>()
    private lateinit var adapter: FollowRequestsAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_follow_requests)

        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)

        // Initialize UI components
        recyclerView = findViewById(R.id.followRequestsRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateFollowRequests)

        // Setup RecyclerView
        setupRecyclerView()

        // Load follow requests
        loadFollowRequests()

        // Back button
        findViewById<ImageView>(R.id.back_arrow)?.setOnClickListener {
            finish()
        }

        // Navigation
        setupNavigation()
    }

    private fun setupRecyclerView() {
        adapter = FollowRequestsAdapter(
            followRequestsList = followRequests,
            onAcceptClicked = { position -> acceptFollowRequest(position) },
            onRejectClicked = { position -> rejectFollowRequest(position) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadFollowRequests() {
        // Get current user's notifications which should include follow requests
        RetrofitClient.apiService.getNotifications(sessionManager.getUserId())
            .enqueue(object : Callback<NotificationsResponse> {
                override fun onResponse(
                    call: Call<NotificationsResponse>,
                    response: Response<NotificationsResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val notifications = response.body()?.notifications ?: emptyList()

                        // Filter for follow requests from the notifications
                        followRequests.clear()
                        notifications.forEach { notification ->
                            if (notification.type == "follow_request" && notification.reference_id != null) {
                                // Parse the sender info from notification
                                followRequests.add(
                                    FollowRequestData(
                                        requestId = notification.reference_id, // Use reference_id which is the actual follow_request ID
                                        userId = 0, // Will be extracted from the API response if needed
                                        username = notification.sender_username ?: "Unknown",
                                        profilePicture = notification.sender_profile_picture,
                                        message = notification.body
                                    )
                                )
                            }
                        }

                        adapter.notifyDataSetChanged()
                        updateEmptyState()
                    } else {
                        Toast.makeText(this@FollowRequestsActivity, "Failed to load follow requests", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<NotificationsResponse>, t: Throwable) {
                    Toast.makeText(this@FollowRequestsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun acceptFollowRequest(position: Int) {
        if (position < 0 || position >= followRequests.size) return

        val followRequest = followRequests[position]
        val respondRequest = RespondFollowRequest(
            request_id = followRequest.requestId,
            action = "accept"
        )

        RetrofitClient.apiService.respondFollowRequest(respondRequest)
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(
                    call: Call<GenericResponse>,
                    response: Response<GenericResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(
                            this@FollowRequestsActivity,
                            "Follow request accepted",
                            Toast.LENGTH_SHORT
                        ).show()
                        followRequests.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        updateEmptyState()
                    } else {
                        Toast.makeText(
                            this@FollowRequestsActivity,
                            "Failed to accept request",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(
                        this@FollowRequestsActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun rejectFollowRequest(position: Int) {
        if (position < 0 || position >= followRequests.size) return

        val followRequest = followRequests[position]
        val respondRequest = RespondFollowRequest(
            request_id = followRequest.requestId,
            action = "reject"
        )

        RetrofitClient.apiService.respondFollowRequest(respondRequest)
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(
                    call: Call<GenericResponse>,
                    response: Response<GenericResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(
                            this@FollowRequestsActivity,
                            "Follow request rejected",
                            Toast.LENGTH_SHORT
                        ).show()
                        followRequests.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        updateEmptyState()
                    } else {
                        Toast.makeText(
                            this@FollowRequestsActivity,
                            "Failed to reject request",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(
                        this@FollowRequestsActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateEmptyState() {
        if (followRequests.isEmpty()) {
            recyclerView.visibility = android.view.View.GONE
            emptyStateLayout.visibility = android.view.View.VISIBLE
        } else {
            recyclerView.visibility = android.view.View.VISIBLE
            emptyStateLayout.visibility = android.view.View.GONE
        }
    }

    private fun setupNavigation() {
        findViewById<ImageView>(R.id.nav_home)?.setOnClickListener {
            startActivity(Intent(this, Main_feed::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }

        findViewById<ImageView>(R.id.nav_search)?.setOnClickListener {
            startActivity(Intent(this, Explore_page::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }

        findViewById<ImageView>(R.id.nav_heart)?.setOnClickListener {
            // Already on follow requests activity
        }

        findViewById<ImageView>(R.id.nav_profile)?.setOnClickListener {
            startActivity(Intent(this, Profile::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }
    }

    override fun onResume() {
        super.onResume()
        loadFollowRequests()
    }
}

// Data class for follow request
data class FollowRequestData(
    val requestId: Int,
    val userId: Int,
    val username: String,
    val profilePicture: String?,
    val message: String
)

