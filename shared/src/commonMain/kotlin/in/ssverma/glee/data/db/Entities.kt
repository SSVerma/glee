package `in`.ssverma.glee.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import `in`.ssverma.glee.domain.model.ChatRole

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val modelId: String
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long
)
