package com.example.socially

@Suppress("UNUSED")
data class StoryModel(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val image: String = "",
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L
)

