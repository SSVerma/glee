package `in`.ssverma.glee.features.chat.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    CenterAlignedTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                text = stringResource(Res.string.app_name),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        navigationIcon = {
            if (showMenuIcon) {
                IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null) }
            }
        },
        actions = {
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
    )
}
