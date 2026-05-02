package `in`.ssverma.glee.domain

import `in`.ssverma.glee.features.chat.domain.model.AiTool
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import kotlinx.coroutines.flow.Flow

/**
 * Common interface for the LiteRT-LM (Gemma) model engine.
 */
expect class LiteRtEngine() : AiEngine {
    override suspend fun loadModel(config: ModelConfig): Result<Unit>

    override fun generateResponse(prompt: String, files: List<AttachedFile>): Flow<AiChunk>

    override fun setSystemPrompt(prompt: String)

    override fun setSkills(skills: List<AiTool>)

    override suspend fun clearConversation()

    override val isLowConstraintDevice: Boolean

    override suspend fun close()
}
