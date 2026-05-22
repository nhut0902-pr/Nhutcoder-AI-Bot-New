package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "assistant"
    val content: String, // message text or the prompt used for image generation
    val isImage: Boolean = false,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
