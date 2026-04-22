package `in`.ssverma.glee.domain.model

import kotlinx.serialization.Serializable

/**
 * Rich metadata for AI models, defining capabilities and resources.
 */
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
    val downloadStatus: ModelDownloadStatus = ModelDownloadStatus.NotDownloaded
)

sealed interface ModelDownloadStatus {
    data object NotDownloaded : ModelDownloadStatus
    data class Downloading(val progress: Float) : ModelDownloadStatus
    data object Downloaded : ModelDownloadStatus
    data class Error(val message: String) : ModelDownloadStatus
}

@Serializable
data class GleeModelConfig(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val useGpu: Boolean = false,
    val enableThinking: Boolean = false
)

