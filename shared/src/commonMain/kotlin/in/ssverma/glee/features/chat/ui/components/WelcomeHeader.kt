package `in`.ssverma.glee.features.chat.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.available_offline
import glee.shared.generated.resources.meet_glee
import glee.shared.generated.resources.temporary_chat
import glee.shared.generated.resources.temporary_chat_desc
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeHeader(
    isPrivateMode: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPrivateMode) {
        PrivateWelcomeHeader(modifier)
    } else {
        StandardWelcomeHeader(modifier)
    }
}

@Composable
private fun StandardWelcomeHeader(modifier: Modifier = Modifier) {
    val primaryGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 80.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val welcomeText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                append("Hi, How can ")
            }
            withStyle(
                style = SpanStyle(
                    brush = primaryGradient,
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append("Glee")
            }
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                append(" help you today?")
            }
        }

        Text(
            text = welcomeText,
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("Your ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                    append("Offline")
                }
                append(" and ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                    append("Private")
                }
                append(" AI assistant")
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PrivateWelcomeHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(top = 80.dp, bottom = 48.dp)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.temporary_chat),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.temporary_chat_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
