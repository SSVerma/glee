package `in`.ssverma.glee.features.chat.data.repository

import glee.shared.generated.resources.Res
import glee.shared.generated.resources.custom_inference
import glee.shared.generated.resources.depends_on_model_size
import glee.shared.generated.resources.gemma_4_e2b_best_for
import glee.shared.generated.resources.gemma_4_e2b_desc
import glee.shared.generated.resources.gemma_4_e2b_name
import glee.shared.generated.resources.gemma_4_e2b_resource_usage
import glee.shared.generated.resources.gemma_4_e4b_best_for
import glee.shared.generated.resources.gemma_4_e4b_desc
import glee.shared.generated.resources.gemma_4_e4b_name
import glee.shared.generated.resources.gemma_4_e4b_resource_usage
import glee.shared.generated.resources.import_unsupported_format
import glee.shared.generated.resources.imported_model_desc
import `in`.ssverma.glee.core.common.currentTimeMillis
import `in`.ssverma.glee.core.common.platform.GleeFileSystem
import `in`.ssverma.glee.core.common.platform.PlatformType
import `in`.ssverma.glee.core.common.platform.getPlatformType
import `in`.ssverma.glee.features.chat.domain.model.ModelDownloadStatus
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.Path
import org.jetbrains.compose.resources.getString

class AiModelRepository(
    private val fileSystem: GleeFileSystem,
    private val appDataDir: Path
) {
    private val _modelsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val modelsChanged: SharedFlow<Unit> = _modelsChanged.asSharedFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val customModelsFile = appDataDir.resolve("custom_models.json")
    private var customModelsList = mutableListOf<ModelInfo>()

    private suspend fun loadCustomModels() {
        try {
            if (fileSystem.exists(customModelsFile)) {
                fileSystem.readFile(customModelsFile).onSuccess { content ->
                    customModelsList =
                        json.decodeFromString<List<ModelInfo>>(content).toMutableList()
                }
            }
        } catch (e: Exception) {
            println("[Glee] Failed to load custom models: ${e.message}")
        }
    }

    private suspend fun saveCustomModels() {
        try {
            val content = json.encodeToString(customModelsList)
            fileSystem.writeFile(customModelsFile, content)
            _modelsChanged.tryEmit(Unit)
        } catch (e: Exception) {
            println("[Glee] Failed to save custom models: ${e.message}")
        }
    }

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

    private var isCustomModelsLoaded = false

    suspend fun getModelsWithStatus(): List<ModelInfo> {
        if (!isCustomModelsLoaded) {
            loadCustomModels()
            isCustomModelsLoaded = true
        }
        val allModels = getAllModelsStatic() + customModelsList
        return allModels.map { model ->
            // Use the URL as a hint for extension if available, otherwise check common ones
            val extension =
                if (model.url.contains(".task")) ".litertlm" else ".litertlm" // Actually we store it as .litertlm for static

            // For custom models, the ID might already contain the info or we can store the path
            val fileName = if (model.isCustom) {
                "${model.id}${model.url}" // Use url field to store original extension for custom models
            } else {
                "${model.id}.litertlm"
            }

            val path = appDataDir.resolve(fileName)
            val exists = withContext(Dispatchers.Default) { fileSystem.exists(path) }
            if (exists) {
                model.copy(downloadStatus = ModelDownloadStatus.Downloaded)
            } else {
                model.copy(downloadStatus = ModelDownloadStatus.NotDownloaded)
            }
        }
    }

    suspend fun importModel(
        file: PlatformFile,
        onProgress: (Float) -> Unit
    ): Result<ModelInfo> {
        onProgress(0.05f) // Immediate feedback
        return withContext(Dispatchers.Default) {
            try {
                val fileName = file.name
                val extension = ".${fileName.substringAfterLast(".", "task")}"
                val id = "custom-${fileName.substringBeforeLast(".")}-${currentTimeMillis()}"

                // We use .litertlm as the standard extension for all imported models 
                // because the model loader (ChatViewModel) currently expects this.
                val targetFileName = "$id.litertlm"
                val targetPath = appDataDir.resolve(targetFileName)

                // Basic validation
                val isSupported = listOf(".task", ".litertlm", ".bin", ".tflite").any {
                    extension.endsWith(it, ignoreCase = true)
                }

                if (!isSupported) {
                    return@withContext Result.failure(Exception(getString(resource = Res.string.import_unsupported_format)))
                }

                var success = false
                try {
                    fileSystem.importFile(file, targetPath) { progress ->
                        onProgress((0.1f + (progress * 0.9f)).coerceIn(0.1f, 1.0f))
                    }.onSuccess { success = true }.onFailure { throw it }
                } finally {
                    if (!success) {
                        fileSystem.delete(targetPath)
                    }
                }

                val model = ModelInfo(
                    id = id,
                    name = fileName.substringBeforeLast("."),
                    description = getString(resource = Res.string.imported_model_desc, fileName),
                    bestFor = getString(resource = Res.string.custom_inference),
                    resourceUsage = getString(resource = Res.string.depends_on_model_size),
                    url = ".litertlm",
                    infoUrl = "",
                    sizeGb = 0.1f, // PlatformFile.size is not available in the current version
                    supportsVision = extension.contains("task") || extension.contains("litertlm"),
                    isCustom = true
                )

                customModelsList.add(model)
                saveCustomModels()
                _modelsChanged.emit(Unit)

                Result.success(model)
            } catch (e: Throwable) {
                Result.failure(Exception(e.message, e))
            }
        }
    }

    suspend fun deleteModel(model: ModelInfo) {
        val extension = if (model.isCustom) model.url else ".litertlm"
        val fileName = "${model.id}$extension"

        customModelsList.removeAll { it.id == model.id }
        saveCustomModels()

        val path = appDataDir.resolve(fileName)
        if (fileSystem.exists(path)) {
            fileSystem.delete(path)
        }
        _modelsChanged.emit(Unit)
    }

    suspend fun notifyModelsChanged() {
        _modelsChanged.emit(Unit)
    }
}
