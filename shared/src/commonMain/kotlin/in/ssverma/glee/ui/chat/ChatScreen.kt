@file:OptIn(ExperimentalMaterial3Api::class)

package `in`.ssverma.glee.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import com.mikepenz.markdown.m3.Markdown
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.active_skills
import glee.shared.generated.resources.ai_disclosure
import glee.shared.generated.resources.app_name
import glee.shared.generated.resources.ask_glee
import glee.shared.generated.resources.benchmark
import glee.shared.generated.resources.cancel
import glee.shared.generated.resources.context_window
import glee.shared.generated.resources.download
import glee.shared.generated.resources.inspector
import glee.shared.generated.resources.latency
import glee.shared.generated.resources.meet_glee
import glee.shared.generated.resources.model_config
import glee.shared.generated.resources.model_loading
import glee.shared.generated.resources.model_not_ready_warning
import glee.shared.generated.resources.ram_usage
import glee.shared.generated.resources.restore_defaults
import glee.shared.generated.resources.select_model
import glee.shared.generated.resources.suggestion_email
import glee.shared.generated.resources.suggestion_recipe
import glee.shared.generated.resources.suggestion_trip
import glee.shared.generated.resources.suggestion_workout
import glee.shared.generated.resources.temperature
import glee.shared.generated.resources.top_k
import glee.shared.generated.resources.use_gpu
import `in`.ssverma.glee.domain.model.AttachedFile
import `in`.ssverma.glee.domain.model.ChatMessage
import `in`.ssverma.glee.domain.model.ChatMetrics
import `in`.ssverma.glee.domain.model.ChatRole
import `in`.ssverma.glee.domain.model.GleeModelConfig
import `in`.ssverma.glee.domain.model.ModelDownloadStatus
import `in`.ssverma.glee.domain.model.ModelInfo
import `in`.ssverma.glee.ui.components.GleeSidebar
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showInspectorSheet by remember { mutableStateOf(false) }
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
        Row(modifier = modifier.fillMaxSize()) {
            if (isWide) {
                GleeSidebar(
                    onNewChat = { viewModel.onIntent(ChatIntent.ClearChat) },
                    onModelManagement = onModelManagement,
                    onManageSkills = onManageSkills,
                    onSettings = onSettings,
                    modifier = Modifier.width(280.dp)
                )
                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }

            ChatContent(
                uiState = uiState,
                onIntent = viewModel::onIntent,
                onPickFile = { launcher.launch() },
                onMenuClick = { scope.launch { drawerState.open() } },
                onInspectorClick = { showInspectorSheet = true },
                onModelSelectionClick = { showModelSelectionSheet = true },
                onModelManagement = onModelManagement,
                isWide = isWide,
                modifier = Modifier.weight(1f)
            )

            if (isWide || isMedium) {
                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                InspectorPanel(
                    metrics = uiState.metrics,
                    selectedModel = uiState.selectedModel,
                    activeSkills = uiState.activeSkills,
                    config = uiState.modelConfig,
                    systemPrompt = uiState.systemPrompt,
                    onConfigChange = { viewModel.onIntent(ChatIntent.UpdateModelConfig(it)) },
                    onToggleSkill = { id, enabled ->
                        viewModel.onIntent(
                            ChatIntent.ToggleSkill(
                                id,
                                enabled
                            )
                        )
                    },
                    onUpdateSystemPrompt = { viewModel.onIntent(ChatIntent.UpdateSystemPrompt(it)) },
                    onRestoreDefaultPrompt = { viewModel.onIntent(ChatIntent.RestoreDefaultSystemPrompt) },
                    onBenchmarkClick = { showBenchmarkDialog = true },
                    modifier = Modifier.width(320.dp).fillMaxHeight()
                )
            }
        }
    }

    if (showInspectorSheet) {
        ModalBottomSheet(onDismissRequest = { showInspectorSheet = false }) {
            InspectorPanel(
                metrics = uiState.metrics,
                selectedModel = uiState.selectedModel,
                activeSkills = uiState.activeSkills,
                config = uiState.modelConfig,
                systemPrompt = uiState.systemPrompt,
                onConfigChange = { viewModel.onIntent(ChatIntent.UpdateModelConfig(it)) },
                onToggleSkill = { id, enabled ->
                    viewModel.onIntent(
                        ChatIntent.ToggleSkill(
                            id,
                            enabled
                        )
                    )
                },
                onUpdateSystemPrompt = { viewModel.onIntent(ChatIntent.UpdateSystemPrompt(it)) },
                onRestoreDefaultPrompt = { viewModel.onIntent(ChatIntent.RestoreDefaultSystemPrompt) },
                onBenchmarkClick = {
                    showInspectorSheet = false
                    showBenchmarkDialog = true
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            )
        }
    }

    if (showModelSelectionSheet) {
        ModalBottomSheet(onDismissRequest = { showModelSelectionSheet = false }) {
            ModelSelectionContent(
                availableModels = uiState.availableModels,
                selectedModel = uiState.selectedModel,
                onModelSelected = {
                    viewModel.onIntent(ChatIntent.SelectModel(it))
                    showModelSelectionSheet = false
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            )
        }
    }

    uiState.pendingModelDownload?.let { model ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ChatIntent.CancelPendingDownload) },
            title = { Text("Download ${model.name}?") },
            text = { Text("This model is ${model.sizeGb}GB. You need a stable internet connection to download it.") },
            confirmButton = {
                Button(onClick = { viewModel.onIntent(ChatIntent.ConfirmDownload(model)) }) {
                    Text(stringResource(Res.string.download))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(ChatIntent.CancelPendingDownload) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (showBenchmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBenchmarkDialog = false },
            title = { Text("Model Benchmark") },
            text = { Text("This will run a standardized test to measure the inference speed (Tokens Per Second) and memory efficiency of the current model on your device.") },
            confirmButton = {
                Button(onClick = { showBenchmarkDialog = false }) {
                    Text("Start Benchmark")
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
fun ModelSelectionContent(
    availableModels: List<ModelInfo>,
    selectedModel: ModelInfo?,
    onModelSelected: (ModelInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(Res.string.select_model),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(availableModels) { model ->
                ModelSelectionItem(
                    model = model,
                    isSelected = selectedModel?.id == model.id,
                    onClick = { onModelSelected(model) }
                )
            }
        }
    }
}

@Composable
fun ModelSelectionItem(
    model: ModelInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.5f
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = model.resourceUsage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when (val status = model.downloadStatus) {
                ModelDownloadStatus.Downloaded -> {
                    if (isSelected) Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                is ModelDownloadStatus.Downloading -> {
                    CircularProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.size(24.dp)
                    )
                }

                else -> {
                    Icon(
                        Icons.Default.DownloadForOffline,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatContent(
    uiState: ChatState,
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
                isPrivateMode = uiState.isPrivateMode,
                onMenuClick = onMenuClick,
                onTogglePrivate = { onIntent(ChatIntent.TogglePrivateMode) }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (uiState.messages.isEmpty() && !uiState.isStreaming) {
                    val defaultSuggestions = listOf(
                        stringResource(Res.string.suggestion_trip),
                        stringResource(Res.string.suggestion_recipe),
                        stringResource(Res.string.suggestion_email),
                        stringResource(Res.string.suggestion_workout)
                    )
                    SuggestionChips(
                        suggestions = uiState.suggestions.ifEmpty { defaultSuggestions },
                        onSuggestionClick = { onIntent(ChatIntent.SelectSuggestion(it)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }

                ChatInputBar(
                    input = uiState.currentInput,
                    isStreaming = uiState.isStreaming,
                    isModelReady = uiState.isModelReady,
                    selectedModel = uiState.selectedModel,
                    attachedFiles = uiState.attachedFiles,
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

        LaunchedEffect(uiState.messages.size) {
            if (uiState.messages.isNotEmpty()) {
                listState.animateScrollToItem(uiState.messages.size)
            }
        }

        LaunchedEffect(uiState.streamingContent) {
            if (uiState.isStreaming && uiState.streamingContent.isNotEmpty()) {
                listState.scrollToItem(uiState.messages.size + 1)
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (uiState.messages.isEmpty() && !uiState.isStreaming) {
                item { WelcomeHeader() }
            }

            items(uiState.messages) { message ->
                MessageBubble(message)
            }

            if (uiState.isStreaming && uiState.streamingContent.isNotEmpty()) {
                item { StreamingBubble(uiState.streamingContent) }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ChatTopBar(
    showMenuIcon: Boolean,
    isPrivateMode: Boolean,
    onMenuClick: () -> Unit,
    onTogglePrivate: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.app_name),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        navigationIcon = {
            if (showMenuIcon) {
                IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null) }
            }
        },
        actions = {
            IconButton(onClick = onTogglePrivate) {
                Icon(
                    if (isPrivateMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null,
                    tint = if (isPrivateMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

@Composable
fun WelcomeHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.meet_glee),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SuggestionChips(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(suggestions) { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion) },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun InspectorPanel(
    metrics: ChatMetrics,
    selectedModel: ModelInfo?,
    activeSkills: Map<String, Boolean>,
    config: GleeModelConfig,
    systemPrompt: String,
    onConfigChange: (GleeModelConfig) -> Unit,
    onToggleSkill: (String, Boolean) -> Unit,
    onUpdateSystemPrompt: (String) -> Unit,
    onRestoreDefaultPrompt: () -> Unit,
    onBenchmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(Res.string.inspector),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.4f
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape)
                            .background(if (metrics.latencyMs > 0) Color.Green else Color.Gray)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = metrics.modelName, style = MaterialTheme.typography.titleSmall)
                }
                MetricItem(
                    label = stringResource(Res.string.context_window),
                    value = "${metrics.contextUsed / 1000}K / ${metrics.contextMax / 1000}K",
                    progress = metrics.contextUsed.toFloat() / metrics.contextMax,
                    color = MaterialTheme.colorScheme.primary
                )

                MetricItem(
                    label = stringResource(Res.string.ram_usage),
                    value = "${roundToDecimals(metrics.ramUsedGb, 1)} / ${
                        roundToDecimals(
                            metrics.ramTotalGb,
                            0
                        )
                    } GB",
                    progress = metrics.ramUsedGb / metrics.ramTotalGb,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.latency),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "~${metrics.latencyMs}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green
                        )
                    }
                    TextButton(onClick = onBenchmarkClick) {
                        Text(
                            stringResource(Res.string.benchmark),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Text(
            text = "SYSTEM PROMPT",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = systemPrompt,
            onValueChange = onUpdateSystemPrompt,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(12.dp)
        )
        TextButton(onClick = onRestoreDefaultPrompt, contentPadding = PaddingValues(0.dp)) {
            Text(
                stringResource(Res.string.restore_defaults),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Text(
            text = stringResource(Res.string.model_config),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ConfigSlider(
                label = stringResource(Res.string.temperature),
                value = config.temperature,
                range = 0f..1.5f,
                onValueChange = { onConfigChange(config.copy(temperature = it)) })
            ConfigSlider(
                label = stringResource(Res.string.top_k),
                value = config.topK.toFloat(),
                range = 1f..100f,
                onValueChange = { onConfigChange(config.copy(topK = it.toInt())) })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.use_gpu),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = config.useGpu,
                    onCheckedChange = { onConfigChange(config.copy(useGpu = it)) })
            }
        }

        if (selectedModel?.supportsSkills == true) {
            Text(
                text = stringResource(Res.string.active_skills),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activeSkills.forEach { (id, enabled) ->
                    SkillToggle(
                        name = id.replace("_", " ").uppercase(),
                        isEnabled = enabled,
                        onToggle = { onToggleSkill(id, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(roundToDecimals(value, 2), style = MaterialTheme.typography.labelSmall)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

private fun roundToDecimals(value: Float, decimals: Int): String {
    var factor = 1.0
    repeat(decimals) { factor *= 10.0 }
    return ((value * factor).toLong() / factor).toString()
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    progress: Float,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(text = value, style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = color,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun SkillToggle(name: String, isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = isEnabled, onCheckedChange = onToggle)
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.User
    val isTool = message.role == ChatRole.Tool

    if (isTool) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Executing tool: ${message.content.take(30)}...",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        return
    }

    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor =
        if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val contentColor =
        if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isUser) 600.dp else 800.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(containerColor)
                .padding(16.dp)
        ) {
            if (isUser) {
                Text(
                    text = message.content,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Markdown(content = message.content, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun StreamingBubble(content: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(16.dp)
        ) {
            Markdown(content = content, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ChatInputBar(
    input: String,
    isStreaming: Boolean,
    isModelReady: Boolean,
    selectedModel: ModelInfo?,
    attachedFiles: List<AttachedFile>,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onPickFile: () -> Unit,
    onRemoveFile: (AttachedFile) -> Unit,
    onModelManagement: () -> Unit,
    onInspectorClick: () -> Unit,
    onModelSelectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!isModelReady && !isStreaming) {
// ... (omitting middle for brevity if possible, but let's use exact match)
            Card(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                onClick = onModelManagement
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(Res.string.model_not_ready_warning),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Surface(
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (input.isEmpty()) {
                        Text(
                            text = if (isModelReady) stringResource(Res.string.ask_glee) else stringResource(
                                Res.string.model_loading
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = onInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        enabled = !isStreaming && isModelReady,
                        decorationBox = { innerTextField -> innerTextField() }
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (attachedFiles.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(attachedFiles) { file ->
                            InputChip(
                                selected = true,
                                onClick = { },
                                label = { Text(file.name) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                            .clickable { onRemoveFile(file) })
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPickFile,
                        enabled = isModelReady && (selectedModel?.supportsVision ?: true)
                    ) {
                        Icon(Icons.Default.Add, "Attach")
                    }
                    IconButton(onClick = onInspectorClick) {
                        Icon(Icons.Default.Tune, "Tools")
                    }

                    Spacer(Modifier.weight(1f))

                    FilterChip(
                        selected = false,
                        onClick = onModelSelectionClick,
                        label = {
                            Text(
                                if (isModelReady) (selectedModel?.name
                                    ?: stringResource(Res.string.select_model)) else stringResource(
                                    Res.string.select_model
                                )
                            )
                        },
                        leadingIcon = if (isModelReady) {
                            {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.size(40.dp).background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            CircleShape
                        )
                    ) {
                        Icon(Icons.Default.Mic, null, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (isStreaming) {
                                onStop()
                            } else {
                                onSend()
                            }
                        },
                        enabled = (isStreaming || input.isNotBlank() || attachedFiles.isNotEmpty()) && isModelReady,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (isStreaming) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                            "Action",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.ai_disclosure),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
