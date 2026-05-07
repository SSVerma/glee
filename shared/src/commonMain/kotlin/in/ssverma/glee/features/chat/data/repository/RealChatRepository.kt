package `in`.ssverma.glee.features.chat.data.repository

import `in`.ssverma.glee.core.database.ConversationEntity
import `in`.ssverma.glee.core.database.GleeDatabase
import `in`.ssverma.glee.core.database.MessageEntity
import `in`.ssverma.glee.features.chat.domain.model.ChatMessage
import `in`.ssverma.glee.features.chat.domain.model.Conversation
import `in`.ssverma.glee.features.chat.domain.model.MessageAttachment
import `in`.ssverma.glee.features.chat.domain.repository.ChatRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RealChatRepository(
    private val db: GleeDatabase,
    private val json: Json
) : ChatRepository {

    override suspend fun getConversations(): List<Conversation> {
        return db.chatDao().getConversations().map { it.toDomain() }
    }

    override suspend fun saveConversation(conversation: Conversation) {
        db.chatDao().insertConversation(conversation.toEntity())
    }

    override suspend fun deleteConversation(conversationId: String) {
        db.chatDao().deleteConversation(conversationId)
        db.chatDao().deleteMessages(conversationId)
    }

    override suspend fun getMessages(conversationId: String): List<ChatMessage> {
        return db.chatDao().getMessages(conversationId).map { it.toDomain() }
    }

    override suspend fun saveMessage(conversationId: String, message: ChatMessage) {
        db.chatDao().insertMessage(message.toEntity(conversationId))
    }

    override suspend fun clearAllData() {
        db.chatDao().clearConversations()
        db.chatDao().clearMessages()
    }

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        modelId = modelId,
        createdAt = createdAt
    )

    private fun Conversation.toEntity() = ConversationEntity(
        id = id,
        title = title,
        modelId = modelId,
        createdAt = createdAt
    )

    private fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        role = role,
        content = content,
        timestamp = timestamp,
        attachments = attachmentsJson?.let { 
            runCatching { json.decodeFromString<List<MessageAttachment>>(it) }.getOrNull() 
        } ?: emptyList()
    )

    private fun ChatMessage.toEntity(conversationId: String) = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp,
        attachmentsJson = if (attachments.isNotEmpty()) json.encodeToString(attachments) else null
    )
}
