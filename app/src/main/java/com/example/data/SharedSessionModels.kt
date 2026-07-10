package com.example.data

data class SharedRoom(
    val id: String = "",
    val name: String = "",
    val deckId: String = "",
    val deckName: String = "",
    val hostUserId: String = "",
    val hostUserName: String = "",
    val subject: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val activeParticipantCount: Int = 1
)

data class SharedRoomParticipant(
    val userId: String,
    val name: String,
    val avatar: String, // avatar id/name
    val twinAvatar: String, // "socratic", "scholar", "tech", "creative"
    val twinName: String,
    val cardsReviewed: Int = 0,
    val xpEarned: Int = 0,
    val accuracy: Int = 100 // accuracy rate in %
)

data class SharedRoomMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderName: String,
    val senderAvatar: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSystemAction: Boolean = false,
    val isTwinMessage: Boolean = false
)
