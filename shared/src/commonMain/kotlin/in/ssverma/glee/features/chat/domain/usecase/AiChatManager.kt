package `in`.ssverma.glee.features.chat.domain.usecase

import `in`.ssverma.glee.core.common.currentTimeMillis
import `in`.ssverma.glee.domain.AiChunk
import `in`.ssverma.glee.domain.AiEngine
import `in`.ssverma.glee.features.chat.domain.model.AiTool
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ChatMessage
import `in`.ssverma.glee.features.chat.domain.model.ChatRole
import `in`.ssverma.glee.features.chat.domain.model.Conversation
import `in`.ssverma.glee.features.chat.domain.model.MessageAttachment
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

    private val skills = mutableMapOf<String, AiTool>()
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

        // Restore system prompt
        engine.setSystemPrompt(systemPrompt)

        // Note: LiteRT LM doesn't support direct history restoration without re-running generation.
        // We set the messages in the UI state, but the next message will be fresh unless we re-run history.
        // For now, we avoid re-running to prevent slow/stuck UI.
    }

    suspend fun deleteConversation(conversationId: String) {
        repository.deleteConversation(conversationId)
        loadConversations()
        if (currentConversation?.id == conversationId) {
            clearChat()
        }
    }

    fun registerSkill(skill: AiTool) {
        skills[skill.id] = skill
        engine.setSkills(skills.values.toList())
    }

    fun toggleSkill(skillId: String, enabled: Boolean) {
        // In a production app, we might want to store enabled state
    }

    fun updateSystemPrompt(prompt: String) {
        systemPrompt = prompt
    }

    fun sendMessage(
        uiPrompt: String,
        enginePrompt: String,
        modelId: String,
        isPrivate: Boolean,
        isAgentic: Boolean,
        files: List<AttachedFile> = emptyList(),
        historyFiles: List<AttachedFile> = emptyList()
    ): Flow<AiChunk> = flow {
        val userMessage = ChatMessage(
            id = randomId(),
            role = ChatRole.User,
            content = uiPrompt,
            attachments = historyFiles.map { MessageAttachment(it.name, it.path, it.size) }
        )
        _messages.update { it + userMessage }

        val conversationId = if (!isPrivate) {
            if (currentConversation == null) {
                val newConv = Conversation(
                    id = randomId(),
                    title = uiPrompt.ifBlank { "Image" }.take(30),
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
            agentProcessor.process(enginePrompt, skills, systemPrompt, files).collect { event ->
                when (event) {
                    is AgenticEvent.ResponseChunk -> {
                        emit(event.chunk)
                    }

                    is AgenticEvent.Thought -> {
                        val assistantThought = ChatMessage(
                            id = randomId(),
                            role = ChatRole.Assistant,
                            content = "💭 **Thinking...**\n\n${event.text}"
                        )
                        _messages.update { it + assistantThought }
                        if (conversationId != null) repository.saveMessage(
                            conversationId,
                            assistantThought
                        )
                    }

                    is AgenticEvent.ToolCallDetected -> {
                        emit(AiChunk(text = "", toolCall = event.toolCall, isFinal = false))
                    }

                    is AgenticEvent.ToolExecutionStarted -> {
                        val toolRequestMsg = ChatMessage(
                            id = "tool_${event.skillId}",
                            role = ChatRole.Tool,
                            content = "Executing: ${event.skillId}"
                        )
                        _messages.update { it + toolRequestMsg }
                        if (conversationId != null) repository.saveMessage(
                            conversationId,
                            toolRequestMsg
                        )
                    }

                    is AgenticEvent.ToolProgress -> {
                        // Optionally handle progress
                    }

                    is AgenticEvent.ToolResultReceived -> {
                        _messages.update { list ->
                            list.map { msg ->
                                if (msg.id == "tool_${event.skillId}") {
                                    msg.copy(content = "Completed: ${event.skillId}")
                                } else {
                                    msg
                                }
                            }
                        }
                        // Update in repository if needed, but for now we skip to keep history clean.
                    }

                    is AgenticEvent.Error -> {
                        emit(AiChunk(text = "Error: ${event.message}", isFinal = false))
                    }
                }
            }
        } else {
            // Currently agent Processor does not handle multimodal files in thought loop easily,
            // but we can pass it to the engine. Wait, we should update AgentProcessor to accept files too if needed.
            // For now, if it's not agentic:
            engine.setSystemPrompt(systemPrompt)
            engine.generateResponse(enginePrompt, files).collect { chunk ->
                emit(chunk)
            }
        }
    }

    private fun randomId() = "${currentTimeMillis()}-${(0..9999).random()}"

    /**
     * Commits a completed assistant message to history.
     */
    suspend fun commitAssistantMessage(
        content: String,
        isPrivate: Boolean,
        conversationId: String? = null
    ) {
        val assistantMessage = ChatMessage(
            id = randomId(),
            role = ChatRole.Assistant,
            content = content
        )
        _messages.update { it + assistantMessage }

        val targetConversationId = conversationId ?: currentConversation?.id
        if (!isPrivate && targetConversationId != null) {
            repository.saveMessage(
                conversationId = targetConversationId,
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
