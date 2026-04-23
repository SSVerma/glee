package `in`.ssverma.glee.features.chat.domain.usecase

import `in`.ssverma.glee.core.common.currentTimeMillis
import `in`.ssverma.glee.features.chat.data.local.AiChunk
import `in`.ssverma.glee.features.chat.data.local.LiteRtEngine
import `in`.ssverma.glee.features.chat.domain.model.*
import `in`.ssverma.glee.features.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the agentic chat loop, including tool calling and message history.
 */
class AiChatManager(
    private val engine: LiteRtEngine,
    private val repository: ChatRepository
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var currentConversation: Conversation? = null
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val skills = mutableMapOf<String, AiSkill>()
    private var systemPrompt: String = ""

    suspend fun loadConversations() {
        val list = repository.getConversations()
        _conversations.update { list }
    }

    suspend fun startConversation(conversation: Conversation) {
        currentConversation = conversation
        val messages = repository.getMessages(conversation.id)
        _messages.update { messages }
    }

    suspend fun deleteConversation(conversationId: String) {
        repository.deleteConversation(conversationId)
        loadConversations()
        if (currentConversation?.id == conversationId) {
            clearChat()
        }
    }

    fun registerSkill(skill: AiSkill) {
        skills[skill.id] = skill
        engine.setSkills(skills.values.toList())
    }

    fun toggleSkill(skillId: String, enabled: Boolean) {
        // Logic to enable/disable skill in engine if supported
    }

    fun updateSystemPrompt(prompt: String) {
        systemPrompt = prompt
    }

    suspend fun sendMessage(content: String, modelId: String, isPrivate: Boolean): Flow<AiChunk> {
        val userMessage = ChatMessage(
            id = randomId(),
            role = ChatRole.User,
            content = content
        )
        _messages.update { it + userMessage }

        if (!isPrivate) {
            if (currentConversation == null) {
                val newConv = Conversation(
                    id = randomId(),
                    title = content.take(30),
                    modelId = modelId
                )
                currentConversation = newConv
                repository.saveConversation(newConv)
                repository.saveMessage(newConv.id, userMessage)
                loadConversations()
            } else {
                repository.saveMessage(currentConversation!!.id, userMessage)
            }
        }

        // Start engine generation
        return engine.generateResponse(content)
    }

    /**
     * Commits a completed assistant message to history.
     */
    suspend fun commitAssistantMessage(content: String, isPrivate: Boolean) {
        val assistantMessage = ChatMessage(
            id = randomId(),
            role = ChatRole.Assistant,
            content = content
        )
        _messages.update { it + assistantMessage }

        if (!isPrivate && currentConversation != null) {
            repository.saveMessage(currentConversation!!.id, assistantMessage)
        }
    }

    fun clearChat() {
        _messages.update { emptyList() }
        currentConversation = null
    }

    private fun randomId() = (currentTimeMillis() + (0..1000).random()).toString()
}
