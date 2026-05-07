package `in`.ssverma.glee.features.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.ssverma.glee.core.common.platform.GleeFileSystem
import `in`.ssverma.glee.core.common.platform.PermissionManager
import `in`.ssverma.glee.core.common.platform.PermissionType
import `in`.ssverma.glee.core.common.platform.UrlLauncher
import `in`.ssverma.glee.core.preferences.GleeSettings
import `in`.ssverma.glee.features.chat.data.remote.DownloadStatus
import `in`.ssverma.glee.features.chat.data.remote.ModelDownloader
import `in`.ssverma.glee.features.chat.data.repository.AiModelRepository
import `in`.ssverma.glee.features.chat.domain.model.ModelDownloadStatus
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path

class ModelManagementViewModel(
    private val settings: GleeSettings,
    private val modelRepository: AiModelRepository,
    private val modelDownloader: ModelDownloader,
    private val fileSystem: GleeFileSystem,
    private val appDataDir: Path,
    private val permissionManager: PermissionManager,
    private val urlLauncher: UrlLauncher
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelManagementState())
    val uiState: StateFlow<ModelManagementState> = _uiState.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()
    private var importJob: Job? = null

    init {
        initializeModels()
        viewModelScope.launch {
            modelRepository.modelsChanged.collect {
                initializeModels()
            }
        }
    }

    private fun initializeModels() {
        viewModelScope.launch {
            val updatedModels = modelRepository.getModelsWithStatus()

            // Merge repository state with active job state
            val finalModels = updatedModels.map { repoModel ->
                val activeModel = _uiState.value.availableModels.find { it.id == repoModel.id }
                if (activeModel?.downloadStatus is ModelDownloadStatus.Downloading && repoModel.downloadStatus is ModelDownloadStatus.NotDownloaded) {
                    activeModel
                } else {
                    repoModel
                }
            }

            _uiState.update { it.copy(availableModels = finalModels) }

            // Trigger observation ONLY for models that are actually downloading in background
            // AND only if we aren't already managing them in this VM session.
            finalModels.forEach { model ->
                if (model.downloadStatus is ModelDownloadStatus.NotDownloaded && !downloadJobs.containsKey(model.id)) {
                    val observation = modelDownloader.observeDownload(model.id)
                    if (observation != null) {
                        // Check if observation actually emits anything BEFORE adding to downloadJobs
                        // To avoid blocking future download clicks with "empty" jobs
                        startBackgroundObservation(model, observation)
                    }
                }
            }
        }
    }

    private fun startBackgroundObservation(model: ModelInfo, flow: Flow<DownloadStatus>) {
        viewModelScope.launch {
            flow.collect { status ->
                if (status is DownloadStatus.Progress) {
                    // Only "take over" the job if it actually starts emitting progress
                    if (!downloadJobs.containsKey(model.id)) {
                        downloadJobs[model.id] = coroutineContext[Job]!!
                    }
                    updateModelStatus(model.id, ModelDownloadStatus.Downloading(status.progress))
                } else if (status is DownloadStatus.Success) {
                    updateModelStatus(model.id, ModelDownloadStatus.Downloaded)
                    downloadJobs.remove(model.id)
                    modelRepository.notifyModelsChanged()
                } else if (status is DownloadStatus.Error) {
                    updateModelStatus(model.id, ModelDownloadStatus.Error(status.message))
                    downloadJobs.remove(model.id)
                }
            }
        }
    }

    fun onIntent(intent: ModelManagementIntent) {
        when (intent) {
            is ModelManagementIntent.UpdateHfToken -> _uiState.update { it.copy(hfToken = intent.token) }
            is ModelManagementIntent.DownloadModel -> downloadModel(intent.model)
            is ModelManagementIntent.SelectModel -> {
                if (intent.model.downloadStatus == ModelDownloadStatus.Downloaded) {
                    settings.setSelectedModelId(intent.model.id)
                }
            }

            is ModelManagementIntent.CancelDownload -> {
                _uiState.update {
                    it.copy(
                        showCancelDownloadDialog = true,
                        modelToCancelDownloadId = intent.modelId
                    )
                }
            }

            ModelManagementIntent.ConfirmCancelDownload -> {
                val modelId = _uiState.value.modelToCancelDownloadId
                if (modelId != null) {
                    cancelDownload(modelId)
                }
                _uiState.update {
                    it.copy(
                        showCancelDownloadDialog = false,
                        modelToCancelDownloadId = null
                    )
                }
            }

            ModelManagementIntent.DismissCancelDownload -> {
                _uiState.update {
                    it.copy(
                        showCancelDownloadDialog = false,
                        modelToCancelDownloadId = null
                    )
                }
            }

            is ModelManagementIntent.DeleteModel -> _uiState.update { it.copy(modelToDelete = intent.model) }
            ModelManagementIntent.ConfirmDeleteModel -> {
                _uiState.value.modelToDelete?.let { model ->
                    deleteModel(model)
                    _uiState.update { it.copy(modelToDelete = null) }
                }
            }

            ModelManagementIntent.CancelDeleteModel -> _uiState.update { it.copy(modelToDelete = null) }
            ModelManagementIntent.ResetImportError -> _uiState.update {
                it.copy(
                    importError = null,
                    isImporting = false
                )
            }

            is ModelManagementIntent.ImportModelFile -> {
                importJob?.cancel()
                _uiState.update {
                    it.copy(
                        isImporting = true,
                        importError = null,
                        importProgress = 0f
                    )
                }
                importJob = viewModelScope.launch {
                    modelRepository.importModel(intent.file) { progress ->
                        _uiState.update { it.copy(importProgress = progress) }
                    }.onSuccess {
                        _uiState.update { it.copy(isImporting = false) }
                        initializeModels()
                    }.onFailure { error ->
                        if (error !is CancellationException) {
                            _uiState.update {
                                it.copy(
                                    isImporting = false,
                                    importError = error.message
                                )
                            }
                        }
                    }
                }
            }

            ModelManagementIntent.CancelImport -> {
                importJob?.cancel()
                _uiState.update { it.copy(isImporting = false, importProgress = 0f) }
            }

            is ModelManagementIntent.RequestDownloadModel -> {
                if (permissionManager.isPermissionGranted(PermissionType.Notifications)) {
                    downloadModel(intent.model)
                } else {
                    val shouldShowRationale = permissionManager.shouldShowRationale(PermissionType.Notifications)
                    viewModelScope.launch {
                        val hasRequested = settings.hasRequestedNotifications.first()
                        _uiState.update { 
                            it.copy(
                                modelForNotificationRationale = intent.model,
                                isPermanentlyDenied = hasRequested && !shouldShowRationale
                            )
                        }
                    }
                }
            }

            ModelManagementIntent.DismissNotificationRationale -> {
                settings.setHasRequestedNotifications(true)
                _uiState.update { it.copy(modelForNotificationRationale = null) }
            }

            ModelManagementIntent.OpenAppSettings -> {
                urlLauncher.openAppSettings()
                _uiState.update { it.copy(modelForNotificationRationale = null) }
            }
        }
    }

    fun downloadModel(model: ModelInfo) {
        // Cancel any existing observation or download job for this model
        downloadJobs[model.id]?.cancel()

        val targetPath = appDataDir.resolve("${model.id}.litertlm")
        val job = viewModelScope.launch {
            modelDownloader.downloadModel(
                url = model.url,
                targetPath = targetPath,
                token = _uiState.value.hfToken
            ).collect { status ->
                when (status) {
                    is DownloadStatus.Progress -> updateModelStatus(
                        model.id,
                        ModelDownloadStatus.Downloading(status.progress)
                    )

                    is DownloadStatus.Success -> {
                        updateModelStatus(model.id, ModelDownloadStatus.Downloaded)
                        downloadJobs.remove(model.id)
                        modelRepository.notifyModelsChanged()

                        val savedSelectedId = settings.selectedModelId.first()
                        if (savedSelectedId == null) {
                            settings.setSelectedModelId(model.id)
                        }
                    }

                    is DownloadStatus.Error -> {
                        updateModelStatus(model.id, ModelDownloadStatus.Error(status.message))
                        downloadJobs.remove(model.id)
                        viewModelScope.launch(Dispatchers.Default) {
                            runCatching {
                                fileSystem.delete(
                                    targetPath
                                )
                            }
                        }
                    }
                }
            }
        }
        downloadJobs[model.id] = job
    }

    private fun cancelDownload(modelId: String) {
        modelDownloader.cancelDownload(modelId)
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)
        updateModelStatus(modelId, ModelDownloadStatus.NotDownloaded)
        val targetPath = appDataDir.resolve("${modelId}.litertlm")
        viewModelScope.launch(Dispatchers.Default) { runCatching { fileSystem.delete(targetPath) } }
    }

    private fun deleteModel(model: ModelInfo) {
        viewModelScope.launch {
            // 1. Deselect first to trigger ChatViewModel engine close
            if (settings.selectedModelId.first() == model.id) {
                settings.setSelectedModelId(null)
                // Small delay to allow ChatViewModel to react and close the engine handle
                delay(100)
            }
            // 2. Actually delete the file
            modelRepository.deleteModel(model)
            initializeModels()
        }
    }

    private fun updateModelStatus(id: String, status: ModelDownloadStatus) {
        _uiState.update { state ->
            val updatedList = state.availableModels.map {
                if (it.id == id) it.copy(downloadStatus = status) else it
            }
            state.copy(availableModels = updatedList)
        }
    }
}
