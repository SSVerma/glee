package `in`.ssverma.glee.features.chat.data.local

import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import `in`.ssverma.glee.features.chat.domain.model.ToolCall
import `in`.ssverma.glee.features.chat.domain.model.AiSkill
import kotlinx.coroutines.flow.Flow

/**
 * Common interface for the LiteRT-LM (Gemma) model engine.
 */
expect class LiteRtEngine() {

    suspend fun loadModel(config: ModelConfig): Result<Unit>

    fun generateResponse(prompt: String): Flow<AiChunk>

    fun setSkills(skills: List<AiSkill>)

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
