package `in`.ssverma.glee.domain

import `in`.ssverma.glee.features.chat.domain.model.AiTool
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import `in`.ssverma.glee.features.chat.domain.model.ToolCall
import kotlinx.coroutines.flow.Flow

/**
 * Common interface for AI model engines.
 */
interface AiEngine {
    suspend fun loadModel(config: ModelConfig): Result<Unit>

    fun generateResponse(prompt: String, files: List<AttachedFile> = emptyList()): Flow<AiChunk>

    fun setSystemPrompt(prompt: String)

    fun setSkills(skills: List<AiTool>)

    suspend fun clearConversation()

    suspend fun close()
}

/**
 * A chunk of text or tool call produced by the AI.
 */
data class AiChunk(
    val text: String = "",
    val toolCall: ToolCall? = null,
    val isFinal: Boolean = false
)
