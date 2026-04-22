package `in`.ssverma.glee.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.*
import `in`.ssverma.glee.domain.model.GleeModelConfig
import org.jetbrains.compose.resources.stringResource

@Composable
fun GleeIntelligenceSheet(
    config: GleeModelConfig,
    systemPrompt: String,
    onConfigChange: (GleeModelConfig) -> Unit,
    onUpdateSystemPrompt: (String) -> Unit,
    onRestoreDefaultPrompt: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
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

        SheetSection(title = stringResource(Res.string.system_prompt)) {
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = onUpdateSystemPrompt,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            )
            TextButton(
                onClick = onRestoreDefaultPrompt,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = stringResource(Res.string.restore_defaults),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        SheetSection(title = stringResource(Res.string.model_config)) {
            ConfigSliderItem(
                label = stringResource(Res.string.temperature),
                value = config.temperature,
                range = 0f..1.5f,
                onValueChange = { onConfigChange(config.copy(temperature = it)) }
            )

            ConfigSliderItem(
                label = stringResource(Res.string.top_k),
                value = config.topK.toFloat(),
                range = 1f..100f,
                onValueChange = { onConfigChange(config.copy(topK = it.toInt())) }
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.use_gpu),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Switch(
                        checked = config.useGpu,
                        onCheckedChange = { onConfigChange(config.copy(useGpu = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp
        )
        content()
    }
}

@Composable
private fun ConfigSliderItem(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
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
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
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
