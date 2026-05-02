package `in`.ssverma.glee.features.models

import androidx.compose.runtime.Immutable
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import io.github.vinceglb.filekit.core.PlatformFile

@Immutable
data class ModelManagementState(
    val availableModels: List<ModelInfo> = emptyList(),
    val hfToken: String = "",
    val modelToDelete: ModelInfo? = null,
    val showCancelDownloadDialog: Boolean = false,
    val modelToCancelDownloadId: String? = null,
    val isImporting: Boolean = false,
    val importProgress: Float = 0f,
    val importError: String? = null,
    val modelForNotificationRationale: ModelInfo? = null,
    val isPermanentlyDenied: Boolean = false
)

sealed interface ModelManagementIntent {
    data class UpdateHfToken(val token: String) : ModelManagementIntent
    data class DownloadModel(val model: ModelInfo) : ModelManagementIntent
    data class SelectModel(val model: ModelInfo) : ModelManagementIntent
    data class CancelDownload(val modelId: String) : ModelManagementIntent
    data object ConfirmCancelDownload : ModelManagementIntent
    data object DismissCancelDownload : ModelManagementIntent
    data class DeleteModel(val model: ModelInfo) : ModelManagementIntent
    data object ConfirmDeleteModel : ModelManagementIntent
    data object CancelDeleteModel : ModelManagementIntent
    data object ResetImportError : ModelManagementIntent
    data class ImportModelFile(val file: PlatformFile) : ModelManagementIntent
    data object CancelImport : ModelManagementIntent
    data class RequestDownloadModel(val model: ModelInfo) : ModelManagementIntent
    data object DismissNotificationRationale : ModelManagementIntent
    data object OpenAppSettings : ModelManagementIntent
}
