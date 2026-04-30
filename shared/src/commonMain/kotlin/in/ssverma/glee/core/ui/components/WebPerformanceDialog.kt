package `in`.ssverma.glee.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.get_glee_app
import glee.shared.generated.resources.web_performance_warning
import glee.shared.generated.resources.download_for_desktop
import glee.shared.generated.resources.get_on_play_store
import glee.shared.generated.resources.continue_on_web
import `in`.ssverma.glee.core.common.platform.PlatformType
import `in`.ssverma.glee.core.common.platform.getPlatformType
import `in`.ssverma.glee.core.common.platform.UrlLauncher
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun WebPerformanceDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    val platform = remember { getPlatformType() }
    if (platform != PlatformType.WasmJs && platform != PlatformType.Js) return

    val urlLauncher: UrlLauncher = koinInject()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(Res.string.get_glee_app)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.web_performance_warning))
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { urlLauncher.launchUrl("https://github.com/ssverma/Glee/releases/latest") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.download_for_desktop))
                    }
                    Button(
                        onClick = { urlLauncher.launchUrl("https://play.google.com/store/apps/details?id=in.ssverma.glee") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.get_on_play_store))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(Res.string.continue_on_web))
                }
            }
        )
    }
}
