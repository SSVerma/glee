@file:OptIn(ExperimentalMaterial3Api::class)

package `in`.ssverma.glee.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import glee.shared.generated.resources.model_benchmark
import glee.shared.generated.resources.model_initializing
import glee.shared.generated.resources.please_wait
import glee.shared.generated.resources.start_benchmark
import glee.shared.generated.resources.suggestion_email
import glee.shared.generated.resources.suggestion_recipe
import glee.shared.generated.resources.suggestion_trip
import glee.shared.generated.resources.suggestion_workout
import `in`.ssverma.glee.domain.model.*
import `in`.ssverma.glee.ui.chat.components.*
import `in`.ssverma.glee.ui.components.GleeSidebar
import `in`.ssverma.glee.ui.components.loading.GleeLoadingOverlay
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
    val messages by remember { derivedStateOf { uiState.messages } }
    val isStreaming by remember { derivedStateOf { uiState.isStreaming } }
    val streamingContent by remember { derivedStateOf { uiState.streamingContent } }
    val reversedMessages by remember { derivedStateOf { MessageList(messages.asReversed()) } }

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
                    onNewChat = {
                        viewModel.onIntent(ChatIntent.ClearChat)
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
                        onNewChat = { viewModel.onIntent(ChatIntent.ClearChat) },
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
                    messages = messages,
                    reversedMessages = reversedMessages,
                    isStreaming = isStreaming,
                    streamingContent = streamingContent,
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
                            onUpdateSystemPrompt = { viewModel.onIntent(ChatIntent.UpdateSystemPrompt(it)) },
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
                onTogglePrivate = { onIntent(ChatIntent.TogglePrivateMode) }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
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

        // With reverseLayout = true, index 0 is the bottom.
        val isAtBottom by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf true
                
                // Check if the first visible item is the very first one (the bottom-most item)
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 50
            }
        }

        // Auto-scroll logic: only if user is already at the bottom.
        // We track both messages size and streaming content to follow the live output.
        LaunchedEffect(messages.size, streamingContent) {
            if (isAtBottom && (messages.isNotEmpty() || isStreaming)) {
                listState.animateScrollToItem(0)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                    WelcomeHeader()
                } else {
                    Spacer(Modifier.height(0.dp))
                }
            }
        }
    }
}
