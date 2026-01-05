package com.example.socially

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socially.api.FollowersResponse
import com.example.socially.api.UserData
import com.example.socially.auth.SessionManager
import com.example.socially.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

/**
 * DM Page - Direct Messages
 * Shows list of conversations
 */
class dm_page : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var headerTitle: TextView
    private lateinit var backButton: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView

    private var allUsers: List<UserData> = emptyList()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dm_page)

        sessionManager = SessionManager(this)

        friendsRecyclerView = findViewById(R.id.friendsRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        headerTitle = findViewById(R.id.headerTitle)
        backButton = findViewById(R.id.backButton)
        searchEditText = findViewById(R.id.searchEditText_for_dm)
        searchIcon = findViewById(R.id.searchIcon_for_dm)

        headerTitle.text = sessionManager.getUsername()

        backButton.setOnClickListener { finish() }

        friendsRecyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = ConversationAdapter { user ->
            // Open chat activity with selected user
            val intent = Intent(this@dm_page, chat::class.java)
            intent.putExtra("other_user_id", user.id)
            intent.putExtra("other_username", user.username)
            startActivity(intent)
        }
        friendsRecyclerView.adapter = adapter

        // Load user's followers / following list as potential DM contacts
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                RetrofitClient.init(sessionManager)
                val call = RetrofitClient.apiService.getFollowers(sessionManager.getUserId())
                val resp = call.awaitResponse()
                if (resp.isSuccessful) {
                    val body: FollowersResponse? = resp.body()
                    allUsers = body?.followers ?: emptyList()

                    withContext(Dispatchers.Main) {
                        updateList(adapter, allUsers)
                    }
                } else {
                    withContext(Dispatchers.Main) { showEmpty() }
                }
            } catch (ignored: Exception) {
                withContext(Dispatchers.Main) { showEmpty() }
            }
        }

        // Search/filter logic: debounce simple implementation
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                val q = s?.toString()?.trim() ?: ""
                searchJob = lifecycleScope.launch {
                    delay(250) // debounce
                    val filtered = if (q.isEmpty()) allUsers else allUsers.filter { it.username.contains(q, true) }
                    updateList(adapter, filtered)
                }
            }
        })

        searchIcon.setOnClickListener { searchEditText.requestFocus() }
    }

    private fun updateList(adapter: ConversationAdapter, list: List<UserData>) {
        if (list.isEmpty()) {
            showEmpty()
        } else {
            emptyStateLayout.visibility = View.GONE
            friendsRecyclerView.visibility = View.VISIBLE
            adapter.submitList(list)
        }
    }

    private fun showEmpty() {
        emptyStateLayout.visibility = View.VISIBLE
        friendsRecyclerView.visibility = View.GONE
    }
}
