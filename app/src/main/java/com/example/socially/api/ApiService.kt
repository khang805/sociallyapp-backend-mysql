package com.example.socially.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part


interface ApiService {

    // Authentication Endpoints
    @POST("signup.php")
    fun signupUser(@Body request: SignupRequest): Call<AuthResponse>

    @POST("login.php")
    fun loginUser(@Body request: LoginRequest): Call<AuthResponse>

    // Post Endpoints
    @POST("upload_post.php")
    fun uploadPost(@Body request: UploadPostRequest): Call<UploadPostResponse>

    // Multipart upload for posts (file upload via form-data)
    @Multipart
    @POST("upload_post.php")
    fun uploadPostMultipart(
        @Part("user_id") userId: RequestBody,
        @Part("caption") caption: RequestBody,
        @Part media: MultipartBody.Part
    ): Call<UploadPostResponse>

    @GET("get_posts.php")
    fun getPosts(@Query("user_id") userId: Int): Call<PostsResponse>

    @POST("toggle_like.php")
    fun toggleLike(@Body request: LikeRequest): Call<LikeResponse>

    @POST("add_comment.php")
    fun addComment(@Body request: CommentRequest): Call<CommentResponse>

    // Story Endpoints
    @POST("upload_story.php")
    fun uploadStory(@Body request: UploadStoryRequest): Call<UploadStoryResponse>

    @GET("get_stories.php")
    fun getStories(@Query("user_id") userId: Int): Call<StoriesResponse>

    // Messaging Endpoints
    @POST("send_message.php")
    fun sendMessage(@Body request: MessageRequest): Call<SendMessageResponse>

    // Multipart message upload (if a file needs to be sent as form-data)
    @Multipart
    @POST("send_message.php")
    fun sendMessageMultipart(
        @Part("sender_id") senderId: RequestBody,
        @Part("receiver_id") receiverId: RequestBody,
        @Part("message_text") messageText: RequestBody,
        @Part("is_vanish_mode") isVanish: RequestBody,
        @Part media: MultipartBody.Part
    ): Call<SendMessageResponse>

    @GET("get_messages.php")
    fun getMessages(
        @Query("user1_id") user1Id: Int,
        @Query("user2_id") user2Id: Int
    ): Call<MessagesResponse>

    @POST("edit_message.php")
    fun editMessage(@Body request: EditMessageRequest): Call<GenericResponse>

    @POST("delete_message.php")
    fun deleteMessage(@Body request: DeleteMessageRequest): Call<GenericResponse>

    // Follow System Endpoints
    @POST("send_follow_request.php")
    fun sendFollowRequest(@Body request: FollowRequest): Call<FollowResponse>

    @POST("respond_follow_request.php")
    fun respondFollowRequest(@Body request: RespondFollowRequest): Call<GenericResponse>

    @GET("get_followers.php")
    fun getFollowers(@Query("user_id") userId: Int): Call<FollowersResponse>

    // Search Endpoint
    @GET("search_users.php")
    fun searchUsers(
        @Query("query") query: String,
        @Query("user_id") userId: Int? = null,
        @Query("filter") filter: String = "all"
    ): Call<SearchResponse>

    // User Status Endpoints
    @POST("update_status.php")
    fun updateStatus(@Body request: StatusRequest): Call<GenericResponse>

    @POST("update_profile.php")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<UpdateProfileResponse>

    // Security Endpoints
    @POST("log_screenshot.php")
    fun logScreenshot(@Body request: ScreenshotRequest): Call<ScreenshotResponse>

    @GET("get_notifications.php")
    fun getNotifications(@Query("user_id") userId: Int): Call<NotificationsResponse>

    // Offline Support Endpoints
    @POST("queue_offline_action.php")
    fun queueOfflineAction(@Body request: QueueActionRequest): Call<QueueResponse>

    @POST("sync_offline_queue.php")
    fun syncOfflineQueue(@Body request: SyncRequest): Call<SyncResponse>
    
    // NEW: Polling Endpoints (Replacing Firebase Realtime Database)
    
    /**
     * Poll for new messages since last message ID
     * Replaces Firebase Realtime Database listener
     */
    @GET("poll_new_messages.php")
    fun pollNewMessages(
        @Query("user1_id") user1Id: Int,
        @Query("user2_id") user2Id: Int,
        @Query("last_message_id") lastMessageId: Int = 0
    ): Call<PollMessagesResponse>
    
    /**
     * Get user online/offline status
     * Replaces Firebase Presence system
     */
    @GET("get_user_status.php")
    fun getUserStatus(@Query("user_id") userId: Int): Call<UserStatusResponse>
    
    /**
     * Get screenshot alerts for user
     * Replaces Firebase Realtime Database listener
     */
    @GET("get_screenshot_alerts.php")
    fun getScreenshotAlerts(
        @Query("user_id") userId: Int,
        @Query("last_notification_id") lastNotificationId: Int = 0
    ): Call<ScreenshotAlertsResponse>
    
    /**
     * Get complete user profile data
     * Replaces Firebase Database user data retrieval
     */
    @GET("get_user_profile.php")
    fun getUserProfile(@Query("user_id") userId: Int): Call<UserProfileResponse>
}