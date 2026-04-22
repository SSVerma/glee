package `in`.ssverma.glee.features.chat.domain.repository

import `in`.ssverma.glee.features.chat.domain.model.ChatMessage

interface ChatRepository {
    suspend fun getMessages(conversationId: String): List<ChatMessage>
    suspend fun saveMessage(conversationId: String, message: ChatMessage)
}
