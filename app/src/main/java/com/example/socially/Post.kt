package com.example.socially

/**
 * Post data class for social media posts
 * Compatible with both Firebase (legacy) and REST API
 */
data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val location: String = "",
    val postImageUrl: String = "",
    val avatarUrl: String = "",
    var likes: Int = 0,
    val caption: String = "",
    val timestamp: Long = 0L,
    var likedBy: MutableMap<String, Boolean> = mutableMapOf(),
    var comments: Int = 0,
    val image: String = "" // Firebase compatibility field
) {
    // These fields are NOT saved to database - only for local UI state
    @Transient
    var isLiked: Boolean = false

    @Transient
    var isSaved: Boolean = false

    @Transient
    var likesCount: Int = likes
}