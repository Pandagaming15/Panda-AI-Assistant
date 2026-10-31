package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesList(conversationId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET content = :content, isStreaming = :isStreaming, isError = :isError WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, content: String, isStreaming: Boolean, isError: Boolean)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
}

@Dao
interface MemoryDocumentDao {
    @Query("SELECT * FROM memory_documents ORDER BY filename ASC")
    fun getAllDocuments(): Flow<List<MemoryDocumentEntity>>

    @Query("SELECT * FROM memory_documents WHERE isEnabled = 1")
    suspend fun getEnabledDocuments(): List<MemoryDocumentEntity>

    @Query("SELECT * FROM memory_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): MemoryDocumentEntity?

    @Query("SELECT COUNT(*) FROM memory_documents")
    fun getDocumentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM memory_documents")
    suspend fun getDocumentCountSync(): Int

    @Query("SELECT * FROM memory_documents WHERE filename LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' LIMIT 40")
    suspend fun searchDocuments(query: String): List<MemoryDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: MemoryDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(docs: List<MemoryDocumentEntity>)

    @Update
    suspend fun updateDocument(doc: MemoryDocumentEntity)

    @Query("UPDATE memory_documents SET isEnabled = :enabled WHERE id = :id")
    suspend fun setDocumentEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM memory_documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Query("DELETE FROM memory_documents")
    suspend fun clearAllDocuments()
}
