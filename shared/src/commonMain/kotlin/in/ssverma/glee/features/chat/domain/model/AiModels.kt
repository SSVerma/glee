package `in`.ssverma.glee.features.chat.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Rich metadata for AI models, defining capabilities and resources.
 */
@Immutable
@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val bestFor: String,
    val resourceUsage: String,
    val url: String,
    val infoUrl: String,
    val sizeGb: Float,
    val isRecommended: Boolean = false,
    val supportsThinking: Boolean = false,
    val supportsSkills: Boolean = true,
    val supportsVision: Boolean = false,
    val isCustom: Boolean = false,
    @kotlinx.serialization.Transient
    val downloadStatus: ModelDownloadStatus = ModelDownloadStatus.NotDownloaded
)

@Immutable
sealed interface ModelDownloadStatus {
    data object NotDownloaded : ModelDownloadStatus
    data class Downloading(val progress: Float) : ModelDownloadStatus
    data object Downloaded : ModelDownloadStatus
    data class Error(val message: String) : ModelDownloadStatus
}

@Immutable
@Serializable
enum class BackendType {
    Auto,
    Npu,
    Gpu,
    Cpu
}

@Immutable
@Serializable
data class GleeModelConfig(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val preferredBackend: BackendType = BackendType.Auto,
    val enableThinking: Boolean = false,
    val isAgentic: Boolean = false
)

@Immutable
data class ModelConfig(
    val modelPath: String,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val preferredBackend: BackendType = BackendType.Auto,
    val maxNumImages: Int = 0
)
