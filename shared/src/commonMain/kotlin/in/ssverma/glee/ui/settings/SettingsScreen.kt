package `in`.ssverma.glee.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.*
import `in`.ssverma.glee.domain.model.ThemeMode
import `in`.ssverma.glee.ui.chat.ChatIntent
import `in`.ssverma.glee.ui.chat.ChatViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onModelManagement: () -> Unit,
    onManageSkills: () -> Unit,
    viewModel: ChatViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings)) },
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
                SettingsToggleItem(
                    title = stringResource(Res.string.adaptive_colors),
                    subtitle = stringResource(Res.string.adaptive_colors_desc),
                    isEnabled = uiState.isAdaptiveColorsEnabled,
                    onToggle = { viewModel.onIntent(ChatIntent.SetAdaptiveColors(it)) }
                )
            }

            item {
                SettingsHeader(stringResource(Res.string.model_and_intelligence))
                
                OutlinedTextField(
                    value = uiState.hfToken,
                    onValueChange = { viewModel.onIntent(ChatIntent.UpdateHfToken(it)) },
                    label = { Text(stringResource(Res.string.hugging_face_token)) },
                    placeholder = { Text(stringResource(Res.string.enter_token_placeholder)) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                )

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
                    onClick = { }
                )
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
    onDismiss: () -> Unit
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        RadioButton(selected = isSelected, onClick = null)
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
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
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = isEnabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun SettingsClickItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
}
