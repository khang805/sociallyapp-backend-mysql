package com.example.socially.repository

import android.content.Context
import android.util.Log
import com.example.socially.api.RetrofitClient
import com.example.socially.api.UploadStoryRequest
import com.example.socially.db.AppDatabase
import com.example.socially.db.OfflineQueueEntity
import com.example.socially.db.StoryEntity
import com.example.socially.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class StoryRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val storyDao = db.storyDao()
    private val offlineDao = db.offlineQueueDao()
    private val api = RetrofitClient.apiService

    // Fetch stories from server and cache them locally
    suspend fun refreshStoriesFromServer(userId: Int) = withContext(Dispatchers.IO) {
        try {
            val response = api.getStories(userId).execute()
            if (response.isSuccessful && response.body()?.status == "success") {
                val groups = response.body()?.stories ?: emptyList()
                // Map API models (groups with nested stories) to StoryEntity and insert
                val entities = mutableListOf<StoryEntity>()
                for (group in groups) {
                    val uid = group.user_id
                    val uname = group.username
                    val pfp = group.profile_picture
                    for (s in group.stories) {
                        entities.add(
                            StoryEntity(
                                id = s.id,
                                userId = uid,
                                username = uname,
                                profilePicture = pfp,
                                mediaUrl = s.media_url,
                                mediaType = s.media_type,
                                createdAt = s.created_at,
                                expiresAt = s.expires_at,
                                isSynced = true
                            )
                        )
                    }
                }
                if (entities.isNotEmpty()) {
                    storyDao.insertAll(entities)
                }
                // Clean up expired stories locally
                val now = currentTimestampString()
                storyDao.deleteExpiredStories(now)
            } else {
                Log.e("StoryRepo", "Failed to fetch stories: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("StoryRepo", "Error fetching stories: ${e.message}")
        }
    }

    // Upload a story: if offline, add to offline queue
    suspend fun uploadStoryAsFile(userId: Int, mediaBase64: String, mediaType: String) = withContext(Dispatchers.IO) {
        val timestamp = currentTimestampString()
        if (!NetworkUtils.isOnline(context)) {
            // Queue offline action
            val actionData = "{\"user_id\":$userId,\"media_base64\":\"$mediaBase64\",\"media_type\":\"$mediaType\"}"
            val action = OfflineQueueEntity(
                userId = userId,
                actionType = "story",
                actionData = actionData,
                timestamp = System.currentTimeMillis()
            )
            offlineDao.insert(action)
            return@withContext
        }

        try {
            val request = UploadStoryRequest(
                user_id = userId,
                media_base64 = mediaBase64,
                media_type = mediaType
            )
            val response = api.uploadStory(request).execute()
            if (response.isSuccessful && response.body()?.status == "success") {
                // Insert into local DB for immediate viewing
                val storyId = response.body()?.story_id ?: 0
                val entity = StoryEntity(
                    id = storyId,
                    userId = userId,
                    username = "",
                    profilePicture = null,
                    mediaUrl = response.body()?.media_url ?: "",
                    mediaType = mediaType,
                    createdAt = timestamp,
                    expiresAt = response.body()?.expires_at ?: calculateExpiry(timestamp),
                    isSynced = true
                )
                storyDao.insertAll(listOf(entity))
            } else {
                Log.e("StoryRepo", "Upload failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("StoryRepo", "Upload exception: ${e.message}")
        }
    }

    // Get active stories from local DB
    suspend fun getActiveStories(): List<StoryEntity> = withContext(Dispatchers.IO) {
        val now = currentTimestampString()
        storyDao.getActiveStories(now)
    }

    private fun currentTimestampString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun calculateExpiry(createdAt: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(createdAt) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.HOUR_OF_DAY, 24)
            sdf.format(cal.time)
        } catch (e: Exception) {
            // fallback: now + 24h
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.add(Calendar.HOUR_OF_DAY, 24)
            sdf.format(cal.time)
        }
    }
}
