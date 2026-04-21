package `in`.ssverma.glee.domain

import `in`.ssverma.glee.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the agentic chat loop, including tool calling and message history.
 */
class AiChatManager(
    private val engine: LiteRtEngine
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val skills = mutableMapOf<String, Skill>()
    private var systemPrompt: String = "You are Glee, a short, crisp, and on-point AI assistant. Keep your answers concise."

    fun registerSkill(skill: Skill) {
        skills[skill.id] = skill
        engine.setSkills(skills.values.toList())
    }

    fun toggleSkill(skillId: String, enabled: Boolean) {
        // Logic to enable/disable skill in engine if supported
    }

    fun updateSystemPrompt(prompt: String) {
        systemPrompt = prompt
    }

    suspend fun sendMessage(content: String): Flow<AiChunk> {
        val userMessage = ChatMessage(
            id = randomId(),
            role = ChatRole.User,
            content = content
        )
        _messages.update { it + userMessage }

        // Start engine generation
        return engine.generateResponse(content)
    }

    /**
     * Commits a completed assistant message to history.
     */
    fun commitAssistantMessage(content: String) {
        val assistantMessage = ChatMessage(
            id = randomId(),
            role = ChatRole.Assistant,
            content = content
        )
        _messages.update { it + assistantMessage }
    }

    fun clearChat() {
        _messages.update { emptyList() }
    }

    private fun randomId() = (currentTimeMillis() + (0..1000).random()).toString()
}
