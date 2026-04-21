package `in`.ssverma.glee.domain

import `in`.ssverma.glee.domain.model.ToolCall
import kotlinx.coroutines.flow.Flow

/**
 * Common interface for the LiteRT-LM (Gemma) model engine.
 */
expect class LiteRtEngine() {

    suspend fun loadModel(config: ModelConfig): Result<Unit>

    fun generateResponse(prompt: String): Flow<AiChunk>

    fun setSkills(skills: List<Skill>)

    fun close()
}

/**
 * A chunk of text or tool call produced by the AI.
 */
data class AiChunk(
    val text: String = "",
    val toolCall: ToolCall? = null,
    val isFinal: Boolean = false
)
