package `in`.ssverma.glee.ui.chat

import `in`.ssverma.glee.domain.model.*
import io.github.vinceglb.filekit.core.PlatformFile

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val metrics: ChatMetrics = ChatMetrics(),
    val suggestions: List<String> = emptyList(),
    val attachedFiles: List<AttachedFile> = emptyList(),
    val isModelReady: Boolean = false,
    val hfToken: String = "",
    val activeSkills: Map<String, Boolean> = mapOf(
        "local_file_system" to true,
        "web_search" to false,
        "code_execution" to false
    ),
    val availableModels: List<ModelInfo> = emptyList(),
    val selectedModel: ModelInfo? = null,
    val modelConfig: GleeModelConfig = GleeModelConfig(),
    val themeMode: ThemeMode = ThemeMode.System,
    val isAdaptiveColorsEnabled: Boolean = true,
    val isPrivateMode: Boolean = false,
    
    // UI state for dialogs
    val pendingModelDownload: ModelInfo? = null,
    val showSystemPromptEditor: Boolean = false,
    val systemPrompt: String = "You are Glee, a short, crisp, and on-point AI assistant. Keep your answers concise."
)

sealed interface ChatIntent {
    data class UpdateInput(val input: String) : ChatIntent
    data object SendMessage : ChatIntent
    data object ClearChat : ChatIntent
    data class SelectSuggestion(val suggestion: String) : ChatIntent
    data class PickFile(val file: PlatformFile) : ChatIntent
    data class RemoveFile(val file: AttachedFile) : ChatIntent
    data class ToggleSkill(val skillId: String, val enabled: Boolean) : ChatIntent
    data class DownloadModel(val model: ModelInfo) : ChatIntent
    data class SelectModel(val model: ModelInfo) : ChatIntent
    data class CancelDownload(val model: ModelInfo) : ChatIntent
    data class DeleteModel(val model: ModelInfo) : ChatIntent
    data class UpdateHfToken(val token: String) : ChatIntent
    data class UpdateModelConfig(val config: GleeModelConfig) : ChatIntent
    data class SetThemeMode(val mode: ThemeMode) : ChatIntent
    data class SetAdaptiveColors(val enabled: Boolean) : ChatIntent
    data object ImportModel : ChatIntent
    data object TogglePrivateMode : ChatIntent
    
    // New intents
    data class ConfirmDownload(val model: ModelInfo) : ChatIntent
    data object CancelPendingDownload : ChatIntent
    data class UpdateSystemPrompt(val prompt: String) : ChatIntent
    data object RestoreDefaultSystemPrompt : ChatIntent
    data object ToggleSystemPromptEditor : ChatIntent
    data object StopStreaming : ChatIntent
}
