package `in`.ssverma.glee.ui.chat.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.executing_tool
import `in`.ssverma.glee.domain.model.ChatMessage
import `in`.ssverma.glee.domain.model.ChatRole
import `in`.ssverma.glee.markdown.GleeMarkdown
import org.jetbrains.compose.resources.stringResource

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
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

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isUser) 600.dp else 800.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(containerColor)
                .padding(16.dp)
        ) {
            if (isUser) {
                Text(
                    text = message.content,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                GleeMarkdown(content = message.content, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ToolExecutionBubble(
    content: String,
    modifier: Modifier = Modifier
) {
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
            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.executing_tool, content.take(30)),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun StreamingBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            modifier = Modifier.widthIn(max = 800.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (content.isEmpty()) {
                    TypingIndicator()
                } else {
                    GleeMarkdown(content = content, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "dot1"
    )
    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .heightIn(min = 24.dp), // Stable height matching text
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(8.dp).alpha(dotAlpha1)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Box(
            Modifier.size(8.dp).alpha(dotAlpha2)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Box(
            Modifier.size(8.dp).alpha(dotAlpha3)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}
