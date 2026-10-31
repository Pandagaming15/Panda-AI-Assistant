package com.example.data.api

import kotlinx.coroutines.flow.Flow

data class MessagePayload(
    val role: String, // "user", "model", or "assistant"
    val content: String
)

interface AiService {
    fun streamResponse(
        messages: List<MessagePayload>,
        systemInstruction: String,
        temperature: Float
    ): Flow<String>

    suspend fun generateResponse(
        messages: List<MessagePayload>,
        systemInstruction: String,
        temperature: Float
    ): String
}
