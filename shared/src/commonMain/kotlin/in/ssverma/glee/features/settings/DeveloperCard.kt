package `in`.ssverma.glee.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperCard(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val containerColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val glowBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.1f),
                        secondaryColor.copy(alpha = 0.15f),
                        tertiaryColor.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
                drawRect(
                    brush = glowBrush,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 2.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, 8.dp.toPx())
                )
            }
    ) {
        OutlinedCard(
            onClick = { uriHandler.openUri("https://x.com/ssverma1916") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.15f)),
            colors = CardDefaults.outlinedCardColors(
                containerColor = containerColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(2.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    AsyncImage(
                        model = "https://pbs.twimg.com/profile_images/1807349302164934656/xELoSQEH_400x400.jpg",
                        contentDescription = "SS Verma",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Crafted with ❤️ by",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "SS Verma",
                        style = MaterialTheme.typography.titleLarge.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                Spacer(Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Follow",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
