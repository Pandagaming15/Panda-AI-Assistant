package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String, // "user", "model", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

@Entity(tableName = "memory_documents")
data class MemoryDocumentEntity(
    @PrimaryKey
    val id: String,
    val filename: String,
    val filePath: String,
    val category: String, // "SDK Header", "Source Code", "Build Script", "Custom"
    val content: String,
    val summary: String, // Extracted symbols/classes/functions for quick search
    val sizeBytes: Long,
    val importedAt: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true
)
