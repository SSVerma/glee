package `in`.ssverma.glee.features.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.backend_auto
import glee.shared.generated.resources.cancel
import glee.shared.generated.resources.done
import glee.shared.generated.resources.hardware_accelerator
import glee.shared.generated.resources.intelligence
import glee.shared.generated.resources.low_end_device_warning_desc
import glee.shared.generated.resources.low_end_device_warning_title
import glee.shared.generated.resources.model_backend
import glee.shared.generated.resources.model_backend_info
import glee.shared.generated.resources.model_config
import glee.shared.generated.resources.proceed_anyway
import glee.shared.generated.resources.restore
import glee.shared.generated.resources.save
import glee.shared.generated.resources.system_prompt
import glee.shared.generated.resources.system_prompt_info
import glee.shared.generated.resources.temperature
import glee.shared.generated.resources.temperature_info
import glee.shared.generated.resources.top_k
import glee.shared.generated.resources.top_k_info
import `in`.ssverma.glee.features.chat.domain.model.BackendType
import `in`.ssverma.glee.features.chat.domain.model.GleeModelConfig
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GleeIntelligenceSheet(
    config: GleeModelConfig,
    systemPrompt: String,
    onConfigChange: (GleeModelConfig) -> Unit,
    onUpdateSystemPrompt: (String) -> Unit,
    onRestoreDefaultPrompt: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    isLowConstraintDevice: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var pendingBackend by remember { mutableStateOf<BackendType?>(null) }
    var infoTitle by remember { mutableStateOf<StringResource?>(null) }
    var infoText by remember { mutableStateOf<StringResource?>(null) }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(Res.string.intelligence),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            SheetSection(
                title = stringResource(Res.string.system_prompt),
                onRestore = onRestoreDefaultPrompt,
                onInfoClick = {
                    infoTitle = Res.string.system_prompt
                    infoText = Res.string.system_prompt_info
                }
            ) {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = onUpdateSystemPrompt,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.2f
                        )
                    )
                )
            }

            SheetSection(
                title = stringResource(Res.string.model_config),
                onRestore = { onConfigChange(GleeModelConfig()) }
            ) {
                ConfigSliderItem(
                    label = stringResource(Res.string.temperature),
                    value = config.temperature,
                    range = 0f..1.5f,
                    onValueChange = { onConfigChange(config.copy(temperature = it)) },
                    onInfoClick = {
                        infoTitle = Res.string.temperature
                        infoText = Res.string.temperature_info
                    }
                )

                ConfigSliderItem(
                    label = stringResource(Res.string.top_k),
                    value = config.topK.toFloat(),
                    range = 1f..100f,
                    onValueChange = { onConfigChange(config.copy(topK = it.toInt())) },
                    onInfoClick = {
                        infoTitle = Res.string.top_k
                        infoText = Res.string.top_k_info
                    }
                )

                SheetSection(
                    title = stringResource(Res.string.model_backend),
                    onInfoClick = {
                        infoTitle = Res.string.hardware_accelerator
                        infoText = Res.string.model_backend_info
                    }
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BackendChip(
                            label = stringResource(Res.string.backend_auto),
                            selected = config.preferredBackend == BackendType.Auto,
                            onClick = { onConfigChange(config.copy(preferredBackend = BackendType.Auto)) }
                        )
                        BackendChip(
                            label = "NPU",
                            selected = config.preferredBackend == BackendType.Npu,
                            onClick = {
                                if (isLowConstraintDevice) {
                                    pendingBackend = BackendType.Npu
                                } else {
                                    onConfigChange(config.copy(preferredBackend = BackendType.Npu))
                                }
                            }
                        )
                        BackendChip(
                            label = "GPU",
                            selected = config.preferredBackend == BackendType.Gpu,
                            onClick = {
                                if (isLowConstraintDevice) {
                                    pendingBackend = BackendType.Gpu
                                } else {
                                    onConfigChange(config.copy(preferredBackend = BackendType.Gpu))
                                }
                            }
                        )
                        BackendChip(
                            label = "CPU",
                            selected = config.preferredBackend == BackendType.Cpu,
                            onClick = { onConfigChange(config.copy(preferredBackend = BackendType.Cpu)) }
                        )
                    }
                }
            }
        }

        if (pendingBackend != null) {
            AlertDialog(
                onDismissRequest = { pendingBackend = null },
                title = { Text(stringResource(Res.string.low_end_device_warning_title)) },
                text = { Text(stringResource(Res.string.low_end_device_warning_desc)) },
                confirmButton = {
                    Button(
                        onClick = {
                            onConfigChange(config.copy(preferredBackend = pendingBackend!!))
                            pendingBackend = null
                        }
                    ) {
                        Text(stringResource(Res.string.proceed_anyway))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingBackend = null }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(Res.string.save), fontWeight = FontWeight.Bold)
        }

        if (infoTitle != null && infoText != null) {
            AlertDialog(
                onDismissRequest = {
                    infoTitle = null
                    infoText = null
                },
                title = { Text(stringResource(infoTitle!!)) },
                text = { Text(stringResource(infoText!!)) },
                confirmButton = {
                    TextButton(onClick = {
                        infoTitle = null
                        infoText = null
                    }) {
                        Text(stringResource(Res.string.done))
                    }
                }
            )
        }
    }
}

@Composable
private fun BackendChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = null
    )
}

@Composable
private fun SheetSection(
    title: String,
    onRestore: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                if (onInfoClick != null) {
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            if (onRestore != null) {
                OutlinedButton(
                    onClick = onRestore,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp),
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 0.5.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.restore),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        content()
    }
}

@Composable
private fun ConfigSliderItem(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onInfoClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (onInfoClick != null) {
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = ((value * 100).toInt() / 100.0).toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
