package `in`.ssverma.glee.data

import `in`.ssverma.glee.data.db.GleeDatabase
import `in`.ssverma.glee.data.db.MessageEntity
import `in`.ssverma.glee.domain.model.ChatMessage

class ChatRepository(private val db: GleeDatabase) {

    suspend fun getMessages(conversationId: String): List<ChatMessage> {
        return db.chatDao().getMessages(conversationId).map { it.toDomain() }
    }

    suspend fun saveMessage(conversationId: String, message: ChatMessage) {
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
