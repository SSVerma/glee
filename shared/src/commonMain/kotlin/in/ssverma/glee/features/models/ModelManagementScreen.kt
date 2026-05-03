@file:OptIn(ExperimentalMaterial3Api::class)

package `in`.ssverma.glee.features.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
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
import glee.shared.generated.resources.allow
import glee.shared.generated.resources.cancel
import glee.shared.generated.resources.cancel_download_confirm
import glee.shared.generated.resources.cancel_download_desc
import glee.shared.generated.resources.cancel_download_dismiss
import glee.shared.generated.resources.cancel_download_title
import glee.shared.generated.resources.cancel_import
import glee.shared.generated.resources.delete
import glee.shared.generated.resources.delete_model_desc
import glee.shared.generated.resources.delete_model_title
import glee.shared.generated.resources.download
import glee.shared.generated.resources.downloading_progress
import glee.shared.generated.resources.hardware_requirements
import glee.shared.generated.resources.import_failed
import glee.shared.generated.resources.import_model
import glee.shared.generated.resources.imported
import glee.shared.generated.resources.importing_model
import glee.shared.generated.resources.importing_model_desc
import glee.shared.generated.resources.learn_more_license
import glee.shared.generated.resources.manage_models_desc
import glee.shared.generated.resources.model_management
import glee.shared.generated.resources.notification_rationale_desc
import glee.shared.generated.resources.notification_rationale_title
import glee.shared.generated.resources.ok
import glee.shared.generated.resources.proceed_anyway
import glee.shared.generated.resources.recommended
import glee.shared.generated.resources.retry
import glee.shared.generated.resources.settings
import glee.shared.generated.resources.try_it
import `in`.ssverma.glee.core.common.platform.PermissionType
import `in`.ssverma.glee.features.chat.domain.model.ModelDownloadStatus
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import `in`.ssverma.glee.features.chat.ui.rememberPermissionLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ModelManagementScreen(
    onClose: () -> Unit,
    viewModel: ModelManagementViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isWide = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    val launcher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("task", "bin", "litertlm", "tflite")),
        mode = PickerMode.Single
    ) { file ->
        file?.let { viewModel.onIntent(ModelManagementIntent.ImportModelFile(it)) }
    }

    val permissionLauncher = rememberPermissionLauncher(PermissionType.Notifications) { _ ->
        // No-op here, as we start the download immediately in the button click now
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(resource = Res.string.model_management)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { launcher.launch() },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(resource = Res.string.import_model))
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
                text = stringResource(resource = Res.string.manage_models_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            val modelChunks = if (isWide) {
                uiState.availableModels.chunked(2)
            } else {
                uiState.availableModels.chunked(1)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(modelChunks) { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        chunk.forEach { model ->
                            ModelManagementItem(
                                model = model,
                                onDownload = {
                                    viewModel.onIntent(
                                        ModelManagementIntent.RequestDownloadModel(
                                            model
                                        )
                                    )
                                },
                                onCancel = {
                                    viewModel.onIntent(ModelManagementIntent.CancelDownload(model.id))
                                },
                                onDelete = {
                                    viewModel.onIntent(ModelManagementIntent.DeleteModel(model))
                                },
                                onSelect = {
                                    viewModel.onIntent(ModelManagementIntent.SelectModel(model))
                                    onClose()
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        if (isWide && chunk.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (uiState.modelForNotificationRationale != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ModelManagementIntent.DismissNotificationRationale) },
            title = { Text(stringResource(Res.string.notification_rationale_title)) },
            text = { Text(stringResource(Res.string.notification_rationale_desc)) },
            confirmButton = {
                if (uiState.isPermanentlyDenied) {
                    Button(onClick = { viewModel.onIntent(ModelManagementIntent.OpenAppSettings) }) {
                        Text(stringResource(Res.string.settings))
                    }
                } else {
                    Button(onClick = {
                        uiState.modelForNotificationRationale?.let {
                            viewModel.onIntent(ModelManagementIntent.DownloadModel(it))
                        }
                        permissionLauncher()
                        viewModel.onIntent(ModelManagementIntent.DismissNotificationRationale)
                    }) {
                        Text(stringResource(Res.string.allow))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        uiState.modelForNotificationRationale?.let {
                            viewModel.onIntent(ModelManagementIntent.DownloadModel(it))
                        }
                        viewModel.onIntent(ModelManagementIntent.DismissNotificationRationale)
                    }
                ) {
                    Text(stringResource(Res.string.proceed_anyway))
                }
            }
        )
    }

    uiState.modelToDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ModelManagementIntent.CancelDeleteModel) },
            title = { Text(stringResource(resource = Res.string.delete_model_title, model.name)) },
            text = { Text(stringResource(resource = Res.string.delete_model_desc)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.onIntent(ModelManagementIntent.ConfirmDeleteModel) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        stringResource(Res.string.delete),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(ModelManagementIntent.CancelDeleteModel) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (uiState.showCancelDownloadDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ModelManagementIntent.DismissCancelDownload) },
            title = { Text(stringResource(resource = Res.string.cancel_download_title)) },
            text = { Text(stringResource(resource = Res.string.cancel_download_desc)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.onIntent(ModelManagementIntent.ConfirmCancelDownload) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(resource = Res.string.cancel_download_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(ModelManagementIntent.DismissCancelDownload) }) {
                    Text(stringResource(resource = Res.string.cancel_download_dismiss))
                }
            }
        )
    }

    if (uiState.isImporting) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(ModelManagementIntent.CancelImport) }) {
                    Text(stringResource(resource = Res.string.cancel_import))
                }
            },
            title = { Text(stringResource(resource = Res.string.importing_model)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { uiState.importProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(resource = Res.string.importing_model_desc))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${(uiState.importProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        )
    }

    uiState.importError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ModelManagementIntent.ResetImportError) },
            confirmButton = {
                Button(onClick = { viewModel.onIntent(ModelManagementIntent.ResetImportError) }) {
                    Text(stringResource(resource = Res.string.ok))
                }
            },
            title = { Text(stringResource(resource = Res.string.import_failed)) },
            text = { Text(error) }
        )
    }
}

@Composable
fun ModelManagementItem(
    model: ModelInfo,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                                        stringResource(resource = Res.string.recommended),
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
                        if (model.isCustom) {
                            Spacer(Modifier.width(8.dp))
                            SuggestionChip(
                                onClick = { },
                                label = {
                                    Text(
                                        stringResource(Res.string.imported),
                                        fontSize = 10.sp
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                    text = stringResource(resource = Res.string.learn_more_license),
                    style = MaterialTheme.typography.labelSmall,
                    textDecoration = TextDecoration.Underline
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.height(12.dp))

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
                            text = stringResource(resource = Res.string.hardware_requirements),
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
                        Text(stringResource(resource = Res.string.download))
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
                                    resource =
                                        Res.string.downloading_progress,
                                    (status.progress * 100).toInt()
                                ),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            TextButton(onClick = onCancel) {
                                Text(
                                    stringResource(resource = Res.string.cancel),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                ModelDownloadStatus.Downloaded -> {
                    Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(resource = Res.string.try_it))
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
                                stringResource(resource = Res.string.retry)
                            )
                        }
                    }
                }
            }
        }
    }
}
