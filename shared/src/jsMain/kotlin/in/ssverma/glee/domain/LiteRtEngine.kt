package `in`.ssverma.glee.domain

import `in`.ssverma.glee.features.chat.domain.model.AiTool
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class LiteRtEngine actual constructor() : AiEngine {
    actual override suspend fun loadModel(config: ModelConfig): Result<Unit> {
        return Result.success(Unit)
    }

    actual override fun generateResponse(
        prompt: String,
        files: List<AttachedFile>
    ): Flow<AiChunk> {
        return flow {
            emit(AiChunk("JS LiteRT implementation coming soon...", isFinal = true))
        }
    }

    actual override fun setSystemPrompt(prompt: String) {
    }

    actual override fun setSkills(skills: List<AiTool>) {
    }

    actual override suspend fun clearConversation() {
    }

    actual override val isLowConstraintDevice: Boolean = false

    actual override suspend fun close() {
    }
}
