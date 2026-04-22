package `in`.ssverma.glee.features.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.ssverma.glee.features.chat.data.remote.DownloadStatus
import `in`.ssverma.glee.features.chat.data.remote.ModelDownloader
import `in`.ssverma.glee.core.common.platform.FileSystem
import `in`.ssverma.glee.features.chat.domain.usecase.AiChatManager
import `in`.ssverma.glee.features.chat.data.local.LiteRtEngine
import `in`.ssverma.glee.features.chat.domain.usecase.LocalFileSystemSkill
import `in`.ssverma.glee.features.chat.domain.model.*
import `in`.ssverma.glee.core.common.currentTimeMillis
import `in`.ssverma.glee.core.common.platform.getSystemMetrics
import `in`.ssverma.glee.core.preferences.GleeSettings
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.default_system_prompt
import glee.shared.generated.resources.gemma_3n_e2b_best_for
import glee.shared.generated.resources.gemma_3n_e2b_desc
import glee.shared.generated.resources.gemma_3n_e2b_name
import glee.shared.generated.resources.gemma_3n_e2b_resource_usage
import glee.shared.generated.resources.gemma_4_e2b_best_for
import glee.shared.generated.resources.gemma_4_e2b_desc
import glee.shared.generated.resources.gemma_4_e2b_name
import glee.shared.generated.resources.gemma_4_e2b_resource_usage
import glee.shared.generated.resources.gemma_4_e4b_best_for
import glee.shared.generated.resources.gemma_4_e4b_desc
import glee.shared.generated.resources.gemma_4_e4b_name
import glee.shared.generated.resources.gemma_4_e4b_resource_usage
import glee.shared.generated.resources.phi_4_best_for
import glee.shared.generated.resources.phi_4_desc
import glee.shared.generated.resources.phi_4_name
import glee.shared.generated.resources.phi_4_resource_usage
import glee.shared.generated.resources.loading_model_status
import glee.shared.generated.resources.model_load_failed_status
import glee.shared.generated.resources.model_ready_status
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import kotlinx.coroutines.CancellationException

class ChatViewModel(
    private val chatManager: AiChatManager,
    private val fileSystemSkill: LocalFileSystemSkill,
    private val modelDownloader: ModelDownloader,
    private val engine: LiteRtEngine,
    private val settings: GleeSettings,
    private val fileSystem: FileSystem,
    private val appDataDir: Path
) : ViewModel() {

    private val systemMetrics = getSystemMetrics()
    private val _uiState = MutableStateFlow(ChatState(modelConfig = GleeModelConfig(useGpu = false)))

    private val downloadJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    private var streamingJob: kotlinx.coroutines.Job? = null

    private suspend fun getAllModels() = listOf(
        ModelInfo(
            id = "gemma-4-e2b",
            name = getString(Res.string.gemma_4_e2b_name),
            description = getString(Res.string.gemma_4_e2b_desc),
            bestFor = getString(Res.string.gemma_4_e2b_best_for),
            resourceUsage = getString(Res.string.gemma_4_e2b_resource_usage),
            url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            infoUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
            sizeGb = 2.6f,
            isRecommended = true,
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
            url = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
            infoUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm",
            sizeGb = 3.7f,
            supportsThinking = true,
            supportsSkills = true,
            supportsVision = true
        ),
        ModelInfo(
            id = "gemma-3n-e2b",
            name = getString(Res.string.gemma_3n_e2b_name),
            description = getString(Res.string.gemma_3n_e2b_desc),
            bestFor = getString(Res.string.gemma_3n_e2b_best_for),
            resourceUsage = getString(Res.string.gemma_3n_e2b_resource_usage),
            url = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm",
            infoUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm",
            sizeGb = 3.6f,
            supportsThinking = false,
            supportsSkills = true,
            supportsVision = true
        ),
        ModelInfo(
            id = "phi-4",
            name = getString(Res.string.phi_4_name),
            description = getString(Res.string.phi_4_desc),
            bestFor = getString(Res.string.phi_4_best_for),
            resourceUsage = getString(Res.string.phi_4_resource_usage),
            url = "https://huggingface.co/microsoft/phi-4-litertlm/resolve/main/phi4.litertlm",
            infoUrl = "https://huggingface.co/microsoft/phi-4",
            sizeGb = 2.1f,
            supportsThinking = false,
            supportsSkills = false,
            supportsVision = false
        )
    )

    val uiState: StateFlow<ChatState> = combine(
        _uiState,
        chatManager.messages
    ) { state, messages ->
        state.copy(messages = if (state.isPrivateMode) MessageList(emptyList()) else MessageList(messages))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatState(modelConfig = GleeModelConfig(useGpu = false))
    )

    init {
        chatManager.registerSkill(fileSystemSkill)
        viewModelScope.launch {
            while (true) {
                updateMetrics()
                delay(2000)
            }
        }

        viewModelScope.launch {
            val defaultPrompt = getString(Res.string.default_system_prompt)
            chatManager.updateSystemPrompt(defaultPrompt)
            _uiState.update { it.copy(systemPrompt = defaultPrompt) }
        }
        
        viewModelScope.launch {
            settings.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        
        viewModelScope.launch {
            settings.isAdaptiveColorsEnabled.collect { enabled ->
                _uiState.update { it.copy(isAdaptiveColorsEnabled = enabled) }
            }
        }

        initializeModels()
    }

    private fun initializeModels() {
        viewModelScope.launch {
            val allModels = getAllModels()
            val updatedModels = allModels.map { model ->
                val path = appDataDir.resolve("${model.id}.litertlm")
                val exists = withContext(Dispatchers.Default) { fileSystem.exists(path) }
                if (exists) {
                    model.copy(downloadStatus = ModelDownloadStatus.Downloaded)
                } else {
                    model.copy(downloadStatus = ModelDownloadStatus.NotDownloaded)
                }
            }
            
            val firstDownloaded = updatedModels.find { it.downloadStatus == ModelDownloadStatus.Downloaded }
            
            _uiState.update { 
                it.copy(
                    availableModels = updatedModels, 
                    selectedModel = firstDownloaded ?: updatedModels.first() 
                ) 
            }
            
            firstDownloaded?.let { loadModel(it) }
        }
    }

    private fun downloadModel(model: ModelInfo) {
        if (downloadJobs.containsKey(model.id)) return
        val targetPath = appDataDir.resolve("${model.id}.litertlm")
        val job = viewModelScope.launch {
            try {
                modelDownloader.downloadModel(
                    url = model.url, 
                    targetPath = targetPath
                ).collect { status ->
                    when (status) {
                        is DownloadStatus.Progress -> updateModelStatus(model.id, ModelDownloadStatus.Downloading(status.progress))
                        is DownloadStatus.Success -> {
                            updateModelStatus(model.id, ModelDownloadStatus.Downloaded)
                            downloadJobs.remove(model.id)
                            
                            // Load the model if it's the one we currently have "selected" or if nothing is ready
                            val currentState = _uiState.value
                            if (currentState.selectedModel?.id == model.id || !currentState.isModelReady) {
                                _uiState.update { it.copy(selectedModel = it.availableModels.find { m -> m.id == model.id }) }
                                _uiState.value.selectedModel?.let { loadModel(it) }
                            }
                        }
                        is DownloadStatus.Error -> {
                            updateModelStatus(model.id, ModelDownloadStatus.Error(status.message))
                            downloadJobs.remove(model.id)
                        }
                    }
                }
            } catch (e: Exception) {
                updateModelStatus(model.id, ModelDownloadStatus.Error(e.message ?: "Cancelled"))
                downloadJobs.remove(model.id)
            }
        }
        downloadJobs[model.id] = job
    }

    private fun cancelDownload(modelId: String) {
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)
        updateModelStatus(modelId, ModelDownloadStatus.NotDownloaded)
        val targetPath = appDataDir.resolve("${modelId}.litertlm")
        viewModelScope.launch(Dispatchers.Default) { runCatching { fileSystem.delete(targetPath) } }
    }

    private fun deleteModel(model: ModelInfo) {
        val targetPath = appDataDir.resolve("${model.id}.litertlm")
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { 
                fileSystem.delete(targetPath)
                updateModelStatus(model.id, ModelDownloadStatus.NotDownloaded)
                if (_uiState.value.selectedModel?.id == model.id) {
                    _uiState.update { it.copy(isModelReady = false, selectedModel = null) }
                }
            }
        }
    }

    private fun updateModelStatus(id: String, status: ModelDownloadStatus) {
        _uiState.update { state ->
            val updatedList = state.availableModels.map { 
                if (it.id == id) it.copy(downloadStatus = status) else it 
            }
            state.copy(
                availableModels = updatedList,
                selectedModel = if (state.selectedModel?.id == id) state.selectedModel.copy(downloadStatus = status) else state.selectedModel
            )
        }
    }

    private suspend fun loadModel(model: ModelInfo) {
        val path = appDataDir.resolve("${model.id}.litertlm")
        val currentConfig = _uiState.value.modelConfig
        _uiState.update { it.copy(isInitializing = true, isModelReady = false) }
        
        // Status update for UI if needed, though we now have isInitializing
        
        val result = withContext(Dispatchers.Default) {
            engine.loadModel(
                ModelConfig(
                    modelPath = path.toString(),
                    temperature = currentConfig.temperature,
                    topK = currentConfig.topK,
                    useGpu = currentConfig.useGpu
                )
            )
        }
        if (result.isSuccess) {
            _uiState.update { 
                it.copy(
                    isInitializing = false, 
                    isModelReady = true, 
                    metrics = it.metrics.copy(modelName = model.name) 
                ) 
            }
            delay(1000)
            _uiState.update { it.copy(streamingContent = "") }
        } else {
            _uiState.update { it.copy(isInitializing = false, isModelReady = false) }
        }
    }

    private fun updateMetrics() {
        _uiState.update { state ->
            state.copy(
                metrics = state.metrics.copy(
                    ramUsedGb = if (state.isModelReady) systemMetrics.getUsedRamGb() else 0f,
                    ramTotalGb = systemMetrics.getTotalRamGb()
                )
            )
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> _uiState.update { it.copy(currentInput = intent.input) }
            ChatIntent.SendMessage -> sendMessage()
            ChatIntent.ClearChat -> chatManager.clearChat()
            is ChatIntent.SelectSuggestion -> {
                _uiState.update { it.copy(currentInput = intent.suggestion) }
                sendMessage()
            }
            is ChatIntent.PickFile -> {
                val attached = AttachedFile(
                    name = intent.file.name,
                    path = intent.file.path,
                    size = intent.file.getSize() ?: 0L,
                    platformFile = intent.file
                )
                _uiState.update { it.copy(attachedFiles = it.attachedFiles + attached) }
            }
            is ChatIntent.RemoveFile -> _uiState.update { it.copy(attachedFiles = it.attachedFiles.filter { f -> f != intent.file }) }
            is ChatIntent.ToggleSkill -> {
                chatManager.toggleSkill(intent.skillId, intent.enabled)
                _uiState.update { it.copy(activeSkills = it.activeSkills + (intent.skillId to intent.enabled)) }
            }
            is ChatIntent.DownloadModel -> {
                downloadModel(intent.model)
            }
            is ChatIntent.SelectModel -> {
                if (intent.model.downloadStatus == ModelDownloadStatus.Downloaded) {
                    _uiState.update { it.copy(selectedModel = intent.model) }
                    viewModelScope.launch { loadModel(intent.model) }
                }
            }
            is ChatIntent.CancelDownload -> cancelDownload(intent.modelId)
            is ChatIntent.DeleteModel -> {
                _uiState.update { it.copy(modelToDelete = intent.model) }
            }
            ChatIntent.ConfirmDeleteModel -> {
                _uiState.value.modelToDelete?.let { model ->
                    deleteModel(model)
                    _uiState.update { it.copy(modelToDelete = null) }
                }
            }
            ChatIntent.CancelDeleteModel -> {
                _uiState.update { it.copy(modelToDelete = null) }
            }
            is ChatIntent.UpdateHfToken -> {
                _uiState.update { it.copy(hfToken = intent.token) }
            }
            is ChatIntent.UpdateModelConfig -> {
                val oldGpu = _uiState.value.modelConfig.useGpu
                _uiState.update { it.copy(modelConfig = intent.config) }
                // Reload model if GPU toggle changed
                if (oldGpu != intent.config.useGpu) {
                    _uiState.value.selectedModel?.let { model ->
                        if (model.downloadStatus == ModelDownloadStatus.Downloaded) {
                            viewModelScope.launch { loadModel(model) }
                        }
                    }
                }
            }
            is ChatIntent.SetThemeMode -> settings.setThemeMode(intent.mode)
            is ChatIntent.SetAdaptiveColors -> settings.setAdaptiveColorsEnabled(intent.enabled)
            ChatIntent.ImportModel -> { /* TODO */ }
            ChatIntent.TogglePrivateMode -> _uiState.update { it.copy(isPrivateMode = !it.isPrivateMode) }
            
            is ChatIntent.UpdateSystemPrompt -> {
                chatManager.updateSystemPrompt(intent.prompt)
                _uiState.update { it.copy(systemPrompt = intent.prompt) }
            }
            ChatIntent.RestoreDefaultSystemPrompt -> {
                viewModelScope.launch {
                    val default = getString(Res.string.default_system_prompt)
                    chatManager.updateSystemPrompt(default)
                    _uiState.update { it.copy(systemPrompt = default) }
                }
            }
            ChatIntent.ToggleSystemPromptEditor -> _uiState.update { it.copy(showSystemPromptEditor = !it.showSystemPromptEditor) }
            ChatIntent.StopStreaming -> stopStreaming()
        }
    }

    private fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        val content = _uiState.value.streamingContent
        if (content.isNotEmpty()) {
            chatManager.commitAssistantMessage(content)
        }
        // Force immediate reset and small delay to let UI dispatcher process
        _uiState.update { it.copy(isStreaming = false, streamingContent = "") }
    }

    private fun sendMessage() {
        val text = _uiState.value.currentInput.trim()
        if (text.isEmpty() && _uiState.value.attachedFiles.isEmpty()) return
        if (_uiState.value.isStreaming || !_uiState.value.isModelReady) return

        val filesContext = _uiState.value.attachedFiles.joinToString("\n") { "[File: ${it.name}]" }
        val fullPrompt = if (filesContext.isNotEmpty()) "$filesContext\n$text" else text

        _uiState.update { it.copy(currentInput = "", attachedFiles = emptyList(), isStreaming = true, streamingContent = "") }

        val startTime = currentTimeMillis()
        streamingJob = viewModelScope.launch {
            try {
                var fullResponse = ""
                chatManager.sendMessage(fullPrompt).collect { chunk ->
                    val latency = currentTimeMillis() - startTime
                    fullResponse += chunk.text
                    _uiState.update { 
                        it.copy(
                            streamingContent = fullResponse,
                            isStreaming = !chunk.isFinal,
                            metrics = it.metrics.copy(
                                latencyMs = latency,
                                contextUsed = it.metrics.contextUsed + 1
                            )
                        )
                    }
                    if (chunk.isFinal) {
                        chatManager.commitAssistantMessage(fullResponse)
                        _uiState.update { 
                            it.copy(
                                streamingContent = "", 
                                isStreaming = false 
                            ) 
                        }
                        streamingJob = null
                    }
                }
            } catch (e: CancellationException) {
                // Handled in stopStreaming()
            } catch (e: Exception) {
                _uiState.update { it.copy(isStreaming = false, streamingContent = "Error: ${e.message}") }
            } finally {
                streamingJob = null
            }
        }
    }
}
