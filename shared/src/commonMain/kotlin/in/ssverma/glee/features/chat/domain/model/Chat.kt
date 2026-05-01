package `in`.ssverma.glee.features.chat.domain.model

import androidx.compose.runtime.Immutable
import `in`.ssverma.glee.core.common.currentTimeMillis
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val attachments: List<MessageAttachment> = emptyList(),
    val timestamp: Long = currentTimeMillis()
)

@Immutable
@Serializable
data class MessageAttachment(
    val name: String,
    val path: String?,
    val size: Long
)

@Immutable
@Serializable
data class Conversation(
    val id: String,
    val title: String,
    val modelId: String,
    val createdAt: Long = currentTimeMillis()
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
sealed interface ToolResult {
    @Immutable
    data class Success(val message: String) : ToolResult
    @Immutable
    data class Error(val message: String, val throwable: Throwable? = null) : ToolResult
    @Immutable
    data class Progress(val message: String) : ToolResult
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
    val modelName: String = "",
    val contextUsed: Int = 0,
    val contextMax: Int = 32000,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val latencyMs: Long = 0
)

@Immutable
data class MessageList(
    val messages: List<ChatMessage>
) : List<ChatMessage> by messages
