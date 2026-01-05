package com.example.socially.db

import androidx.room.*

/**
 * DAO for Offline Queue operations
 */
@Dao
interface OfflineQueueDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: OfflineQueueEntity): Long
    
    @Query("SELECT * FROM offline_queue WHERE status = 'pending' ORDER BY timestamp ASC")
    suspend fun getPendingActions(): List<OfflineQueueEntity>
    
    @Query("UPDATE offline_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)
    
    @Query("UPDATE offline_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Int)
    
    @Query("DELETE FROM offline_queue WHERE id = :id")
    suspend fun delete(id: Int)
    
    @Query("DELETE FROM offline_queue WHERE status = 'completed'")
    suspend fun deleteCompleted()
}

/**
 * DAO for Posts
 */
@Dao
interface PostDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)
    
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    suspend fun getAllPosts(): List<PostEntity>
    
    @Query("DELETE FROM posts")
    suspend fun deleteAll()
}

/**
 * DAO for Stories
 */
@Dao
interface StoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stories: List<StoryEntity>)
    
    @Query("SELECT * FROM stories WHERE expiresAt > :currentTime ORDER BY createdAt DESC")
    suspend fun getActiveStories(currentTime: String): List<StoryEntity>
    
    @Query("DELETE FROM stories WHERE expiresAt <= :currentTime")
    suspend fun deleteExpiredStories(currentTime: String)
    
    @Query("DELETE FROM stories")
    suspend fun deleteAll()
}
