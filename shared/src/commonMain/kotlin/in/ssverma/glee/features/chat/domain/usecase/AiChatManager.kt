package `in`.ssverma.glee.features.chat.domain.usecase

import `in`.ssverma.glee.core.common.currentTimeMillis
import `in`.ssverma.glee.domain.AiEngine
import `in`.ssverma.glee.domain.AiChunk
import `in`.ssverma.glee.features.chat.domain.model.*
import `in`.ssverma.glee.features.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/**
 * Manages the agentic chat loop, including tool calling and message history.
 */
class AiChatManager(
    private val engine: AiEngine,
    private val agentProcessor: AgentProcessor,
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
        engine.clearConversation()
        currentConversation = conversation
        val messages = repository.getMessages(conversation.id)
        _messages.update { messages }
        
        // Restore history to the engine's internal conversation buffer
        engine.setSystemPrompt(systemPrompt)
        messages.forEach { msg ->
            if (msg.role == ChatRole.User) {
                // We use a dummy collect because sendMessageAsync triggers generation.
                // LiteRT LM doesn't have a direct "addToHistory" without generation,
                // so we rely on the system to prefill it.
                // NOTE: This is a heavy operation for long histories.
                engine.generateResponse(msg.content).collect { }
            }
        }
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
        // In a production app, we might want to store enabled state
    }

    fun updateSystemPrompt(prompt: String) {
        systemPrompt = prompt
    }

    fun sendMessage(content: String, modelId: String, isPrivate: Boolean, isAgentic: Boolean): Flow<AiChunk> = flow {
        val userMessage = ChatMessage(
            id = randomId(),
            role = ChatRole.User,
            content = content
        )
        _messages.update { it + userMessage }

        val conversationId = if (!isPrivate) {
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
                newConv.id
            } else {
                repository.saveMessage(currentConversation!!.id, userMessage)
                currentConversation!!.id
            }
        } else null

        if (isAgentic) {
            agentProcessor.process(content, skills, systemPrompt).collect { event ->
                when (event) {
                    is AgenticEvent.ResponseChunk -> {
                        emit(event.chunk)
                    }
                    is AgenticEvent.Thought -> {
                        val assistantThought = ChatMessage(
                            id = randomId(),
                            role = ChatRole.Assistant,
                            content = event.text
                        )
                        _messages.update { it + assistantThought }
                        if (conversationId != null) repository.saveMessage(conversationId, assistantThought)
                    }
                    is AgenticEvent.ToolCallDetected -> {
                        emit(AiChunk(text = "", toolCall = event.toolCall, isFinal = false))
                    }
                    is AgenticEvent.ToolExecutionStarted -> {
                        val toolRequestMsg = ChatMessage(
                            id = randomId(),
                            role = ChatRole.Tool,
                            content = event.skillId
                        )
                        _messages.update { it + toolRequestMsg }
                        if (conversationId != null) repository.saveMessage(conversationId, toolRequestMsg)
                    }
                    is AgenticEvent.ToolProgress -> {
                        // Optionally handle progress
                    }
                    is AgenticEvent.ToolResultReceived -> {
                        val toolResultMsg = ChatMessage(
                            id = randomId(),
                            role = ChatRole.Assistant,
                            content = "Tool Result: ${event.result}"
                        )
                        _messages.update { it + toolResultMsg }
                        if (conversationId != null) repository.saveMessage(conversationId, toolResultMsg)
                    }
                    is AgenticEvent.Error -> {
                        emit(AiChunk(text = "Error: ${event.message}", isFinal = false))
                    }
                }
            }
        } else {
            engine.setSystemPrompt(systemPrompt)
            engine.generateResponse(content).collect { chunk ->
                emit(chunk)
            }
        }
    }

    private fun randomId() = (currentTimeMillis() + (0..1000).random()).toString()

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
            repository.saveMessage(
                conversationId = currentConversation?.id.orEmpty(),
                message = assistantMessage
            )
        }
    }

    suspend fun clearChat() {
        engine.clearConversation()
        _messages.update { emptyList() }
        currentConversation = null
    }
}
