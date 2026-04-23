@file:OptIn(ExperimentalMaterial3Api::class)

package `in`.ssverma.glee.features.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.cancel
import glee.shared.generated.resources.delete
import glee.shared.generated.resources.delete_model_desc
import glee.shared.generated.resources.delete_model_title
import glee.shared.generated.resources.download
import glee.shared.generated.resources.downloading_progress
import glee.shared.generated.resources.hardware_requirements
import glee.shared.generated.resources.learn_more_license
import glee.shared.generated.resources.manage_models_desc
import glee.shared.generated.resources.model_management
import glee.shared.generated.resources.recommended
import glee.shared.generated.resources.retry
import glee.shared.generated.resources.try_it
import `in`.ssverma.glee.features.chat.domain.model.ModelDownloadStatus
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import `in`.ssverma.glee.features.chat.ui.ChatIntent
import `in`.ssverma.glee.features.chat.ui.ChatViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ModelManagementScreen(
    onClose: () -> Unit,
    viewModel: ChatViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isWide = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.model_management)) },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onIntent(ChatIntent.ImportModel) }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.manage_models_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = if (isWide) GridCells.Fixed(2) else GridCells.Fixed(1),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.availableModels) { model ->
                    ModelManagementItem(
                        model = model,
                        onDownload = { viewModel.onIntent(ChatIntent.DownloadModel(model)) },
                        onCancel = { viewModel.onIntent(ChatIntent.CancelDownload(model.id)) },
                        onDelete = { viewModel.onIntent(ChatIntent.DeleteModel(model)) },
                        onSelect = {
                            viewModel.onIntent(ChatIntent.SelectModel(model))
                            onClose()
                        }
                    )
                }
            }
        }
    }

    uiState.modelToDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ChatIntent.CancelDeleteModel) },
            title = { Text(stringResource(Res.string.delete_model_title, model.name)) },
            text = { Text(stringResource(Res.string.delete_model_desc)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.onIntent(ChatIntent.ConfirmDeleteModel) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        stringResource(Res.string.delete),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(ChatIntent.CancelDeleteModel) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ModelManagementItem(
    model: ModelInfo,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (model.isRecommended) {
                            Spacer(Modifier.width(8.dp))
                            SuggestionChip(
                                onClick = { },
                                label = {
                                    Text(
                                        stringResource(Res.string.recommended),
                                        fontSize = 10.sp
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DownloadForOffline,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${model.sizeGb} GB",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                if (model.downloadStatus == ModelDownloadStatus.Downloaded) {
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null) }
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = { uriHandler.openUri(model.infoUrl) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.learn_more_license),
                    style = MaterialTheme.typography.labelSmall,
                    textDecoration = TextDecoration.Underline
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // Resource Requirements Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Memory,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.hardware_requirements),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = model.resourceUsage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when (val status = model.downloadStatus) {
                ModelDownloadStatus.NotDownloaded -> {
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.download))
                    }
                }

                is ModelDownloadStatus.Downloading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { status.progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(
                                    Res.string.downloading_progress,
                                    (status.progress * 100).toInt()
                                ),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            TextButton(onClick = onCancel) {
                                Text(
                                    stringResource(Res.string.cancel),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                ModelDownloadStatus.Downloaded -> {
                    Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.try_it))
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                is ModelDownloadStatus.Error -> {
                    Column {
                        Text(
                            text = status.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(Res.string.retry)
                            )
                        }
                    }
                }
            }
        }
    }
}
