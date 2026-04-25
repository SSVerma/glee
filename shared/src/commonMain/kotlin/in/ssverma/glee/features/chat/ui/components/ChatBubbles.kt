package `in`.ssverma.glee.features.chat.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.executing_tool
import `in`.ssverma.glee.features.chat.domain.model.ChatMessage
import `in`.ssverma.glee.features.chat.domain.model.ChatRole
import `in`.ssverma.glee.core.ui.components.GleeLoadingIndicator
import `in`.ssverma.glee.markdown.GleeMarkdown
import org.jetbrains.compose.resources.stringResource

@Composable
fun MessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == ChatRole.User
    val isTool = message.role == ChatRole.Tool

    if (isTool) {
        ToolExecutionBubble(message.content, modifier)
        return
    }

    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor =
        if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val contentColor =
        if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(value = false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 48.dp else 0.dp,
                end = if (isUser) 0.dp else 48.dp
            ),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isUser) 600.dp else 800.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(containerColor)
                .padding(16.dp)
                .animateContentSize()
        ) {
            if (isUser) {
                Column {
                    Text(
                        text = message.content,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (message.content.lines().size > 4 || message.content.length > 200) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.align(Alignment.End).size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                Column {
                    GleeMarkdown(content = message.content, modifier = Modifier.fillMaxWidth())
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(message.content))
                                onCopy()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = contentColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolExecutionBubble(
    content: String,
    modifier: Modifier = Modifier,
) {
    val isCompleted = content.startsWith("Completed:")
    val displayContent = if (isCompleted) content.removePrefix("Completed:").trim() else content.removePrefix("Executing:").trim()

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!isCompleted) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isCompleted) "Completed: ${displayContent.take(30)}" else stringResource(Res.string.executing_tool, displayContent.take(30)),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StreamingBubble(
    content: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            modifier = Modifier.widthIn(max = 800.dp).padding(end = 48.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (content.isEmpty()) {
                    GleeLoadingIndicator(modifier = Modifier.size(24.dp))
                } else {
                    GleeMarkdown(content = content, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
