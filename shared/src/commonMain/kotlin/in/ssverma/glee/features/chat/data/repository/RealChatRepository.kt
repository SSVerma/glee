package `in`.ssverma.glee.features.chat.data.repository

import `in`.ssverma.glee.core.database.GleeDatabase
import `in`.ssverma.glee.core.database.MessageEntity
import `in`.ssverma.glee.features.chat.domain.model.ChatMessage
import `in`.ssverma.glee.features.chat.domain.repository.ChatRepository

class RealChatRepository(private val db: GleeDatabase) : ChatRepository {

    override suspend fun getMessages(conversationId: String): List<ChatMessage> {
        return db.chatDao().getMessages(conversationId).map { it.toDomain() }
    }

    override suspend fun saveMessage(conversationId: String, message: ChatMessage) {
        db.chatDao().insertMessage(message.toEntity(conversationId))
    }

    private fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        role = role,
        content = content,
        timestamp = timestamp
    )

    private fun ChatMessage.toEntity(conversationId: String) = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp
    )
}
