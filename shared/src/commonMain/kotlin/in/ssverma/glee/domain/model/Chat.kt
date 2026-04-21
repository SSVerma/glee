package `in`.ssverma.glee.domain.model

import `in`.ssverma.glee.domain.currentTimeMillis
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long = currentTimeMillis()
)

@Serializable
enum class ChatRole {
    User,
    Assistant,
    System,
    Tool
}

/**
 * Result of a skill execution.
 */
sealed interface SkillResult {
    data class Success(val message: String) : SkillResult
    data class Error(val message: String, val throwable: Throwable? = null) : SkillResult
    data class Progress(val message: String) : SkillResult
}

/**
 * Represents a tool call request from the LLM.
 */
data class ToolCall(
    val skillId: String,
    val input: String
)

data class AttachedFile(
    val name: String,
    val path: String?,
    val size: Long,
    val platformFile: PlatformFile
)

data class ChatMetrics(
    val modelName: String = "No Model",
    val contextUsed: Int = 0,
    val contextMax: Int = 128000,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val latencyMs: Long = 0
)
