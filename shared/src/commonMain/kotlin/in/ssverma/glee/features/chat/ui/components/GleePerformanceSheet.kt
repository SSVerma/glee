package `in`.ssverma.glee.features.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.context_window
import glee.shared.generated.resources.context_window_value
import glee.shared.generated.resources.latency
import glee.shared.generated.resources.latency_value
import glee.shared.generated.resources.no_model
import glee.shared.generated.resources.performance
import glee.shared.generated.resources.ram_usage
import glee.shared.generated.resources.ram_usage_value
import `in`.ssverma.glee.features.chat.domain.model.ChatMetrics
import org.jetbrains.compose.resources.stringResource

@Composable
fun GleePerformanceSheet(
    metrics: ChatMetrics,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
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
                text = stringResource(Res.string.performance),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (metrics.latencyMs > 0) Color(0xFF4CAF50) else Color.Gray)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = metrics.modelName.ifEmpty { stringResource(Res.string.no_model) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val contextValue = if (metrics.contextUsed < 1000) {
                    "${metrics.contextUsed} / ${metrics.contextMax / 1000}K"
                } else {
                    stringResource(
                        Res.string.context_window_value,
                        metrics.contextUsed / 1000,
                        metrics.contextMax / 1000
                    )
                }

                PerformanceMetricItem(
                    label = stringResource(Res.string.context_window),
                    icon = Icons.Default.Dns,
                    value = contextValue,
                    progress = metrics.contextUsed.toFloat() / metrics.contextMax,
                    color = MaterialTheme.colorScheme.primary
                )

                PerformanceMetricItem(
                    label = stringResource(Res.string.ram_usage),
                    icon = Icons.Default.Memory,
                    value = stringResource(
                        Res.string.ram_usage_value,
                        ((metrics.ramUsedGb * 10).toInt() / 10.0).toString(),
                        metrics.ramTotalGb.toInt()
                    ),
                    progress = metrics.ramUsedGb / metrics.ramTotalGb,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Speed,
                        null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Column {
                        Text(
                            text = stringResource(Res.string.latency),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = stringResource(Res.string.latency_value, metrics.latencyMs),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceMetricItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    progress: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            strokeCap = StrokeCap.Round
        )
    }
}
