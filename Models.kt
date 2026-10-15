package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val preferredCreativeStyle: String = "poetic",
    val favoriteDomains: List<String> = listOf("Storytelling", "Sci-Fi", "Music"),
    val enableHighThinking: Boolean = false,
    val enableLowLatency: Boolean = false,
    val preferredVoice: String = "Kore",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommunityPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "general",
    val mediaUrl: String? = null,
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommunityComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Session(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "general",
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Message(
    @DocumentId val id: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val role: String = "user", // "user" or "model"
    val text: String = "",
    val multimodalType: String = "none", // "none", "image", "audio", "video", "science"
    val mediaUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Spark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "prompt", // story, music, character, art, branding, science, prompt, general
    val tags: List<String> = emptyList(),
    @get:PropertyName("isFavorite") @set:PropertyName("isFavorite") var isFavorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomVoice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val baseVoice: String = "Kore", // Kore, Aoede, Fenrir, Puck, System
    val gender: String = "Female", // Female, Male, Robotic, Ethereal, Neutral
    val language: String = "English", // English, Urdu (اردو), Multilingual
    val style: String = "Warm & Expressive", // Poetic & Ghazal, Storyteller, Dramatic, Calm, Energetic, News
    val pitch: Float = 1.0f, // 0.5f to 2.0f
    val speed: Float = 1.0f, // 0.5f to 2.0f
    val isCloned: Boolean = false,
    val sampleAudioPath: String? = null,
    val voiceBlueprint: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
