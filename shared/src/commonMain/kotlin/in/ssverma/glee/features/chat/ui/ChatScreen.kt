@file:OptIn(ExperimentalMaterial3Api::class)

package `in`.ssverma.glee.features.chat.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.cancel
import glee.shared.generated.resources.copied_to_clipboard
import glee.shared.generated.resources.delete
import glee.shared.generated.resources.delete_conversation_desc
import glee.shared.generated.resources.delete_conversation_title
import glee.shared.generated.resources.done
import glee.shared.generated.resources.dont_show_again
import glee.shared.generated.resources.incognito_desc
import glee.shared.generated.resources.incognito_info_title
import glee.shared.generated.resources.initializing_model_banner
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
import `in`.ssverma.glee.features.chat.ui.components.GleeToolsSheet
import `in`.ssverma.glee.features.chat.ui.components.MessageBubble
import `in`.ssverma.glee.features.chat.ui.components.ModelSelectionContent
import `in`.ssverma.glee.features.chat.ui.components.StreamingBubble
import `in`.ssverma.glee.features.chat.ui.components.SuggestionChips
import `in`.ssverma.glee.features.chat.ui.components.WelcomeHeader
import `in`.ssverma.glee.features.models.ModelManagementIntent
import `in`.ssverma.glee.features.models.ModelManagementViewModel
import `in`.ssverma.glee.features.settings.SettingsIntent
import `in`.ssverma.glee.features.settings.SettingsViewModel
import `in`.ssverma.glee.features.skills.ManageSkillsIntent
import `in`.ssverma.glee.features.skills.ManageToolsViewModel
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// For permission handling
@Composable
expect fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    modelManagementViewModel: ModelManagementViewModel = koinViewModel(),
    manageToolsViewModel: ManageToolsViewModel = koinViewModel(),
    onModelManagement: () -> Unit = { },
    onManageSkills: () -> Unit = { },
    onSettings: () -> Unit = { },
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val modelManagementState by modelManagementViewModel.uiState.collectAsState()
    val manageSkillsState by manageToolsViewModel.uiState.collectAsState()

    val reversedMessages = remember(uiState.messages) {
        MessageList(uiState.messages.asReversed())
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showActionSheet by remember { mutableStateOf(false) }
    var currentActionSheet by remember { mutableStateOf(ChatActionSheetType.Root) }

    var showModelSelectionSheet by remember { mutableStateOf(false) }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isWide = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    val isMedium = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM

    val launcher = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single
    ) { file ->
        file?.let { viewModel.onIntent(ChatIntent.PickFile(it)) }
    }

    val anyModelDownloaded = remember(modelManagementState.availableModels) {
        modelManagementState.availableModels.any { it.downloadStatus == ModelDownloadStatus.Downloaded }
    }

    val showNoModelBanner = remember(modelManagementState.availableModels, anyModelDownloaded) {
        modelManagementState.availableModels.isNotEmpty() && !anyModelDownloaded
    }

    val permissionLauncher = rememberPermissionLauncher { granted ->
        if (granted) {
            viewModel.onIntent(ChatIntent.ToggleVoiceRecording)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
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
                    onModelManagement = {
                        onModelManagement()
                        scope.launch { drawerState.close() }
                    },
                    onManageSkills = {
                        onManageSkills()
                        scope.launch { drawerState.close() }
                    },
                    onSettings = {
                        onSettings()
                        scope.launch { drawerState.close() }
                    }
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
                    isInitializing = uiState.isInitializing,
                    anyModelDownloaded = anyModelDownloaded,
                    showNoModelBanner = showNoModelBanner,
                    selectedModel = uiState.selectedModel,
                    attachedFiles = uiState.attachedFiles,
                    isSpeechRecognitionSupported = uiState.isSpeechRecognitionSupported,
                    isRecordingVoice = uiState.isRecordingVoice,
                    messages = uiState.messages,
                    reversedMessages = reversedMessages,
                    isStreaming = uiState.isStreaming,
                    streamingContent = uiState.streamingContent,
                    onIntent = viewModel::onIntent,
                    onToggleVoiceRecording = {
                        permissionLauncher()
                    },
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
                        AnimatedContent(
                            targetState = currentActionSheet,
                            transitionSpec = {
                                if (targetState != ChatActionSheetType.Root) {
                                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                                } else {
                                    (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                                }
                            },
                            label = "InspectorSheet"
                        ) { sheetType ->
                            when (sheetType) {
                                ChatActionSheetType.Root -> {
                                    GleeActionMenuSheet(
                                        isAgentic = settingsState.modelConfig.isAgentic,
                                        onToggleAgentic = {
                                            settingsViewModel.onIntent(
                                                SettingsIntent.UpdateModelConfig(
                                                    settingsState.modelConfig.copy(isAgentic = it)
                                                )
                                            )
                                            settingsViewModel.onIntent(SettingsIntent.SaveIntelligenceConfig)
                                        },
                                        onSelectAction = { currentActionSheet = it },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                ChatActionSheetType.Intelligence -> {
                                    GleeIntelligenceSheet(
                                        config = settingsState.modelConfig,
                                        systemPrompt = settingsState.systemPrompt,
                                        onConfigChange = {
                                            settingsViewModel.onIntent(
                                                SettingsIntent.UpdateModelConfig(it)
                                            )
                                        },
                                        onUpdateSystemPrompt = {
                                            settingsViewModel.onIntent(
                                                SettingsIntent.UpdateSystemPrompt(it)
                                            )
                                        },
                                        onRestoreDefaultPrompt = {
                                            settingsViewModel.onIntent(
                                                SettingsIntent.RestoreDefaultSystemPrompt
                                            )
                                        },
                                        onSave = {
                                            settingsViewModel.onIntent(SettingsIntent.SaveIntelligenceConfig)
                                        },
                                        onBack = { currentActionSheet = ChatActionSheetType.Root }
                                    )
                                }

                                ChatActionSheetType.Performance -> {
                                    GleePerformanceSheet(
                                        metrics = uiState.metrics,
                                        onBack = { currentActionSheet = ChatActionSheetType.Root }
                                    )
                                }

                                ChatActionSheetType.Skills -> {
                                    GleeToolsSheet(
                                        activeSkills = manageSkillsState.activeSkills,
                                        onToggleSkill = { id, enabled ->
                                            manageToolsViewModel.onIntent(
                                                ManageSkillsIntent.ToggleSkill(
                                                    id,
                                                    enabled
                                                )
                                            )
                                        },
                                        onManageSkills = {
                                            onManageSkills()
                                        },
                                        onBack = { currentActionSheet = ChatActionSheetType.Root }
                                    )
                                }
                            }
                        }
                    }
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
                            isAgentic = settingsState.modelConfig.isAgentic,
                            onToggleAgentic = {
                                settingsViewModel.onIntent(
                                    SettingsIntent.UpdateModelConfig(
                                        settingsState.modelConfig.copy(isAgentic = it)
                                    )
                                )
                                settingsViewModel.onIntent(SettingsIntent.SaveIntelligenceConfig)
                            },
                            onSelectAction = { currentActionSheet = it }
                        )
                    }

                    ChatActionSheetType.Intelligence -> {
                        GleeIntelligenceSheet(
                            config = settingsState.modelConfig,
                            systemPrompt = settingsState.systemPrompt,
                            onConfigChange = {
                                settingsViewModel.onIntent(
                                    SettingsIntent.UpdateModelConfig(
                                        it
                                    )
                                )
                            },
                            onUpdateSystemPrompt = {
                                settingsViewModel.onIntent(
                                    SettingsIntent.UpdateSystemPrompt(
                                        it
                                    )
                                )
                            },
                            onRestoreDefaultPrompt = { settingsViewModel.onIntent(SettingsIntent.RestoreDefaultSystemPrompt) },
                            onSave = {
                                settingsViewModel.onIntent(SettingsIntent.SaveIntelligenceConfig)
                                showActionSheet = false
                            },
                            onBack = { currentActionSheet = ChatActionSheetType.Root }
                        )
                    }

                    ChatActionSheetType.Performance -> {
                        GleePerformanceSheet(
                            metrics = uiState.metrics,
                            onBack = { currentActionSheet = ChatActionSheetType.Root }
                        )
                    }

                    ChatActionSheetType.Skills -> {
                        GleeToolsSheet(
                            activeSkills = manageSkillsState.activeSkills,
                            onToggleSkill = { id, enabled ->
                                manageToolsViewModel.onIntent(
                                    ManageSkillsIntent.ToggleSkill(
                                        id,
                                        enabled
                                    )
                                )
                            },
                            onManageSkills = {
                                onManageSkills()
                                showActionSheet = false
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
                availableModels = modelManagementState.availableModels,
                selectedModel = uiState.selectedModel,
                onModelSelected = { model ->
                    if (model.downloadStatus == ModelDownloadStatus.Downloaded) {
                        modelManagementViewModel.onIntent(ModelManagementIntent.SelectModel(model))
                    } else {
                        onModelManagement()
                    }
                    showModelSelectionSheet = false
                },
                onManageModelsClick = {
                    showModelSelectionSheet = false
                    onModelManagement()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            )
        }
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
    isInitializing: Boolean,
    anyModelDownloaded: Boolean,
    showNoModelBanner: Boolean,
    selectedModel: ModelInfo?,
    attachedFiles: List<AttachedFile>,
    isSpeechRecognitionSupported: Boolean,
    isRecordingVoice: Boolean,
    messages: MessageList,
    reversedMessages: MessageList,
    isStreaming: Boolean,
    streamingContent: String,
    onIntent: (ChatIntent) -> Unit,
    onToggleVoiceRecording: () -> Unit,
    onPickFile: () -> Unit,
    onMenuClick: () -> Unit,
    onInspectorClick: () -> Unit,
    onModelSelectionClick: () -> Unit,
    onModelManagement: () -> Unit,
    isWide: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(Res.string.copied_to_clipboard)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatTopBar(
                showMenuIcon = !isWide && anyModelDownloaded,
                isPrivateMode = isPrivateMode,
                onMenuClick = onMenuClick,
                onTogglePrivate = { onIntent(ChatIntent.TogglePrivateMode) },
                onNewChat = { onIntent(ChatIntent.NewChat) },
                onDownloadAppsClick = { onIntent(ChatIntent.SetShowDownloadDialog(true)) },
                scrollBehavior = scrollBehavior,
                actionsEnabled = anyModelDownloaded
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                if (messages.isEmpty() && !isStreaming && isModelReady && suggestions.isNotEmpty()) {
                    SuggestionChips(
                        suggestions = suggestions,
                        onSuggestionClick = { onIntent(ChatIntent.SelectSuggestion(it)) },
                        modifier = Modifier.padding(vertical = 8.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }

                AnimatedVisibility(
                    visible = isInitializing,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    InitializingInfoBar()
                }

                ChatInputBar(
                    input = currentInput,
                    isStreaming = isStreaming,
                    isModelReady = isModelReady,
                    selectedModel = selectedModel,
                    attachedFiles = attachedFiles,
                    isSpeechRecognitionSupported = isSpeechRecognitionSupported,
                    isRecordingVoice = isRecordingVoice,
                    onInputChange = { onIntent(ChatIntent.UpdateInput(it)) },
                    onSend = { onIntent(ChatIntent.SendMessage) },
                    onStop = { onIntent(ChatIntent.StopStreaming) },
                    onPickFile = onPickFile,
                    onRemoveFile = { onIntent(ChatIntent.RemoveFile(it)) },
                    onToggleVoiceRecording = onToggleVoiceRecording,
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
            // Auto-scroll logic
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
                    MessageBubble(
                        message = message,
                        onCopy = {
                            scope.launch {
                                snackbarHostState.showSnackbar(copiedMessage)
                            }
                        }
                    )
                }

                item(key = "welcome_header") {
                    if (messages.isEmpty() && !isStreaming) {
                        WelcomeHeader(
                            isPrivateMode = isPrivateMode,
                            showNoModelBanner = showNoModelBanner,
                            onDownloadClick = onModelManagement
                        )
                    } else {
                        Spacer(Modifier.height(0.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InitializingInfoBar() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.initializing_model_banner),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
