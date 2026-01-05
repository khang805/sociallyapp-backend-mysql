package com.example.socially.repository

import android.content.Context
import android.util.Log
import com.example.socially.api.RetrofitClient
import com.example.socially.api.UploadPostRequest
import com.example.socially.db.AppDatabase
import com.example.socially.db.OfflineQueueEntity
import com.example.socially.db.PostEntity
import com.example.socially.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PostRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val postDao = db.postDao()
    private val offlineDao = db.offlineQueueDao()
    private val api = RetrofitClient.apiService

    suspend fun refreshPostsFromServer(userId: Int) = withContext(Dispatchers.IO) {
        try {
            val response = api.getPosts(userId).execute()
            if (response.isSuccessful && response.body()?.status == "success") {
                val posts = response.body()?.posts ?: emptyList()
                Log.d("PostRepo", "Fetched ${posts.size} posts from server")
                
                val entities = posts.map { p ->
                    Log.d("PostRepo", "Post ${p.id}: imageUrl=${p.image_url}, username=${p.username}")
                    PostEntity(
                        id = p.id,
                        userId = p.user_id,
                        username = p.username ?: "",
                        profilePicture = p.profile_picture,
                        caption = p.caption,
                        imageUrl = p.image_url ?: "",
                        likesCount = p.likes_count ?: 0,
                        commentsCount = p.comments_count ?: 0,
                        isLiked = (p.is_liked ?: 0) == 1,  // Convert Int (0/1) to Boolean
                        createdAt = p.created_at ?: currentTimestampString(),
                        isSynced = true
                    )
                }
                postDao.deleteAll() // Clear old posts before inserting new ones
                postDao.insertAll(entities)
                Log.d("PostRepo", "Saved ${entities.size} posts to local database")
            } else {
                Log.e("PostRepo", "Failed to fetch posts: ${response.code()} - ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("PostRepo", "Error fetching posts: ${e.message}", e)
        }
    }

    // Upload post with offline queuing
    suspend fun uploadPost(userId: Int, caption: String?, imageBase64: String) = withContext(Dispatchers.IO) {
        val timestamp = currentTimestampString()

        if (!NetworkUtils.isOnline(context)) {
            val actionData = "{\"user_id\":$userId,\"caption\":\"${caption ?: ""}\",\"image_base64\":\"$imageBase64\"}"
            val action = OfflineQueueEntity(
                userId = userId,
                actionType = "post",
                actionData = actionData,
                timestamp = System.currentTimeMillis()
            )
            offlineDao.insert(action)
            return@withContext
        }

        try {
            val request = UploadPostRequest(
                user_id = userId,
                caption = caption,
                image_base64 = imageBase64
            )
            val response = api.uploadPost(request).execute()
            if (response.isSuccessful && response.body()?.status == "success") {
                val postId = response.body()?.post_id ?: 0
                val entity = PostEntity(
                    id = postId,
                    userId = userId,
                    username = "",
                    profilePicture = null,
                    caption = caption,
                    imageUrl = response.body()?.image_url ?: "",
                    likesCount = 0,
                    commentsCount = 0,
                    isLiked = false,
                    createdAt = timestamp,
                    isSynced = true
                )
                postDao.insert(entity)
            } else {
                Log.e("PostRepo", "Upload failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("PostRepo", "Upload exception: ${e.message}")
        }
    }

    suspend fun getCachedPosts(): List<PostEntity> = withContext(Dispatchers.IO) {
        postDao.getAllPosts()
    }

    private fun currentTimestampString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
