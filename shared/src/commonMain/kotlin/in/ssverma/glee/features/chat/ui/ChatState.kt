package `in`.ssverma.glee.features.chat.ui

import androidx.compose.runtime.Immutable
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ChatMetrics
import `in`.ssverma.glee.features.chat.domain.model.Conversation
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
    val selectedModel: ModelInfo? = null,
    val themeMode: ThemeMode = ThemeMode.System,
    val isAdaptiveColorsEnabled: Boolean = true,
    val isPrivateMode: Boolean = false,
    val shouldShowIncognitoInfo: Boolean = true,
    val loadError: String? = null,

    // Conversations
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: String? = null,
    val conversationToDelete: Conversation? = null,
    
    // UI state
    val showIncognitoInfoDialog: Boolean = false,
    val isSpeechRecognitionSupported: Boolean = false,
    val isRecordingVoice: Boolean = false,
    val showDownloadDialog: Boolean = false,
    val isLowConstraintDevice: Boolean = false
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
    
    data object TogglePrivateMode : ChatIntent
    data object DismissIncognitoInfo : ChatIntent
    data class SetShowIncognitoInfo(val show: Boolean) : ChatIntent
    
    data class StartConversation(val conversation: Conversation) : ChatIntent
    data class DeleteConversation(val conversation: Conversation) : ChatIntent
    data object ConfirmDeleteConversation : ChatIntent
    data object CancelDeleteConversation : ChatIntent
    data object NewChat : ChatIntent
    data class SetShowDownloadDialog(val show: Boolean) : ChatIntent
}
