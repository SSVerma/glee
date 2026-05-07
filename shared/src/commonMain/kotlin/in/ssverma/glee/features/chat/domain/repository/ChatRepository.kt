package `in`.ssverma.glee.features.chat.domain.repository

import `in`.ssverma.glee.features.chat.domain.model.ChatMessage
import `in`.ssverma.glee.features.chat.domain.model.Conversation

interface ChatRepository {
    suspend fun getConversations(): List<Conversation>
    suspend fun saveConversation(conversation: Conversation)
    suspend fun deleteConversation(conversationId: String)
    suspend fun getMessages(conversationId: String): List<ChatMessage>
    suspend fun saveMessage(conversationId: String, message: ChatMessage)
    suspend fun clearAllData()
}
