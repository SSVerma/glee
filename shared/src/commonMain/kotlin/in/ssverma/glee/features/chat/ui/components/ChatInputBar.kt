package `in`.ssverma.glee.features.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.action
import glee.shared.generated.resources.ai_disclosure
import glee.shared.generated.resources.ask_glee
import glee.shared.generated.resources.attach
import glee.shared.generated.resources.model_loading
import glee.shared.generated.resources.model_not_ready_warning
import glee.shared.generated.resources.select_model
import glee.shared.generated.resources.tools
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ModelInfo
import org.jetbrains.compose.resources.stringResource

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
    modifier: Modifier = Modifier,
) {
    var isInputExpanded by remember { mutableStateOf(value = false) }

    LaunchedEffect(input) {
        if (input.isBlank()) {
            isInputExpanded = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!isModelReady && !isStreaming) {
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
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    BasicTextField(
                        value = input,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = if (isInputExpanded) 200.dp else 40.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        enabled = isModelReady,
                        maxLines = if (isInputExpanded) 30 else 4,
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = if (isInputExpanded) Alignment.TopStart else Alignment.CenterStart,
                                modifier = Modifier.padding(top = if (isInputExpanded) 8.dp else 0.dp)
                            ) {
                                if (input.isEmpty()) {
                                    Text(
                                        text = if (isModelReady) stringResource(Res.string.ask_glee) else stringResource(
                                            Res.string.model_loading
                                        ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.6f
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (input.isNotEmpty()) {
                        IconButton(
                            onClick = { isInputExpanded = !isInputExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isInputExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                contentDescription = if (isInputExpanded) "Collapse" else "Expand",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
                        Icon(Icons.Default.Add, stringResource(Res.string.attach))
                    }
                    IconButton(onClick = onInspectorClick) {
                        Icon(Icons.Default.Tune, stringResource(Res.string.tools))
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

                    val sendButtonEnabled = (isStreaming || input.isNotBlank() || attachedFiles.isNotEmpty()) && isModelReady
                    IconButton(
                        onClick = {
                            if (isStreaming) {
                                onStop()
                            } else {
                                onSend()
                            }
                        },
                        enabled = sendButtonEnabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (isStreaming) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                            stringResource(Res.string.action),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.ai_disclosure),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
