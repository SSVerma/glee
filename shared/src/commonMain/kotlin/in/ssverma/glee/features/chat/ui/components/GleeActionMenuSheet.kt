package `in`.ssverma.glee.features.chat.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.agentic_mode
import glee.shared.generated.resources.agentic_mode_info
import glee.shared.generated.resources.done
import glee.shared.generated.resources.glee_tools
import glee.shared.generated.resources.intelligence
import glee.shared.generated.resources.intelligence_desc
import glee.shared.generated.resources.performance
import glee.shared.generated.resources.performance_desc
import glee.shared.generated.resources.skills
import glee.shared.generated.resources.skills_desc
import org.jetbrains.compose.resources.stringResource

enum class ChatActionSheetType {
    Root,
    Intelligence,
    Performance,
    Skills
}

@Composable
fun GleeActionMenuSheet(
    isAgentic: Boolean,
    onToggleAgentic: (Boolean) -> Unit,
    onSelectAction: (ChatActionSheetType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAgenticInfo by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.glee_tools),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.agentic_mode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { showAgenticInfo = true },
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
                Switch(
                    checked = isAgentic,
                    onCheckedChange = onToggleAgentic,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        if (showAgenticInfo) {
            AlertDialog(
                onDismissRequest = { showAgenticInfo = false },
                title = { Text(stringResource(Res.string.agentic_mode)) },
                text = { Text(stringResource(Res.string.agentic_mode_info)) },
                confirmButton = {
                    TextButton(onClick = { showAgenticInfo = false }) {
                        Text(stringResource(Res.string.done))
                    }
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        ActionMenuItem(
            icon = Icons.Default.AutoAwesome,
            title = stringResource(Res.string.intelligence),
            description = stringResource(Res.string.intelligence_desc),
            onClick = { onSelectAction(ChatActionSheetType.Intelligence) }
        )
        ActionMenuItem(
            icon = Icons.Default.BarChart,
            title = stringResource(Res.string.performance),
            description = stringResource(Res.string.performance_desc),
            onClick = { onSelectAction(ChatActionSheetType.Performance) }
        )
        ActionMenuItem(
            icon = Icons.Default.Construction,
            title = stringResource(Res.string.skills),
            description = stringResource(Res.string.skills_desc),
            enabled = isAgentic,
            onClick = { onSelectAction(ChatActionSheetType.Skills) }
        )
    }
}

@Composable
private fun ActionMenuItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.large,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    }
                )
                Text(
                    text = if (enabled) description else "Requires Agentic Mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    }
                )
            }
        }
    }
}
