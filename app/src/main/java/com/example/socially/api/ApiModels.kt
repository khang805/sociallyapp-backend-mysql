package com.example.socially.api

// Authentication Models
data class SignupRequest(
    val username: String,
    val email: String,
    val password: String,
    val fcm_token: String = ""
)

data class LoginRequest(
    val email: String,
    val password: String,
    val fcm_token: String = ""
)

data class AuthResponse(
    val status: String,
    val message: String,
    val user_id: Int?,
    val username: String?,
    val email: String?,
    val profile_picture: String?,
    val token: String?, // Changed from auth_token for consistency
    val is_first_time: Boolean? = null  // login.php returns PHP boolean (true/false)
)

// Post Models
data class UploadPostRequest(
    val user_id: Int,
    val caption: String?,
    val image_base64: String
)

data class UploadPostResponse(
    val status: String,
    val message: String,
    val post_id: Int?,
    val image_url: String?
)

data class PostsResponse(
    val status: String,
    val posts: List<PostData>
)

data class PostData(
    val id: Int,
    val user_id: Int,
    val username: String,
    val profile_picture: String?,
    val caption: String?,
    val image_url: String,
    val likes_count: Int,
    val comments_count: Int,
    val is_liked: Int = 0,  // Backend returns 0/1 as NUMBER
    val created_at: String
)

data class LikeRequest(
    val post_id: Int,
    val user_id: Int
)

data class LikeResponse(
    val status: String,
    val action: String,
    val likes_count: Int
)

data class CommentRequest(
    val post_id: Int,
    val user_id: Int,
    val comment_text: String
)

data class CommentResponse(
    val status: String,
    val message: String,
    val comment_id: Int?
)

// Story Models
data class UploadStoryRequest(
    val user_id: Int,
    val media_base64: String,
    val media_type: String = "image"
)

data class UploadStoryResponse(
    val status: String,
    val message: String,
    val story_id: Int?,
    val media_url: String?,
    val expires_at: String?
)

data class StoriesResponse(
    val status: String,
    val stories: List<StoryGroup>
)

data class StoryGroup(
    val user_id: Int,
    val username: String,
    val profile_picture: String?,
    val stories: List<StoryData>
)

data class StoryData(
    val id: Int,
    val media_url: String,
    val media_type: String,
    val created_at: String,
    val expires_at: String
)

// Messaging Models
data class MessageRequest(
    val sender_id: Int,
    val receiver_id: Int,
    val message_text: String?,
    val media_base64: String? = null,
    val media_type: String = "text",
    val is_vanish_mode: Boolean = false
)

data class SendMessageResponse(
    val status: String,
    val message_id: Int?,
    val media_url: String?
)

data class MessagesResponse(
    val status: String,
    val messages: List<MessageData>
)

data class MessageData(
    val id: Int,
    val sender_id: Int,
    val receiver_id: Int,
    val message_text: String?,
    val media_url: String?,
    val media_type: String,
    val is_vanish_mode: Int = 0,  // Backend returns 0/1 as NUMBER
    val is_seen: Int = 0,          // Backend returns 0/1 as NUMBER
    val is_edited: Int = 0,        // Backend returns 0/1 as NUMBER
    val created_at: String = ""
)

data class EditMessageRequest(
    val message_id: Int,
    val new_text: String
)

data class DeleteMessageRequest(
    val message_id: Int
)

// Follow System Models
data class FollowRequest(
    val sender_id: Int,
    val receiver_id: Int
)

data class FollowResponse(
    val status: String,
    val message: String,
    val request_id: Int?
)

data class RespondFollowRequest(
    val request_id: Int,
    val action: String // "accept" or "reject"
)

data class FollowersResponse(
    val status: String,
    val followers: List<UserData>,
    val following: List<UserData>,
    val followers_count: Int,
    val following_count: Int
)

data class UserData(
    val id: Int,
    val username: String,
    val profile_picture: String?,
    val bio: String?,
    val is_online: Int = 0  // Backend returns 0 or 1 as NUMBER, not BOOLEAN
)

// Search Models
data class SearchResponse(
    val status: String,
    val users: List<UserData>,
    val count: Int
)

// Status Models
data class StatusRequest(
    val user_id: Int,
    val is_online: Boolean
)

data class UpdateProfileRequest(
    val user_id: Int,
    val profile_picture_base64: String? = null,
    val cover_photo_base64: String? = null,
    val bio: String? = null
)

data class UpdateProfileResponse(
    val status: String,
    val message: String,
    val profile_picture: String?,
    val cover_photo: String?
)

// Security Models
data class ScreenshotRequest(
    val chat_user_id: Int,
    val screenshot_taker_id: Int
)

data class ScreenshotResponse(
    val status: String,
    val message: String,
    val alert_id: Int?,
    val fcm_token: String?
)

// Notification Models
data class NotificationsResponse(
    val status: String,
    val notifications: List<NotificationData>
)

data class NotificationData(
    val id: Int,
    val type: String,
    val title: String,
    val body: String,
    val sender_username: String?,
    val sender_profile_picture: String?,
    val reference_id: Int? = null,  // For follow requests, this is the follow_request ID
    val is_read: Int = 0,  // Backend returns 0/1 as NUMBER, convert with: is_read == 1
    val created_at: String = ""
)

// Offline Support Models
data class QueueActionRequest(
    val user_id: Int,
    val action_type: String,
    val action_data: Map<String, Any>
)

data class QueueResponse(
    val status: String,
    val message: String,
    val queue_id: Int?
)

data class SyncRequest(
    val user_id: Int
)

data class SyncResponse(
    val status: String,
    val message: String,
    val processed_count: Int,
    val results: List<SyncResult>
)

data class SyncResult(
    val queue_id: Int,
    val status: String,
    val error: String?
)

//==============================================================================
// NEW: Polling API Models (Replacing Firebase Realtime Database)
//==============================================================================

/**
 * Response for polling new messages
 * Replaces Firebase Realtime Database message listener
 */
data class PollMessagesResponse(
    val status: String,
    val messages: List<MessageData>,
    val count: Int
)

/**
 * Response for user online/offline status
 * Replaces Firebase Presence system
 */
data class UserStatusResponse(
    val status: String,
    val user: UserStatus
)

data class UserStatus(
    val id: Int,
    val username: String,
    val profile_picture: String?,
    val is_online: Boolean = false,  // get_user_status.php casts to (bool), so it returns Boolean
    val last_seen: String = "",
    val status_text: String = ""
)

/**
 * Response for screenshot alerts
 * Replaces Firebase Realtime Database listener
 */
data class ScreenshotAlertsResponse(
    val status: String,
    val alerts: List<ScreenshotAlert>,
    val count: Int
)

data class ScreenshotAlert(
    val id: Int,
    val screenshot_taker_id: Int,
    val taker_username: String,
    val taker_profile_picture: String?,
    val created_at: String,
    val message: String
)

/**
 * Response for complete user profile
 * Replaces Firebase Database user data retrieval
 */
data class UserProfileResponse(
    val status: String,
    val user: UserProfile
)

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String,
    val profile_picture: String?,
    val cover_photo: String?,
    val bio: String?,
    val is_online: Boolean = false,  // get_user_profile.php casts to (bool), so it returns Boolean
    val last_seen: String = "",
    val created_at: String = "",
    val followers_count: Int = 0,
    val following_count: Int = 0,
    val posts_count: Int = 0
)

// Generic Response
data class GenericResponse(
    val status: String,
    val message: String
)