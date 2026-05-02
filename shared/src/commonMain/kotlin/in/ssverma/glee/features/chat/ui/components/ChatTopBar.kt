package `in`.ssverma.glee.features.chat.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ssverma.glee.core.common.platform.getPlatformType
import `in`.ssverma.glee.core.common.platform.PlatformType
import `in`.ssverma.glee.features.chat.domain.model.ChatMetrics
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    showMenuIcon: Boolean,
    isPrivateMode: Boolean,
    onMenuClick: () -> Unit,
    onTogglePrivate: () -> Unit,
    onNewChat: () -> Unit,
    metrics: ChatMetrics,
    onDownloadAppsClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actionsEnabled: Boolean = true
) {
    val platform = getPlatformType()
    val isWeb = platform == PlatformType.WasmJs || platform == PlatformType.Js

    CenterAlignedTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.app_name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (actionsEnabled && metrics.ramUsedGb > 0) {
                    AssistChip(
                        onClick = {},
                        label = {
                            val latencySec = metrics.latencyMs / 1000f
                            val latencyText = if (latencySec < 0.1f) "<0.1s" else "${latencySec.toString().take(3)}s"
                            Text(
                                text = "${metrics.ramUsedGb.toString().take(3)}GB | $latencyText",
                                fontSize = 10.sp
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null,
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
        },
        navigationIcon = {
            if (showMenuIcon) {
                IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null) }
            }
        },
        actions = {
            if (actionsEnabled) {
                if (isWeb) {
                    IconButton(onClick = onDownloadAppsClick) {
                        Icon(Icons.Default.Download, null)
                    }
                }
                IconButton(onClick = onNewChat) {
                    Icon(Icons.Default.Add, null)
                }
                IconButton(onClick = onTogglePrivate) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isPrivateMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    )
}
