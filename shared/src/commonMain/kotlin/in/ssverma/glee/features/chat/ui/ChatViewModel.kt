package `in`.ssverma.glee.features.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.default_system_prompt
import glee.shared.generated.resources.describe_image
import `in`.ssverma.glee.core.common.currentTimeMillis
import `in`.ssverma.glee.core.common.platform.PlatformType
import `in`.ssverma.glee.core.common.platform.SpeechRecognizerManager
import `in`.ssverma.glee.core.common.platform.getPlatformType
import `in`.ssverma.glee.core.common.platform.getSystemMetrics
import `in`.ssverma.glee.core.common.platform.toCoilPath
import `in`.ssverma.glee.core.preferences.GleeSettings
import `in`.ssverma.glee.domain.AiEngine
import `in`.ssverma.glee.features.chat.data.repository.AiModelRepository
import `in`.ssverma.glee.features.chat.domain.ChatSuggestionProvider
import `in`.ssverma.glee.features.chat.domain.model.AiTool
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.MessageList
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import `in`.ssverma.glee.features.chat.domain.model.ModelDownloadStatus
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import `in`.ssverma.glee.features.chat.domain.usecase.AiChatManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import org.jetbrains.compose.resources.getString

class ChatViewModel(
    private val chatManager: AiChatManager,
    skills: List<AiTool>,
    private val engine: AiEngine,
    private val settings: GleeSettings,
    private val appDataDir: Path,
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val modelRepository: AiModelRepository,
    private val suggestionProvider: ChatSuggestionProvider,
) : ViewModel() {

    private val systemMetrics = getSystemMetrics()
    private val _uiState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    private var streamingJob: kotlinx.coroutines.Job? = null
    private var voiceRecordingJob: kotlinx.coroutines.Job? = null

    private var currentSystemPrompt: String = ""
    private var currentTemperature: Float = 0.7f
    private var currentTopK: Int = 40
    private var currentUseGpu: Boolean = false
    private var isAgentic: Boolean = false

    init {
        _uiState.update {
            it.copy(
                isSpeechRecognitionSupported = speechRecognizerManager.isSupported,
                showDownloadDialog = (getPlatformType() == PlatformType.WasmJs) || (getPlatformType() == PlatformType.Js)
            )
        }

        skills.forEach { chatManager.registerSkill(it) }
        
        viewModelScope.launch {
            while (true) {
                updateMetrics()
                delay(2000)
            }
        }

        observeChatManager()
        observeSettings()
        observeSelectedModel()
        
        viewModelScope.launch {
            chatManager.loadConversations()
        }

        refreshSuggestions()
    }

    private fun observeChatManager() {
        viewModelScope.launch {
            chatManager.messages.collect { messages ->
                _uiState.update { it.copy(messages = MessageList(messages)) }
            }
        }
        viewModelScope.launch {
            chatManager.conversations.collect { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
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
                settings.useGpu,
                settings.isAgentic
            ) { systemPrompt, temp, topK, useGpu, agentic ->
                val defaultPrompt = getString(Res.string.default_system_prompt)
                currentSystemPrompt = systemPrompt ?: defaultPrompt
                currentTemperature = temp
                currentTopK = topK
                
                val gpuChanged = currentUseGpu != useGpu
                currentUseGpu = useGpu
                isAgentic = agentic
                
                chatManager.updateSystemPrompt(currentSystemPrompt)
                
                if (gpuChanged) {
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
            settings.selectedModelId
                .distinctUntilChanged()
                .collect { id ->
                    val models = modelRepository.getModelsWithStatus()

                    val model = models.find { it.id == id } ?: return@collect
                    
                    if (model.downloadStatus == ModelDownloadStatus.Downloaded) {
                        _uiState.update { it.copy(selectedModel = model) }
                        loadModel(model)
                    } else {
                        _uiState.update { it.copy(selectedModel = model, isModelReady = false) }
                    }
                }
        }
    }

    private fun refreshSuggestions() {
        viewModelScope.launch {
            val suggestions = suggestionProvider.getSuggestions()
            _uiState.update { it.copy(suggestions = suggestions) }
        }
    }

    private suspend fun loadModel(model: ModelInfo) {
        val path = appDataDir.resolve("${model.id}.litertlm")
        _uiState.update { it.copy(isInitializing = true, isModelReady = false, loadError = null) }

        val result = withContext(Dispatchers.Default) {
            engine.loadModel(
                ModelConfig(
                    modelPath = path.toString(),
                    temperature = currentTemperature,
                    topK = currentTopK,
                    useGpu = currentUseGpu,
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
            _uiState.update {
                it.copy(
                    isInitializing = false,
                    isModelReady = false,
                    loadError = error?.message ?: "Unknown error while loading model"
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
                    contextUsed = historyTokens + systemTokens
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
        val job = streamingJob
        if (job != null) {
            job.cancel()
            streamingJob = null
            
            val content = _uiState.value.streamingContent
            if (content.isNotEmpty()) {
                val isPrivate = _uiState.value.isPrivateMode
                val convId = _uiState.value.currentConversationId
                viewModelScope.launch {
                    chatManager.commitAssistantMessage(content, isPrivate, convId)
                }
            }
        }
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
        val filesForEngine = if (supportsVision) attachedFiles else emptyList()

        val isPrivate = currentState.isPrivateMode
        val modelId = selectedModel?.id ?: ""

        _uiState.update {
            it.copy(
                currentInput = "",
                attachedFiles = emptyList(),
                isStreaming = true,
                streamingContent = ""
            )
        }

        val startTime = currentTimeMillis()
        val job = viewModelScope.launch {
            try {
                val enginePrompt = text.ifEmpty { getString(Res.string.describe_image) }

                var fullResponse = ""
                chatManager.sendMessage(
                    uiPrompt = text,
                    enginePrompt = enginePrompt,
                    modelId = modelId,
                    isPrivate = isPrivate,
                    isAgentic = isAgentic,
                    files = filesForEngine,
                    historyFiles = attachedFiles
                ).collect { chunk ->
                    val latency = currentTimeMillis() - startTime
                    fullResponse += chunk.text

                    val historyTokens =
                        chatManager.messages.value.sumOf { (it.content.length / 4) + 1 }
                    val systemTokens = currentSystemPrompt.length / 4
                    val estimatedTokens =
                        historyTokens + systemTokens + (fullResponse.length / 4) + (text.length / 4)

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
                        if (streamingJob == coroutineContext[Job]) {
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
                }
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                if (streamingJob == coroutineContext[Job]) {
                    _uiState.update {
                        it.copy(
                            isStreaming = false,
                            streamingContent = "Error: ${e.message}"
                        )
                    }
                }
            } finally {
                if (streamingJob == coroutineContext[Job]) {
                    streamingJob = null
                }
            }
        }
        streamingJob = job
    }
}
