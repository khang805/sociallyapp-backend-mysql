package com.example.socially.api

import com.example.socially.Post

/**
 * Extension functions to convert API models to UI models
 */

fun PostData.toPost(): Post {
    return Post(
        postId = this.id.toString(),
        userId = this.user_id.toString(),
        username = this.username,
        image = this.image_url,
        postImageUrl = this.image_url,
        avatarUrl = this.profile_picture ?: "",
        caption = this.caption ?: "",
        likes = this.likes_count,
        comments = this.comments_count,
        timestamp = 0L, // Will be parsed from created_at if needed
        likedBy = mutableMapOf()
    ).apply {
        isLiked = this@toPost.is_liked == 1  // Convert Int (0/1) to Boolean
        likesCount = this@toPost.likes_count
        isSaved = false
    }
}
