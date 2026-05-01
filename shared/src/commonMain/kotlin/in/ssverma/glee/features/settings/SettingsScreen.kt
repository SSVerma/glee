package `in`.ssverma.glee.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.about
import glee.shared.generated.resources.adaptive_colors
import glee.shared.generated.resources.appearance
import glee.shared.generated.resources.dark
import glee.shared.generated.resources.light
import glee.shared.generated.resources.manage_skills
import glee.shared.generated.resources.manage_skills_desc
import glee.shared.generated.resources.model_and_intelligence
import glee.shared.generated.resources.model_management
import glee.shared.generated.resources.model_management_desc
import glee.shared.generated.resources.settings
import glee.shared.generated.resources.system_default
import glee.shared.generated.resources.theme
import glee.shared.generated.resources.version
import glee.shared.generated.resources.version_desc
import `in`.ssverma.glee.core.common.platform.PlatformType
import `in`.ssverma.glee.core.common.platform.getPlatformType
import `in`.ssverma.glee.features.chat.domain.model.ThemeMode
import `in`.ssverma.glee.features.chat.ui.ChatIntent
import `in`.ssverma.glee.features.chat.ui.ChatViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onModelManagement: () -> Unit,
    onManageSkills: () -> Unit,
    viewModel: ChatViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeSheet by remember { mutableStateOf(false) }
    val platformType = remember { getPlatformType() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings)) },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            item {
                SettingsHeader(stringResource(Res.string.appearance))
                SettingsClickItem(
                    title = stringResource(Res.string.theme),
                    subtitle = when (uiState.themeMode) {
                        ThemeMode.System -> stringResource(Res.string.system_default)
                        ThemeMode.Light -> stringResource(Res.string.light)
                        ThemeMode.Dark -> stringResource(Res.string.dark)
                    },
                    icon = Icons.Default.Palette,
                    onClick = { showThemeSheet = true }
                )

                if (platformType == PlatformType.Android) {
                    SettingsToggleItem(
                        title = stringResource(Res.string.adaptive_colors),
                        subtitle = "Sync Glee colors with your system wallpaper.",
                        isEnabled = uiState.isAdaptiveColorsEnabled,
                        onToggle = { viewModel.onIntent(ChatIntent.SetAdaptiveColors(it)) }
                    )
                }
            }

            item {
                SettingsHeader(stringResource(Res.string.model_and_intelligence))

                SettingsClickItem(
                    title = stringResource(Res.string.model_management),
                    subtitle = stringResource(Res.string.model_management_desc),
                    icon = Icons.Default.ModelTraining,
                    onClick = onModelManagement
                )
                SettingsClickItem(
                    title = stringResource(Res.string.manage_skills),
                    subtitle = stringResource(Res.string.manage_skills_desc),
                    icon = Icons.Default.Extension,
                    onClick = onManageSkills
                )
            }

            item {
                SettingsHeader(stringResource(Res.string.about))
                SettingsClickItem(
                    title = stringResource(Res.string.version),
                    subtitle = stringResource(Res.string.version_desc),
                    icon = Icons.Default.Info,
                    showChevron = false,
                    onClick = { }
                )

                DeveloperCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                GithubCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(Modifier.height(32.dp))
            }
        }

        if (showThemeSheet) {
            ThemeSelectionBottomSheet(
                currentMode = uiState.themeMode,
                onModeSelected = {
                    viewModel.onIntent(ChatIntent.SetThemeMode(it))
                    showThemeSheet = false
                },
                onDismiss = { showThemeSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionBottomSheet(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = stringResource(Res.string.theme),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            ThemeOption(
                title = stringResource(Res.string.system_default),
                isSelected = currentMode == ThemeMode.System,
                onClick = { onModeSelected(ThemeMode.System) }
            )
            ThemeOption(
                title = stringResource(Res.string.light),
                isSelected = currentMode == ThemeMode.Light,
                onClick = { onModeSelected(ThemeMode.Light) }
            )
            ThemeOption(
                title = stringResource(Res.string.dark),
                isSelected = currentMode == ThemeMode.Dark,
                onClick = { onModeSelected(ThemeMode.Dark) }
            )
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        RadioButton(selected = isSelected, onClick = null)
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isEnabled) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = isEnabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun SettingsClickItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showChevron) {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
