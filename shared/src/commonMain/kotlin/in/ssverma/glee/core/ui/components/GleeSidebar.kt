package `in`.ssverma.glee.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.app_name
import glee.shared.generated.resources.help
import glee.shared.generated.resources.manage_skills
import glee.shared.generated.resources.model_management
import glee.shared.generated.resources.models
import glee.shared.generated.resources.new_chat
import glee.shared.generated.resources.recent_conversations
import glee.shared.generated.resources.settings
import `in`.ssverma.glee.features.chat.domain.model.Conversation
import org.jetbrains.compose.resources.stringResource

@Composable
fun GleeSidebar(
    conversations: List<Conversation>,
    selectedConversationId: String?,
    onConversationClick: (Conversation) -> Unit,
    onDeleteConversation: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    onModelManagement: () -> Unit,
    onManageSkills: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SidebarActionItem(
                icon = Icons.Default.Add,
                label = stringResource(Res.string.new_chat),
                onClick = onNewChat,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (conversations.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.recent_conversations),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            isSelected = conversation.id == selectedConversationId,
                            onClick = { onConversationClick(conversation) },
                            onDelete = { onDeleteConversation(conversation) }
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = stringResource(Res.string.models).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            SidebarActionItem(
                icon = Icons.Default.SmartToy,
                label = stringResource(Res.string.model_management),
                onClick = onModelManagement
            )

            SidebarActionItem(
                icon = Icons.Default.Construction,
                label = stringResource(Res.string.manage_skills),
                onClick = onManageSkills
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SidebarActionItem(
                icon = Icons.Default.Settings,
                label = stringResource(Res.string.settings),
                onClick = onSettings
            )

            SidebarActionItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                label = stringResource(Res.string.help),
                onClick = { }
            )
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = conversation.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SidebarActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
