package com.dettle.app.data.db.dao

import androidx.room.*
import com.dettle.app.data.db.entity.ConversationEntity
import com.dettle.app.data.db.entity.ConversationMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conv: ConversationEntity)

    @Update
    suspend fun updateConversation(conv: ConversationEntity)

    @Insert
    suspend fun insertMessage(msg: ConversationMessageEntity): Long

    @Query("SELECT * FROM conversations WHERE project_id IS :projectId ORDER BY updated_at DESC")
    fun observeConversations(projectId: String?): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY updated_at DESC LIMIT 20")
    fun observeRecent(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversation_messages WHERE conversation_id = :conversationId ORDER BY created_at ASC")
    suspend fun getMessages(conversationId: String): List<ConversationMessageEntity>

    @Query("UPDATE conversations SET title = :title, updated_at = :time WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET message_count = message_count + 1, updated_at = :time WHERE id = :id")
    suspend fun incrementMessageCount(id: String, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)
}
