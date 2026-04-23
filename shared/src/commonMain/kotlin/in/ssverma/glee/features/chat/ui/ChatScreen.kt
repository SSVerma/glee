@file:OptIn(ExperimentalMaterial3Api::class)

package `in`.ssverma.glee.features.chat.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.benchmark_desc
import glee.shared.generated.resources.cancel
import glee.shared.generated.resources.delete
import glee.shared.generated.resources.delete_conversation_desc
import glee.shared.generated.resources.delete_conversation_title
import glee.shared.generated.resources.delete_model_desc
import glee.shared.generated.resources.delete_model_title
import glee.shared.generated.resources.done
import glee.shared.generated.resources.dont_show_again
import glee.shared.generated.resources.incognito_desc
import glee.shared.generated.resources.incognito_info_title
import glee.shared.generated.resources.model_benchmark
import glee.shared.generated.resources.model_initializing
import glee.shared.generated.resources.please_wait
import glee.shared.generated.resources.start_benchmark
import glee.shared.generated.resources.suggestion_email
import glee.shared.generated.resources.suggestion_recipe
import glee.shared.generated.resources.suggestion_trip
import glee.shared.generated.resources.suggestion_workout
import `in`.ssverma.glee.core.ui.components.GleeLoadingOverlay
import `in`.ssverma.glee.core.ui.components.GleeSidebar
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.MessageList
import `in`.ssverma.glee.features.chat.domain.model.ModelDownloadStatus
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import `in`.ssverma.glee.features.chat.ui.components.ChatActionSheetType
import `in`.ssverma.glee.features.chat.ui.components.ChatInputBar
import `in`.ssverma.glee.features.chat.ui.components.ChatTopBar
import `in`.ssverma.glee.features.chat.ui.components.GleeActionMenuSheet
import `in`.ssverma.glee.features.chat.ui.components.GleeIntelligenceSheet
import `in`.ssverma.glee.features.chat.ui.components.GleePerformanceSheet
import `in`.ssverma.glee.features.chat.ui.components.GleeSkillsSheet
import `in`.ssverma.glee.features.chat.ui.components.MessageBubble
import `in`.ssverma.glee.features.chat.ui.components.ModelSelectionContent
import `in`.ssverma.glee.features.chat.ui.components.StreamingBubble
import `in`.ssverma.glee.features.chat.ui.components.SuggestionChips
import `in`.ssverma.glee.features.chat.ui.components.WelcomeHeader
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel(),
    onModelManagement: () -> Unit = { },
    onManageSkills: () -> Unit = { },
    onSettings: () -> Unit = { },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val reversedMessages = remember(uiState.messages) { 
        MessageList(uiState.messages.asReversed()) 
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showActionSheet by remember { mutableStateOf(false) }
    var currentActionSheet by remember { mutableStateOf(ChatActionSheetType.Root) }

    var showModelSelectionSheet by remember { mutableStateOf(false) }
    var showBenchmarkDialog by remember { mutableStateOf(false) }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isWide = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    val isMedium = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM

    val launcher = rememberFilePickerLauncher(
        type = PickerType.File(),
        mode = PickerMode.Single
    ) { file ->
        file?.let { viewModel.onIntent(ChatIntent.PickFile(it)) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                GleeSidebar(
                    conversations = uiState.conversations,
                    selectedConversationId = uiState.currentConversationId,
                    onConversationClick = {
                        viewModel.onIntent(ChatIntent.StartConversation(it))
                        scope.launch { drawerState.close() }
                    },
                    onDeleteConversation = {
                        viewModel.onIntent(ChatIntent.DeleteConversation(it))
                    },
                    onNewChat = {
                        viewModel.onIntent(ChatIntent.NewChat)
                        scope.launch { drawerState.close() }
                    },
                    onModelManagement = onModelManagement,
                    onManageSkills = onManageSkills,
                    onSettings = onSettings
                )
            }
        },
        gesturesEnabled = !isWide
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    GleeSidebar(
                        conversations = uiState.conversations,
                        selectedConversationId = uiState.currentConversationId,
                        onConversationClick = { viewModel.onIntent(ChatIntent.StartConversation(it)) },
                        onDeleteConversation = { viewModel.onIntent(ChatIntent.DeleteConversation(it)) },
                        onNewChat = { viewModel.onIntent(ChatIntent.NewChat) },
                        onModelManagement = onModelManagement,
                        onManageSkills = onManageSkills,
                        onSettings = onSettings,
                        modifier = Modifier.width(280.dp)
                    )
                    VerticalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                ChatContent(
                    isPrivateMode = uiState.isPrivateMode,
                    suggestions = uiState.suggestions,
                    currentInput = uiState.currentInput,
                    isModelReady = uiState.isModelReady,
                    selectedModel = uiState.selectedModel,
                    attachedFiles = uiState.attachedFiles,
                    messages = uiState.messages,
                    reversedMessages = reversedMessages,
                    isStreaming = uiState.isStreaming,
                    streamingContent = uiState.streamingContent,
                    onIntent = viewModel::onIntent,
                    onPickFile = { launcher.launch() },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onInspectorClick = {
                        currentActionSheet = ChatActionSheetType.Root
                        showActionSheet = true
                    },
                    onModelSelectionClick = { showModelSelectionSheet = true },
                    onModelManagement = onModelManagement,
                    isWide = isWide,
                    modifier = Modifier.weight(1f)
                )

                if (isWide || isMedium) {
                    VerticalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Column(modifier = Modifier.width(350.dp).fillMaxHeight()) {
                        GleeActionMenuSheet(
                            onSelectAction = { currentActionSheet = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentActionSheet) {
                                ChatActionSheetType.Intelligence, ChatActionSheetType.Root -> {
                                    GleeIntelligenceSheet(
                                        config = uiState.modelConfig,
                                        systemPrompt = uiState.systemPrompt,
                                        onConfigChange = {
                                            viewModel.onIntent(
                                                ChatIntent.UpdateModelConfig(it)
                                            )
                                        },
                                        onUpdateSystemPrompt = {
                                            viewModel.onIntent(
                                                ChatIntent.UpdateSystemPrompt(it)
                                            )
                                        },
                                        onRestoreDefaultPrompt = {
                                            viewModel.onIntent(
                                                ChatIntent.RestoreDefaultSystemPrompt
                                            )
                                        },
                                        onBack = { currentActionSheet = ChatActionSheetType.Root }
                                    )
                                }

                                ChatActionSheetType.Performance -> {
                                    GleePerformanceSheet(
                                        metrics = uiState.metrics,
                                        onBenchmarkClick = { showBenchmarkDialog = true },
                                        onBack = { currentActionSheet = ChatActionSheetType.Root }
                                    )
                                }

                                ChatActionSheetType.Skills -> {
                                    GleeSkillsSheet(
                                        activeSkills = uiState.activeSkills,
                                        onToggleSkill = { id, enabled ->
                                            viewModel.onIntent(ChatIntent.ToggleSkill(id, enabled))
                                        },
                                        onBack = { currentActionSheet = ChatActionSheetType.Root }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isInitializing) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    GleeLoadingOverlay(
                        title = stringResource(Res.string.model_initializing),
                        subtitle = stringResource(Res.string.please_wait)
                    )
                }
            }
        }
    }

    if (showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showActionSheet = false
                currentActionSheet = ChatActionSheetType.Root
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                when (currentActionSheet) {
                    ChatActionSheetType.Root -> {
                        GleeActionMenuSheet(
                            onSelectAction = { currentActionSheet = it }
                        )
                    }

                    ChatActionSheetType.Intelligence -> {
                        GleeIntelligenceSheet(
                            config = uiState.modelConfig,
                            systemPrompt = uiState.systemPrompt,
                            onConfigChange = { viewModel.onIntent(ChatIntent.UpdateModelConfig(it)) },
                            onUpdateSystemPrompt = {
                                viewModel.onIntent(
                                    ChatIntent.UpdateSystemPrompt(
                                        it
                                    )
                                )
                            },
                            onRestoreDefaultPrompt = { viewModel.onIntent(ChatIntent.RestoreDefaultSystemPrompt) },
                            onBack = { currentActionSheet = ChatActionSheetType.Root },
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }

                    ChatActionSheetType.Performance -> {
                        GleePerformanceSheet(
                            metrics = uiState.metrics,
                            onBenchmarkClick = {
                                showActionSheet = false
                                showBenchmarkDialog = true
                            },
                            onBack = { currentActionSheet = ChatActionSheetType.Root },
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }

                    ChatActionSheetType.Skills -> {
                        GleeSkillsSheet(
                            activeSkills = uiState.activeSkills,
                            onToggleSkill = { id, enabled ->
                                viewModel.onIntent(ChatIntent.ToggleSkill(id, enabled))
                            },
                            onBack = { currentActionSheet = ChatActionSheetType.Root },
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }
                }
            }
        }
    }

    if (showModelSelectionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelSelectionSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ModelSelectionContent(
                availableModels = uiState.availableModels,
                selectedModel = uiState.selectedModel,
                onModelSelected = { model ->
                    if (model.downloadStatus == ModelDownloadStatus.Downloaded) {
                        viewModel.onIntent(ChatIntent.SelectModel(model))
                    } else {
                        onModelManagement()
                    }
                    showModelSelectionSheet = false
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            )
        }
    }

    if (showBenchmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBenchmarkDialog = false },
            title = { Text(stringResource(Res.string.model_benchmark)) },
            text = { Text(stringResource(Res.string.benchmark_desc)) },
            confirmButton = {
                Button(onClick = { showBenchmarkDialog = false }) {
                    Text(stringResource(Res.string.start_benchmark))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBenchmarkDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    uiState.modelToDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ChatIntent.CancelDeleteModel) },
            title = { Text(stringResource(Res.string.delete_model_title, model.name)) },
            text = { Text(stringResource(Res.string.delete_model_desc)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.onIntent(ChatIntent.ConfirmDeleteModel) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(ChatIntent.CancelDeleteModel) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    uiState.conversationToDelete?.let {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ChatIntent.CancelDeleteConversation) },
            title = { Text(stringResource(Res.string.delete_conversation_title)) },
            text = { Text(stringResource(Res.string.delete_conversation_desc)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.onIntent(ChatIntent.ConfirmDeleteConversation) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(ChatIntent.CancelDeleteConversation) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (uiState.showIncognitoInfoDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ChatIntent.DismissIncognitoInfo) },
            title = { Text(stringResource(Res.string.incognito_info_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.incognito_desc))
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = !uiState.shouldShowIncognitoInfo,
                            onCheckedChange = { viewModel.onIntent(ChatIntent.SetShowIncognitoInfo(!it)) }
                        )
                        Text(
                            text = stringResource(Res.string.dont_show_again),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.onIntent(ChatIntent.DismissIncognitoInfo) }) {
                    Text(stringResource(Res.string.done))
                }
            }
        )
    }
}

@Composable
fun ChatContent(
    isPrivateMode: Boolean,
    suggestions: List<String>,
    currentInput: String,
    isModelReady: Boolean,
    selectedModel: ModelInfo?,
    attachedFiles: List<AttachedFile>,
    messages: MessageList,
    reversedMessages: MessageList,
    isStreaming: Boolean,
    streamingContent: String,
    onIntent: (ChatIntent) -> Unit,
    onPickFile: () -> Unit,
    onMenuClick: () -> Unit,
    onInspectorClick: () -> Unit,
    onModelSelectionClick: () -> Unit,
    onModelManagement: () -> Unit,
    isWide: Boolean,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ChatTopBar(
                showMenuIcon = !isWide,
                isPrivateMode = isPrivateMode,
                onMenuClick = onMenuClick,
                onTogglePrivate = { onIntent(ChatIntent.TogglePrivateMode) },
                onNewChat = { onIntent(ChatIntent.NewChat) }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (messages.isEmpty() && !isStreaming) {
                    val defaultSuggestions = listOf(
                        stringResource(Res.string.suggestion_trip),
                        stringResource(Res.string.suggestion_recipe),
                        stringResource(Res.string.suggestion_email),
                        stringResource(Res.string.suggestion_workout)
                    )
                    SuggestionChips(
                        suggestions = suggestions.ifEmpty { defaultSuggestions },
                        onSuggestionClick = { onIntent(ChatIntent.SelectSuggestion(it)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }

                ChatInputBar(
                    input = currentInput,
                    isStreaming = isStreaming,
                    isModelReady = isModelReady,
                    selectedModel = selectedModel,
                    attachedFiles = attachedFiles,
                    onInputChange = { onIntent(ChatIntent.UpdateInput(it)) },
                    onSend = { onIntent(ChatIntent.SendMessage) },
                    onStop = { onIntent(ChatIntent.StopStreaming) },
                    onPickFile = onPickFile,
                    onRemoveFile = { onIntent(ChatIntent.RemoveFile(it)) },
                    onModelManagement = onModelManagement,
                    onInspectorClick = onInspectorClick,
                    onModelSelectionClick = onModelSelectionClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            // Auto-scroll logic: only if user is already at the bottom.
            // We track both messages size and streaming content to follow the live output.
            LaunchedEffect(messages.size, streamingContent) {
                val isAtBottom =
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 50
                if (isAtBottom && (messages.isNotEmpty() || isStreaming)) {
                    listState.animateScrollToItem(0)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = if (isWide) 64.dp else 16.dp,
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                reverseLayout = true
            ) {
                item(key = "bottom_spacer") { Spacer(Modifier.height(32.dp)) }

                if (isStreaming) {
                    item(key = "streaming_bubble") {
                        StreamingBubble(streamingContent)
                    }
                }

                items(
                    items = reversedMessages,
                    key = { it.id }
                ) { message ->
                    MessageBubble(message)
                }

                item(key = "welcome_header") {
                    if (messages.isEmpty() && !isStreaming) {
                        WelcomeHeader(isPrivateMode = isPrivateMode)
                    } else {
                        Spacer(Modifier.height(0.dp))
                    }
                }
            }
        }
    }
}
