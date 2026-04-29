package `in`.ssverma.glee.features.chat.data.repository

import glee.shared.generated.resources.Res
import glee.shared.generated.resources.gemma_4_e2b_best_for
import glee.shared.generated.resources.gemma_4_e2b_desc
import glee.shared.generated.resources.gemma_4_e2b_name
import glee.shared.generated.resources.gemma_4_e2b_resource_usage
import glee.shared.generated.resources.gemma_4_e4b_best_for
import glee.shared.generated.resources.gemma_4_e4b_desc
import glee.shared.generated.resources.gemma_4_e4b_name
import glee.shared.generated.resources.gemma_4_e4b_resource_usage
import `in`.ssverma.glee.core.common.platform.GleeFileSystem
import `in`.ssverma.glee.core.common.platform.PlatformType
import `in`.ssverma.glee.core.common.platform.getPlatformType
import `in`.ssverma.glee.features.chat.domain.model.ModelDownloadStatus
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import org.jetbrains.compose.resources.getString

class AiModelRepository(
    private val fileSystem: GleeFileSystem,
    private val appDataDir: Path
) {
    private suspend fun getAllModelsStatic(): List<ModelInfo> {
        val isWeb = getPlatformType() == PlatformType.WasmJs || getPlatformType() == PlatformType.Js

        return listOf(
            ModelInfo(
                id = "gemma-4-e2b",
                name = getString(Res.string.gemma_4_e2b_name),
                description = getString(Res.string.gemma_4_e2b_desc),
                bestFor = getString(Res.string.gemma_4_e2b_best_for),
                resourceUsage = getString(Res.string.gemma_4_e2b_resource_usage),
                url = if (isWeb) {
                    "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-web.task"
                } else {
                    "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
                },
                infoUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
                sizeGb = 2.6f,
                supportsThinking = true,
                supportsSkills = true,
                supportsVision = true
            ),
            ModelInfo(
                id = "gemma-4-e4b",
                name = getString(Res.string.gemma_4_e4b_name),
                description = getString(Res.string.gemma_4_e4b_desc),
                bestFor = getString(Res.string.gemma_4_e4b_best_for),
                resourceUsage = getString(Res.string.gemma_4_e4b_resource_usage),
                url = if (isWeb) {
                    "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it-web.task"
                } else {
                    "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm"
                },
                infoUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm",
                sizeGb = 3.7f,
                supportsThinking = true,
                supportsSkills = true,
                supportsVision = true
            )
        )
    }

    suspend fun getModelsWithStatus(): List<ModelInfo> {
        val allModels = getAllModelsStatic()
        return allModels.map { model ->
            val path = appDataDir.resolve("${model.id}.litertlm")
            val exists = withContext(Dispatchers.Default) { fileSystem.exists(path) }
            if (exists) {
                model.copy(downloadStatus = ModelDownloadStatus.Downloaded)
            } else {
                model.copy(downloadStatus = ModelDownloadStatus.NotDownloaded)
            }
        }
    }
}
