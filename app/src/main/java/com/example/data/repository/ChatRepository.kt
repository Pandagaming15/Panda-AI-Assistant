package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.AiService
import com.example.data.api.GeminiApiClient
import com.example.data.api.MessagePayload
import com.example.data.api.OpenAiApiClient
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.model.AiProviderType
import com.example.data.pref.AppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val context: Context,
    val preferences: AppPreferences = AppPreferences(context),
    val database: AppDatabase = AppDatabase.getInstance(context)
) {
    companion object {
        private const val TAG = "ChatRepository"

        @Volatile
        private var INSTANCE: ChatRepository? = null

        fun getInstance(context: Context): ChatRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ChatRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val conversationDao = database.conversationDao()
    private val messageDao = database.chatMessageDao()

    val memoryRepository = MemoryRepository.getInstance(context)

    // Application/Repository lifecycle scope that survives composable leaves and floating window minimizes
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val geminiClient = GeminiApiClient(
        apiKeyProvider = { preferences.geminiApiKey },
        modelProvider = { preferences.selectedModel }
    )

    private val openAiClient = OpenAiApiClient(
        endpointProvider = { preferences.openAiEndpoint },
        apiKeyProvider = { preferences.openAiApiKey },
        modelProvider = { preferences.openAiModel }
    )

    private val _activeConversationId = MutableStateFlow<String?>(preferences.activeConversationId)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private var activeGenerationJob: Job? = null

    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    fun getMessagesForConversation(convId: String): Flow<List<ChatMessageEntity>> {
        return messageDao.getMessagesForConversation(convId)
    }

    suspend fun getOrCreateActiveConversation(): String = withContext(Dispatchers.IO) {
        val currentId = _activeConversationId.value
        if (currentId != null) {
            val exists = conversationDao.getConversationById(currentId)
            if (exists != null) return@withContext currentId
        }

        // Try getting first existing conversation
        val firstExisting = conversationDao.getAllConversations().firstOrNull()?.firstOrNull()
        if (firstExisting != null) {
            setActiveConversation(firstExisting.id)
            return@withContext firstExisting.id
        }

        // Otherwise create brand new
        createNewConversation("New Chat")
    }

    suspend fun createNewConversation(initialTitle: String = "New Chat"): String = withContext(Dispatchers.IO) {
        val newId = UUID.randomUUID().toString()
        val conversation = ConversationEntity(
            id = newId,
            title = initialTitle,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        conversationDao.insertConversation(conversation)
        setActiveConversation(newId)
        newId
    }

    fun setActiveConversation(id: String) {
        _activeConversationId.value = id
        preferences.activeConversationId = id
    }

    suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessagesForConversation(id)
        conversationDao.deleteConversation(id)
        if (_activeConversationId.value == id) {
            val next = conversationDao.getAllConversations().firstOrNull()?.firstOrNull()
            if (next != null) {
                setActiveConversation(next.id)
            } else {
                _activeConversationId.value = null
                preferences.activeConversationId = null
            }
        }
    }

    suspend fun clearAllConversations() = withContext(Dispatchers.IO) {
        stopGeneration()
        conversationDao.clearAllConversations()
        _activeConversationId.value = null
        preferences.activeConversationId = null
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _isGenerating.value = false
    }

    fun sendMessage(
        userText: String,
        scope: CoroutineScope = repositoryScope,
        onComplete: (() -> Unit)? = null
    ) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isGenerating.value) return

        // Always run on repositoryScope so background streaming continues when window minimizes
        activeGenerationJob = repositoryScope.launch {
            try {
                _isGenerating.value = true
                val convId = getOrCreateActiveConversation()

                // Insert user message
                val userMsgId = UUID.randomUUID().toString()
                val userMsg = ChatMessageEntity(
                    id = userMsgId,
                    conversationId = convId,
                    role = "user",
                    content = trimmed,
                    timestamp = System.currentTimeMillis()
                )
                messageDao.insertMessage(userMsg)

                // Update conversation title if default
                val conv = conversationDao.getConversationById(convId)
                if (conv != null && (conv.title == "New Chat" || conv.title.isBlank())) {
                    val smartTitle = if (trimmed.length > 32) trimmed.take(30) + "..." else trimmed
                    conversationDao.updateConversation(conv.copy(title = smartTitle, updatedAt = System.currentTimeMillis()))
                }

                // Prepare placeholder for model response
                val modelMsgId = UUID.randomUUID().toString()
                val modelMsg = ChatMessageEntity(
                    id = modelMsgId,
                    conversationId = convId,
                    role = "model",
                    content = "",
                    timestamp = System.currentTimeMillis() + 1,
                    isStreaming = true
                )
                messageDao.insertMessage(modelMsg)

                // Fetch past conversation messages for context
                val pastEntities = messageDao.getMessagesList(convId)
                val messagesPayload = pastEntities
                    .filter { !it.isStreaming && !it.isError && it.content.isNotBlank() }
                    .takeLast(16) // Keep sensible context window
                    .map { MessagePayload(role = it.role, content = it.content) }

                val aiService: AiService = when (preferences.providerType) {
                    AiProviderType.GEMINI -> geminiClient
                    AiProviderType.OPENAI_COMPATIBLE -> openAiClient
                }

                // Retrieve relevant project knowledge from imported memory database
                val relevantMemory = memoryRepository.findRelevantKnowledge(trimmed)
                val fullSystemInstruction = if (relevantMemory.isNotBlank()) {
                    preferences.systemPrompt + relevantMemory
                } else {
                    preferences.systemPrompt
                }

                val accumulated = StringBuilder()
                var lastDbUpdateTime = System.currentTimeMillis()

                try {
                    aiService.streamResponse(
                        messages = messagesPayload,
                        systemInstruction = fullSystemInstruction,
                        temperature = preferences.temperature
                    ).collect { chunk ->
                        accumulated.append(chunk)
                        val now = System.currentTimeMillis()
                        // Throttle DB updates to avoid overloading UI recomposition
                        if (now - lastDbUpdateTime > 100) {
                            lastDbUpdateTime = now
                            messageDao.updateMessageContent(
                                messageId = modelMsgId,
                                content = accumulated.toString(),
                                isStreaming = true,
                                isError = false
                            )
                        }
                    }

                    // Streaming finished
                    val finalContent = if (accumulated.isBlank()) "No response received." else accumulated.toString()
                    messageDao.updateMessageContent(
                        messageId = modelMsgId,
                        content = finalContent,
                        isStreaming = false,
                        isError = false
                    )

                } catch (e: CancellationException) {
                    // Cleanly stop streaming on cancellation without recording false errors
                    if (accumulated.isNotEmpty()) {
                        messageDao.updateMessageContent(
                            messageId = modelMsgId,
                            content = accumulated.toString(),
                            isStreaming = false,
                            isError = false
                        )
                    } else {
                        messageDao.deleteMessage(modelMsgId)
                    }
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "AI Generation failed: ${e.message}", e)
                    val errMsg = e.message ?: "Failed to generate AI response. Please check your network and API key."
                    val displayContent = if (accumulated.isNotEmpty()) {
                        accumulated.toString() + "\n\n⚠️ *Generation halted: $errMsg*"
                    } else {
                        "⚠️ **Error**: $errMsg\n\n*Tip: Check your API key in Settings.*"
                    }
                    messageDao.updateMessageContent(
                        messageId = modelMsgId,
                        content = displayContent,
                        isStreaming = false,
                        isError = true
                    )
                }

            } finally {
                _isGenerating.value = false
                activeGenerationJob = null
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    fun regenerateLastResponse(scope: CoroutineScope = repositoryScope) {
        repositoryScope.launch {
            val convId = _activeConversationId.value ?: return@launch
            val messages = messageDao.getMessagesList(convId)
            if (messages.isEmpty()) return@launch

            val lastMsg = messages.last()
            if (lastMsg.role == "model") {
                // Delete last model message and re-query
                messageDao.deleteMessage(lastMsg.id)
                val remaining = messageDao.getMessagesList(convId)
                val lastUserMsg = remaining.lastOrNull { it.role == "user" }
                if (lastUserMsg != null) {
                    messageDao.deleteMessage(lastUserMsg.id)
                    sendMessage(lastUserMsg.content)
                }
            } else if (lastMsg.role == "user") {
                messageDao.deleteMessage(lastMsg.id)
                sendMessage(lastMsg.content)
            }
        }
    }
}
