package `in`.ssverma.glee.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class LiteRtEngine actual constructor() {
    actual suspend fun loadModel(config: ModelConfig): Result<Unit> {
        return Result.success(Unit)
    }

    actual fun generateResponse(prompt: String): Flow<AiChunk> {
        return flow { 
            emit(AiChunk("iOS LiteRT implementation coming soon...", isFinal = true))
        }
    }

    actual fun setSkills(skills: List<Skill>) {
        // TODO: Implement tool calling
    }

    actual fun close() {
        // TODO: Release resources
    }
}
