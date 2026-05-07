package `in`.ssverma.glee.features.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.default_system_prompt
import `in`.ssverma.glee.core.common.platform.SpeechRecognizerManager
import `in`.ssverma.glee.core.common.platform.SystemMetrics
import `in`.ssverma.glee.core.common.platform.toCoilPath
import `in`.ssverma.glee.core.preferences.GleeSettings
import `in`.ssverma.glee.domain.AiEngine
import `in`.ssverma.glee.features.chat.data.repository.AiModelRepository
import `in`.ssverma.glee.features.chat.domain.ChatSuggestionProvider
import `in`.ssverma.glee.features.chat.domain.model.AiTool
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.BackendType
import `in`.ssverma.glee.features.chat.domain.model.MessageList
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import `in`.ssverma.glee.features.chat.domain.model.ModelDownloadStatus
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import `in`.ssverma.glee.features.chat.domain.usecase.AiChatManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.cancellation.CancellationException

class ChatViewModel(
    private val chatManager: AiChatManager,
    private val skills: List<AiTool>,
    private val engine: AiEngine,
    private val settings: GleeSettings,
    private val appDataDir: Path,
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val modelRepository: AiModelRepository,
    private val suggestionProvider: ChatSuggestionProvider
) : ViewModel() {

    private val systemMetrics = object : SystemMetrics {
        override fun getUsedRamGb(): Float = 0f
        override fun getTotalRamGb(): Float = 0f
    }

    private val _uiState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null
    private var voiceRecordingJob: Job? = null

    private var currentSystemPrompt: String = ""
    private var currentTemperature: Float = 0.7f
    private var currentTopK: Int = 40
    private var currentBackend: BackendType = BackendType.Auto
    private var isAgentic: Boolean = false

    init {
        observeChatManager()
        observeSettings()
        observeSelectedModel()
        refreshSuggestions()

        _uiState.update { it.copy(isSpeechRecognitionSupported = speechRecognizerManager.isSupported) }

        viewModelScope.launch {
            while (true) {
                updateMetrics()
                delay(2000)
            }
        }

        // Skills registration
        skills.forEach { chatManager.registerSkill(it) }
    }

    private fun observeChatManager() {
        viewModelScope.launch {
            chatManager.messages.collect { messages ->
                _uiState.update { it.copy(messages = MessageList(messages)) }
            }
        }
        viewModelScope.launch {
            chatManager.conversations.collect { list ->
                _uiState.update { it.copy(conversations = list) }
            }
        }
    }

    private fun observeSettings() {
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
                settings.preferredBackend,
                settings.isAgentic
            ) { systemPrompt, temp, topK, backend, agentic ->
                val defaultPrompt = getString(Res.string.default_system_prompt)
                currentSystemPrompt = systemPrompt ?: defaultPrompt
                currentTemperature = temp
                currentTopK = topK

                val preferredBackend =
                    runCatching { BackendType.valueOf(backend) }.getOrDefault(BackendType.Auto)
                val backendChanged = currentBackend != preferredBackend
                currentBackend = preferredBackend
                isAgentic = agentic

                chatManager.updateSystemPrompt(currentSystemPrompt)

                if (backendChanged) {
                    _uiState.value.selectedModel?.let { model ->
                        if (model.downloadStatus == ModelDownloadStatus.Downloaded) {
                            loadModel(model)
                        }
                    }
                }
            }.collect {}
        }
    }

    private fun observeSelectedModel() {
        viewModelScope.launch {
            combine(
                settings.selectedModelId.distinctUntilChanged(),
                modelRepository.modelsChanged
            ) { id, _ ->
                val models = modelRepository.getModelsWithStatus()
                val model = models.find { it.id == id }

                // 1. If an ID is set but the model is missing (deleted), clear the setting
                if (id != null && model == null) {
                    settings.setSelectedModelId(null)
                    return@combine
                }

                // 2. Auto-select first available if none selected and NO ID is present
                if (model == null && id == null) {
                    val firstAvailable =
                        models.find { it.downloadStatus == ModelDownloadStatus.Downloaded }
                    if (firstAvailable != null) {
                        settings.setSelectedModelId(firstAvailable.id)
                        return@combine
                    }
                }

                // 3. Handle model loading or engine cleanup
                if (model != null && model.downloadStatus == ModelDownloadStatus.Downloaded) {
                    _uiState.update { it.copy(selectedModel = model, loadError = null) }
                    loadModel(model)
                } else {
                    _uiState.update {
                        it.copy(
                            selectedModel = model,
                            isModelReady = false,
                            loadError = if (id != null && model == null) "Selected model not found" else null
                        )
                    }
                    // Explicitly close engine to free RAM and release file handles
                    engine.close()
                }
            }.collect {}
        }
    }

    private fun refreshSuggestions() {
        viewModelScope.launch {
            val suggestions = suggestionProvider.getSuggestions()
            _uiState.update { it.copy(suggestions = suggestions) }
        }
    }

    private suspend fun loadModel(model: ModelInfo) {
        val extension = if (model.isCustom) model.url else ".litertlm"
        val fileName = "${model.id}$extension"
        val path = appDataDir.resolve(fileName)

        _uiState.update { it.copy(isInitializing = true, isModelReady = false, loadError = null) }

        try {
            val result = withContext(Dispatchers.Default) {
                engine.loadModel(
                    ModelConfig(
                        modelPath = path.toString(),
                        temperature = currentTemperature,
                        topK = currentTopK,
                        preferredBackend = currentBackend,
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
                val error = result.exceptionOrNull()
                println("[Glee] Failed to load model: ${error?.message}")
                error?.printStackTrace()
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        isModelReady = false,
                        loadError = error?.message ?: "Unknown error while loading model"
                    )
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            println("[Glee] Exception during loadModel: ${e.message}")
            e.printStackTrace()
            _uiState.update {
                it.copy(
                    isInitializing = false,
                    isModelReady = false,
                    loadError = e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun updateMetrics() {
        _uiState.update { state ->
            val historyTokens = chatManager.messages.value.sumOf { (it.content.length / 4) + 1 }
            val systemTokens = currentSystemPrompt.length / 4

            state.copy(
                metrics = state.metrics.copy(
                    ramUsedGb = if (state.isModelReady) systemMetrics.getUsedRamGb() else 0f,
                    ramTotalGb = systemMetrics.getTotalRamGb(),
                    contextUsed = historyTokens + systemTokens,
                    contextMax = 128000
                )
            )
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> _uiState.update { it.copy(currentInput = intent.input) }
            ChatIntent.SendMessage -> sendMessage()
            ChatIntent.ClearChat -> {
                stopStreaming()
                _uiState.update { it.copy(currentConversationId = null) }
                viewModelScope.launch {
                    chatManager.clearChat()
                    refreshSuggestions()
                }
            }

            ChatIntent.NewChat -> {
                stopStreaming()
                _uiState.update { it.copy(currentConversationId = null, isPrivateMode = false) }
                viewModelScope.launch {
                    chatManager.clearChat()
                    refreshSuggestions()
                }
            }

            is ChatIntent.StartConversation -> {
                stopStreaming()
                _uiState.update {
                    it.copy(
                        isPrivateMode = false,
                        currentConversationId = intent.conversation.id
                    )
                }
                viewModelScope.launch {
                    chatManager.startConversation(intent.conversation)
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

            ChatIntent.StopStreaming -> stopStreaming()
            ChatIntent.ToggleVoiceRecording -> toggleVoiceRecording()
            is ChatIntent.SetShowDownloadDialog -> _uiState.update { it.copy(showDownloadDialog = intent.show) }
        }
    }

    private fun toggleVoiceRecording() {
        if (_uiState.value.isRecordingVoice) {
            voiceRecordingJob?.cancel()
            _uiState.update { it.copy(isRecordingVoice = false) }
            speechRecognizerManager.stopListening()
        } else {
            _uiState.update { it.copy(isRecordingVoice = true) }
            voiceRecordingJob = viewModelScope.launch {
                speechRecognizerManager.startListening().collect { text ->
                    _uiState.update { it.copy(currentInput = it.currentInput + " " + text) }
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
                chatManager.commitAssistantMessage(
                    content = content,
                    isPrivate = _uiState.value.isPrivateMode
                )
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        streamingContent = ""
                    )
                }
            }
        } else {
            _uiState.update { it.copy(isStreaming = false) }
        }
    }

    private fun sendMessage() {
        val input = _uiState.value.currentInput
        val attachedFiles = _uiState.value.attachedFiles
        if (input.isBlank() && attachedFiles.isEmpty()) return

        stopStreaming()

        val isPrivate = _uiState.value.isPrivateMode
        val modelId = _uiState.value.selectedModel?.id ?: "unknown"

        _uiState.update {
            it.copy(
                currentInput = "",
                attachedFiles = emptyList(),
                isStreaming = true,
                streamingContent = ""
            )
        }

        streamingJob = viewModelScope.launch {
            val historyFiles = attachedFiles.map { it.copy() }

            chatManager.sendMessage(
                uiPrompt = input,
                enginePrompt = input,
                modelId = modelId,
                isPrivate = isPrivate,
                isAgentic = isAgentic,
                files = attachedFiles,
                historyFiles = historyFiles
            ).catch { e ->
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        streamingContent = "Error: ${e.message}"
                    )
                }
            }.collect { chunk ->
                if (chunk.isFinal) {
                    chatManager.commitAssistantMessage(
                        content = _uiState.value.streamingContent,
                        isPrivate = isPrivate
                    )
                    _uiState.update {
                        it.copy(
                            isStreaming = false,
                            streamingContent = ""
                        )
                    }
                } else {
                    _uiState.update { it.copy(streamingContent = it.streamingContent + chunk.text) }
                }
            }
        }
    }
}
