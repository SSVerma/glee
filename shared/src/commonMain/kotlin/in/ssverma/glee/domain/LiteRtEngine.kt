package `in`.ssverma.glee.domain

import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import `in`.ssverma.glee.features.chat.domain.model.AiSkill
import kotlinx.coroutines.flow.Flow

/**
 * Common interface for the LiteRT-LM (Gemma) model engine.
 */
expect class LiteRtEngine() : AiEngine {
    override suspend fun loadModel(config: ModelConfig): Result<Unit>
    override fun generateResponse(prompt: String, files: List<`in`.ssverma.glee.features.chat.domain.model.AttachedFile>): Flow<AiChunk>
    override fun setSystemPrompt(prompt: String)
    override fun setSkills(skills: List<AiSkill>)
    override suspend fun clearConversation()
    override suspend fun close()
}
