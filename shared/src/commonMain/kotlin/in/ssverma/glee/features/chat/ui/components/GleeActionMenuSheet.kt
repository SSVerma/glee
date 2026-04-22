package `in`.ssverma.glee.features.chat.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glee.shared.generated.resources.Res
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
    onSelectAction: (ChatActionSheetType) -> Unit,
    modifier: Modifier = Modifier
) {
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
            onClick = { onSelectAction(ChatActionSheetType.Skills) }
        )
    }
}

@Composable
private fun ActionMenuItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
