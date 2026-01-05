package com.example.socially.models

/**
 * Enhanced Message Entity with vanish mode, edit/delete support
 */
data class MessageUIModel(
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val text: String?,
    val mediaUrl: String?,
    val mediaType: String = "text",
    val timestamp: String,
    val isVanish: Boolean = false,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val editedAt: String? = null,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val isOwn: Boolean = false
)

/**
 * Message action types for menu
 */
enum class MessageAction {
    REPLY,
    EDIT,
    DELETE,
    COPY,
    SHARE,
    REACT
}

