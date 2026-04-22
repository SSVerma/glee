package `in`.ssverma.glee.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.select_model
import `in`.ssverma.glee.domain.model.ModelDownloadStatus
import `in`.ssverma.glee.domain.model.ModelInfo
import org.jetbrains.compose.resources.stringResource

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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.5f
        ),
        border = if (isSelected) BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.primary
        ) else null,
        modifier = modifier
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
