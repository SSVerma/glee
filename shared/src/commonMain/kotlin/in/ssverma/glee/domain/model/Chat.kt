package `in`.ssverma.glee.domain.model

import androidx.compose.runtime.Immutable
import `in`.ssverma.glee.domain.currentTimeMillis
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long = currentTimeMillis()
)

@Immutable
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
    @Immutable
    data class Success(val message: String) : SkillResult
    @Immutable
    data class Error(val message: String, val throwable: Throwable? = null) : SkillResult
    @Immutable
    data class Progress(val message: String) : SkillResult
}

/**
 * Represents a tool call request from the LLM.
 */
@Immutable
data class ToolCall(
    val skillId: String,
    val input: String
)

@Immutable
data class AttachedFile(
    val name: String,
    val path: String?,
    val size: Long,
    val platformFile: PlatformFile
)

@Immutable
data class ChatMetrics(
    val modelName: String = "No Model",
    val contextUsed: Int = 0,
    val contextMax: Int = 128000,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val latencyMs: Long = 0
)

@Immutable
data class MessageList(
    val messages: List<ChatMessage>
) : List<ChatMessage> by messages
