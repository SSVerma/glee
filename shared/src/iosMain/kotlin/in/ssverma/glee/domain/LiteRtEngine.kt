package `in`.ssverma.glee.domain

import `in`.ssverma.glee.features.chat.domain.model.AiTool
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual class LiteRtEngine actual constructor() : AiEngine {

    actual override suspend fun loadModel(config: ModelConfig): Result<Unit> {
        return Result.failure(Exception("iOS inference is not supported yet."))
    }

    actual override fun generateResponse(
        prompt: String,
        files: List<AttachedFile>
    ): Flow<AiChunk> = emptyFlow()

    actual override fun setSystemPrompt(prompt: String) {
        // No-op for iOS
    }

    actual override fun setSkills(skills: List<AiTool>) {
        // No-op for iOS
    }

    actual override suspend fun clearConversation() {
        // No-op for iOS
    }

    actual override val isLowConstraintDevice: Boolean = false

    actual override suspend fun close() {
        // No-op for iOS
    }
}
