package `in`.ssverma.glee.features.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.default_system_prompt
import glee.shared.generated.resources.describe_image
import glee.shared.generated.resources.gemma_4_e2b_best_for
import glee.shared.generated.resources.gemma_4_e2b_desc
import glee.shared.generated.resources.gemma_4_e2b_name
import glee.shared.generated.resources.gemma_4_e2b_resource_usage
import glee.shared.generated.resources.gemma_4_e4b_best_for
import glee.shared.generated.resources.gemma_4_e4b_desc
import glee.shared.generated.resources.gemma_4_e4b_name
import glee.shared.generated.resources.gemma_4_e4b_resource_usage
import glee.shared.generated.resources.suggestion_book
import glee.shared.generated.resources.suggestion_budget
import glee.shared.generated.resources.suggestion_cleaning
import glee.shared.generated.resources.suggestion_coding
import glee.shared.generated.resources.suggestion_cooking
import glee.shared.generated.resources.suggestion_email
import glee.shared.generated.resources.suggestion_gift
import glee.shared.generated.resources.suggestion_history
import glee.shared.generated.resources.suggestion_joke
import glee.shared.generated.resources.suggestion_language
import glee.shared.generated.resources.suggestion_meditation
import glee.shared.generated.resources.suggestion_productivity
import glee.shared.generated.resources.suggestion_recipe
import glee.shared.generated.resources.suggestion_travel
import glee.shared.generated.resources.suggestion_trip
import glee.shared.generated.resources.suggestion_workout
import `in`.ssverma.glee.core.common.currentTimeMillis
import `in`.ssverma.glee.core.common.platform.GleeFileSystem
import `in`.ssverma.glee.core.common.platform.SpeechRecognizerManager
import `in`.ssverma.glee.core.common.platform.getSystemMetrics
import `in`.ssverma.glee.core.common.platform.toCoilPath
import `in`.ssverma.glee.core.preferences.GleeSettings
import `in`.ssverma.glee.domain.AiEngine
import `in`.ssverma.glee.features.chat.data.remote.DownloadStatus
import `in`.ssverma.glee.features.chat.data.remote.ModelDownloader
import `in`.ssverma.glee.features.chat.domain.model.AiSkill
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.GleeModelConfig
import `in`.ssverma.glee.features.chat.domain.model.MessageList
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import `in`.ssverma.glee.features.chat.domain.model.ModelDownloadStatus
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import `in`.ssverma.glee.features.chat.domain.usecase.AiChatManager
import kotlinx.coroutines.CancellationException
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
import org.jetbrains.compose.resources.getString

class ChatViewModel(
    private val chatManager: AiChatManager,
    private val skills: List<AiSkill>,
    private val modelDownloader: ModelDownloader,
    private val engine: AiEngine,
    private val settings: GleeSettings,
    private val fileSystem: GleeFileSystem,
    private val appDataDir: Path,
    private val speechRecognizerManager: SpeechRecognizerManager
) : ViewModel() {

    private val systemMetrics = getSystemMetrics()
    private val _uiState =
        MutableStateFlow(ChatState(modelConfig = GleeModelConfig(useGpu = false)))

    private val downloadJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    private var streamingJob: kotlinx.coroutines.Job? = null
    private var voiceRecordingJob: kotlinx.coroutines.Job? = null

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
        )
    )

    val uiState: StateFlow<ChatState> = combine(
        _uiState,
        chatManager.messages,
        chatManager.conversations
    ) { state, messages, conversations ->
        state.copy(
            messages = MessageList(messages),
            conversations = conversations
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatState(modelConfig = GleeModelConfig(useGpu = false))
    )

    init {
        val initialSkills = skills.associate { it.id to true }
        _uiState.update {
            it.copy(
                activeSkills = initialSkills,
                isSpeechRecognitionSupported = speechRecognizerManager.isSupported
            )
        }

        skills.forEach { chatManager.registerSkill(it) }
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

        viewModelScope.launch {
            settings.shouldShowIncognitoInfo.collect { shouldShow ->
                _uiState.update { it.copy(shouldShowIncognitoInfo = shouldShow) }
            }
        }

        viewModelScope.launch {
            combine(
                settings.systemPrompt,
                settings.temperature,
                settings.topK,
                settings.useGpu,
                settings.isAgentic
            ) { systemPrompt, temp, topK, useGpu, isAgentic ->
                val defaultPrompt = getString(Res.string.default_system_prompt)
                _uiState.update {
                    it.copy(
                        systemPrompt = systemPrompt ?: defaultPrompt,
                        modelConfig = it.modelConfig.copy(
                            temperature = temp,
                            topK = topK,
                            useGpu = useGpu,
                            isAgentic = isAgentic
                        )
                    )
                }
                chatManager.updateSystemPrompt(systemPrompt ?: defaultPrompt)
            }.collect {}
        }

        viewModelScope.launch {
            chatManager.loadConversations()
        }

        refreshSuggestions()
        initializeModels()
    }

    private fun refreshSuggestions() {
        viewModelScope.launch {
            val allSuggestions = listOf(
                Res.string.suggestion_trip,
                Res.string.suggestion_recipe,
                Res.string.suggestion_email,
                Res.string.suggestion_workout,
                Res.string.suggestion_book,
                Res.string.suggestion_gift,
                Res.string.suggestion_productivity,
                Res.string.suggestion_coding,
                Res.string.suggestion_history,
                Res.string.suggestion_cooking,
                Res.string.suggestion_travel,
                Res.string.suggestion_language,
                Res.string.suggestion_joke,
                Res.string.suggestion_meditation,
                Res.string.suggestion_budget,
                Res.string.suggestion_cleaning
            ).shuffled().take(4)

            val suggestions = allSuggestions.map { getString(it) }
            _uiState.update { it.copy(suggestions = suggestions) }
        }
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

            val firstDownloaded =
                updatedModels.find { it.downloadStatus == ModelDownloadStatus.Downloaded }

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
                        is DownloadStatus.Progress -> updateModelStatus(
                            model.id,
                            ModelDownloadStatus.Downloading(status.progress)
                        )

                        is DownloadStatus.Success -> {
                            updateModelStatus(model.id, ModelDownloadStatus.Downloaded)
                            downloadJobs.remove(model.id)

                            // Immediately load the newly downloaded model to enable the chat box
                            _uiState.update { it.copy(selectedModel = it.availableModels.find { m -> m.id == model.id }) }
                            val newSelected = _uiState.value.selectedModel
                            if (newSelected != null) {
                                viewModelScope.launch { loadModel(newSelected) }
                            }
                        }

                        is DownloadStatus.Error -> {
                            updateModelStatus(model.id, ModelDownloadStatus.Error(status.message))
                            downloadJobs.remove(model.id)
                        }
                    }
                }
            } catch (e: CancellationException) {
                updateModelStatus(model.id, ModelDownloadStatus.NotDownloaded)
                downloadJobs.remove(model.id)
            } catch (e: Exception) {
                updateModelStatus(model.id, ModelDownloadStatus.Error(e.message ?: "Unknown error"))
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

                val anyLeft = getAllModels().any {
                    val p = appDataDir.resolve("${it.id}.litertlm")
                    fileSystem.exists(p)
                }

                if (!anyLeft || _uiState.value.selectedModel?.id == model.id) {
                    _uiState.update {
                        it.copy(
                            isModelReady = false,
                            selectedModel = null,
                            currentConversationId = null
                        )
                    }
                    chatManager.clearChat()
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
                selectedModel = if (state.selectedModel?.id == id) state.selectedModel.copy(
                    downloadStatus = status
                ) else state.selectedModel
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
                    useGpu = currentConfig.useGpu,
                    maxNumImages = if (model.supportsVision) 1 else 0
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

    fun getSkills(): List<AiSkill> = skills

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> _uiState.update { it.copy(currentInput = intent.input) }
            ChatIntent.SendMessage -> sendMessage()
            ChatIntent.ClearChat -> {
                stopStreaming()
                viewModelScope.launch { chatManager.clearChat() }
                _uiState.update { it.copy(currentConversationId = null) }
                refreshSuggestions()
            }

            ChatIntent.NewChat -> {
                stopStreaming()
                viewModelScope.launch { chatManager.clearChat() }
                _uiState.update { it.copy(currentConversationId = null) }
                refreshSuggestions()
            }

            is ChatIntent.StartConversation -> {
                stopStreaming()
                _uiState.update { it.copy(isPrivateMode = false) }
                viewModelScope.launch {
                    chatManager.startConversation(intent.conversation)
                    _uiState.update { it.copy(currentConversationId = intent.conversation.id) }
                }
            }

            is ChatIntent.DeleteConversation -> {
                _uiState.update { it.copy(conversationToDelete = intent.conversation) }
            }

            ChatIntent.ConfirmDeleteConversation -> {
                _uiState.value.conversationToDelete?.let { conv ->
                    viewModelScope.launch {
                        chatManager.deleteConversation(conv.id)
                        if (_uiState.value.currentConversationId == conv.id) {
                            _uiState.update { it.copy(currentConversationId = null) }
                        }
                        _uiState.update { it.copy(conversationToDelete = null) }
                    }
                }
            }

            ChatIntent.CancelDeleteConversation -> {
                _uiState.update { it.copy(conversationToDelete = null) }
            }

            is ChatIntent.SelectSuggestion -> {
                _uiState.update { it.copy(currentInput = intent.suggestion) }
                sendMessage()
            }

            is ChatIntent.PickFile -> {
                val attached = AttachedFile(
                    name = intent.file.name,
                    path = intent.file.toCoilPath(),
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
                if (intent.model.id == _uiState.value.selectedModel?.id && _uiState.value.isModelReady) {
                    // Already loaded
                    return
                }
                if (intent.model.downloadStatus == ModelDownloadStatus.Downloaded) {
                    _uiState.update { it.copy(selectedModel = intent.model) }
                    viewModelScope.launch { loadModel(intent.model) }
                }
            }

            is ChatIntent.CancelDownload -> {
                _uiState.update {
                    it.copy(
                        showCancelDownloadDialog = true,
                        modelToCancelDownloadId = intent.modelId
                    )
                }
            }

            ChatIntent.ConfirmCancelDownload -> {
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

            ChatIntent.DismissCancelDownload -> {
                _uiState.update {
                    it.copy(
                        showCancelDownloadDialog = false,
                        modelToCancelDownloadId = null
                    )
                }
            }

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
                _uiState.update { it.copy(modelConfig = intent.config) }
            }

            is ChatIntent.SetThemeMode -> settings.setThemeMode(intent.mode)
            is ChatIntent.SetAdaptiveColors -> settings.setAdaptiveColorsEnabled(intent.enabled)
            ChatIntent.ImportModel -> { /* TODO */
            }

            ChatIntent.TogglePrivateMode -> {
                val isCurrentlyPrivate = _uiState.value.isPrivateMode
                if (!isCurrentlyPrivate && _uiState.value.shouldShowIncognitoInfo) {
                    _uiState.update { it.copy(showIncognitoInfoDialog = true) }
                } else {
                    stopStreaming()
                    _uiState.update { it.copy(isPrivateMode = !isCurrentlyPrivate) }
                    if (!isCurrentlyPrivate) {
                        viewModelScope.launch { chatManager.clearChat() }
                        _uiState.update { it.copy(currentConversationId = null) }
                    }
                }
            }

            ChatIntent.DismissIncognitoInfo -> {
                stopStreaming()
                _uiState.update {
                    it.copy(
                        showIncognitoInfoDialog = false,
                        isPrivateMode = true
                    )
                }
                viewModelScope.launch { chatManager.clearChat() }
                _uiState.update { it.copy(currentConversationId = null) }
            }

            is ChatIntent.SetShowIncognitoInfo -> {
                settings.setShouldShowIncognitoInfo(intent.show)
            }

            is ChatIntent.UpdateSystemPrompt -> {
                _uiState.update { it.copy(systemPrompt = intent.prompt) }
            }

            ChatIntent.SaveIntelligenceConfig -> {
                val state = _uiState.value
                val previousUseGpu = _uiState.value.modelConfig.useGpu

                settings.setSystemPrompt(state.systemPrompt)
                settings.setTemperature(state.modelConfig.temperature)
                settings.setTopK(state.modelConfig.topK)
                settings.setUseGpu(state.modelConfig.useGpu)
                settings.setIsAgentic(state.modelConfig.isAgentic)
                chatManager.updateSystemPrompt(state.systemPrompt)

                // Only reload model if engine-level config (like GPU) changed
                if (previousUseGpu != state.modelConfig.useGpu) {
                    state.selectedModel?.let { model ->
                        if (model.downloadStatus == ModelDownloadStatus.Downloaded) {
                            viewModelScope.launch { loadModel(model) }
                        }
                    }
                }
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
            ChatIntent.ToggleVoiceRecording -> toggleVoiceRecording()
        }
    }

    private fun toggleVoiceRecording() {
        if (_uiState.value.isRecordingVoice) {
            speechRecognizerManager.stopListening()
            voiceRecordingJob?.cancel()
            voiceRecordingJob = null
            _uiState.update { it.copy(isRecordingVoice = false) }
        } else {
            _uiState.update { it.copy(isRecordingVoice = true) }
            voiceRecordingJob = viewModelScope.launch {
                try {
                    speechRecognizerManager.startListening().collect { text ->
                        if (text.isNotBlank()) {
                            _uiState.update {
                                val current = it.currentInput
                                val sep =
                                    if (current.isNotEmpty() && !current.endsWith(" ")) " " else ""
                                it.copy(currentInput = current + sep + text)
                            }
                        }
                    }
                } finally {
                    _uiState.update { it.copy(isRecordingVoice = false) }
                }
            }
        }
    }

    private fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        val content = _uiState.value.streamingContent
        if (content.isNotEmpty()) {
            viewModelScope.launch {
                chatManager.commitAssistantMessage(content, _uiState.value.isPrivateMode)
            }
        }
        // Force immediate reset and small delay to let UI dispatcher process
        _uiState.update { it.copy(isStreaming = false, streamingContent = "") }
    }

    private fun sendMessage() {
        val currentState = _uiState.value
        val text = currentState.currentInput.trim()
        val attachedFiles = currentState.attachedFiles

        if (text.isEmpty() && attachedFiles.isEmpty()) return
        if (currentState.isStreaming || !currentState.isModelReady) return

        val selectedModel = currentState.selectedModel
        val supportsVision = selectedModel?.supportsVision == true

        // Only include files if the model supports vision, but keep them for UI/History
        val filesForEngine = if (supportsVision) attachedFiles else emptyList()

        val fullPrompt = text

        val isPrivate = currentState.isPrivateMode
        val modelId = selectedModel?.id ?: ""
        val isAgentic = currentState.modelConfig.isAgentic

        _uiState.update {
            it.copy(
                currentInput = "",
                attachedFiles = emptyList(),
                isStreaming = true,
                streamingContent = ""
            )
        }

        val startTime = currentTimeMillis()
        streamingJob = viewModelScope.launch {
            try {
                val uiPrompt = text
                val enginePrompt = text.ifEmpty { getString(Res.string.describe_image) }

                var fullResponse = ""
                // Pass attachedFiles to chatManager so they are saved in history, 
                // but pass filesForEngine to ensure only supported models get the bytes.
                chatManager.sendMessage(
                    uiPrompt = uiPrompt,
                    enginePrompt = enginePrompt,
                    modelId = modelId,
                    isPrivate = isPrivate,
                    isAgentic = isAgentic,
                    files = filesForEngine,
                    historyFiles = attachedFiles
                ).collect { chunk ->
                    val latency = currentTimeMillis() - startTime
                    fullResponse += chunk.text

                    // Estimate tokens: roughly 1 token per 4 chars for English, plus prompt overhead
                    val estimatedTokens = (fullResponse.length / 4) + (fullPrompt.length / 4)

                    _uiState.update {
                        it.copy(
                            streamingContent = fullResponse,
                            isStreaming = !chunk.isFinal,
                            metrics = it.metrics.copy(
                                latencyMs = latency,
                                contextUsed = estimatedTokens
                            )
                        )
                    }
                    if (chunk.isFinal) {
                        chatManager.commitAssistantMessage(fullResponse, isPrivate)
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
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        streamingContent = "Error: ${e.message}"
                    )
                }
            } finally {
                streamingJob = null
            }
        }
    }
}
