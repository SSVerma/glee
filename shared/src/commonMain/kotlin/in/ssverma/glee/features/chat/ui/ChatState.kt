package `in`.ssverma.glee.features.chat.ui

import androidx.compose.runtime.Immutable
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ChatMetrics
import `in`.ssverma.glee.features.chat.domain.model.Conversation
import `in`.ssverma.glee.features.chat.domain.model.GleeModelConfig
import `in`.ssverma.glee.features.chat.domain.model.MessageList
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import `in`.ssverma.glee.features.chat.domain.model.ThemeMode
import io.github.vinceglb.filekit.core.PlatformFile

@Immutable
data class ChatState(
    val messages: MessageList = MessageList(emptyList()),
    val currentInput: String = "",
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val metrics: ChatMetrics = ChatMetrics(),
    val suggestions: List<String> = emptyList(),
    val attachedFiles: List<AttachedFile> = emptyList(),
    val isModelReady: Boolean = false,
    val isInitializing: Boolean = false,
    val hfToken: String = "",
    val activeSkills: Map<String, Boolean> = emptyMap(),
    val availableModels: List<ModelInfo> = emptyList(),
    val selectedModel: ModelInfo? = null,
    val modelConfig: GleeModelConfig = GleeModelConfig(),
    val themeMode: ThemeMode = ThemeMode.System,
    val isAdaptiveColorsEnabled: Boolean = true,
    val isPrivateMode: Boolean = false,
    val shouldShowIncognitoInfo: Boolean = true,
    
    // Conversations
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: String? = null,
    val conversationToDelete: Conversation? = null,
    
    // UI state
    val showSystemPromptEditor: Boolean = false,
    val showIncognitoInfoDialog: Boolean = false,
    val showCancelDownloadDialog: Boolean = false,
    val modelToCancelDownloadId: String? = null,
    val systemPrompt: String = "",
    val modelToDelete: ModelInfo? = null,
    val isSpeechRecognitionSupported: Boolean = false,
    val isRecordingVoice: Boolean = false,
    val isImporting: Boolean = false,
    val importProgress: Float = 0f,
    val importError: String? = null,
    val loadError: String? = null
)

sealed interface ChatIntent {
    data class UpdateInput(val input: String) : ChatIntent
    data object SendMessage : ChatIntent
    data object StopStreaming : ChatIntent
    data object ToggleVoiceRecording : ChatIntent
    data object ClearChat : ChatIntent
    data class SelectSuggestion(val suggestion: String) : ChatIntent
    data class PickFile(val file: PlatformFile) : ChatIntent
    data class RemoveFile(val file: AttachedFile) : ChatIntent
    data class ToggleSkill(val skillId: String, val enabled: Boolean) : ChatIntent
    data class DownloadModel(val model: ModelInfo) : ChatIntent
    data class SelectModel(val model: ModelInfo) : ChatIntent
    data class CancelDownload(val modelId: String) : ChatIntent
    data object ConfirmCancelDownload : ChatIntent
    data object DismissCancelDownload : ChatIntent
    data class DeleteModel(val model: ModelInfo) : ChatIntent
    data object ConfirmDeleteModel : ChatIntent
    data object CancelDeleteModel : ChatIntent
    data class UpdateHfToken(val token: String) : ChatIntent
    data class UpdateModelConfig(val config: GleeModelConfig) : ChatIntent
    data class SetThemeMode(val mode: ThemeMode) : ChatIntent
    data class SetAdaptiveColors(val enabled: Boolean) : ChatIntent
    data object ImportModel : ChatIntent
    data class ImportModelFile(val file: PlatformFile) : ChatIntent
    data object CancelImport : ChatIntent
    data object TogglePrivateMode : ChatIntent
    data object DismissIncognitoInfo : ChatIntent
    data class SetShowIncognitoInfo(val show: Boolean) : ChatIntent
    
    // New intents
    data class UpdateSystemPrompt(val prompt: String) : ChatIntent
    data object RestoreDefaultSystemPrompt : ChatIntent
    data object ToggleSystemPromptEditor : ChatIntent
    data object SaveIntelligenceConfig : ChatIntent
    
    data class StartConversation(val conversation: Conversation) : ChatIntent
    data class DeleteConversation(val conversation: Conversation) : ChatIntent
    data object ConfirmDeleteConversation : ChatIntent
    data object CancelDeleteConversation : ChatIntent
    data object NewChat : ChatIntent
}
