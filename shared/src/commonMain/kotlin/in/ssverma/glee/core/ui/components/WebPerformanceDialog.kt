package `in`.ssverma.glee.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.continue_on_web
import glee.shared.generated.resources.download_android_app
import glee.shared.generated.resources.get_for_desktop
import glee.shared.generated.resources.get_glee_app
import glee.shared.generated.resources.get_on_iphone
import glee.shared.generated.resources.non_signed_warning
import glee.shared.generated.resources.web_performance_warning
import `in`.ssverma.glee.core.common.platform.OsType
import `in`.ssverma.glee.core.common.platform.PlatformType
import `in`.ssverma.glee.core.common.platform.UrlLauncher
import `in`.ssverma.glee.core.common.platform.getOsType
import `in`.ssverma.glee.core.common.platform.getPlatformType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun WebPerformanceDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    val platform = remember { getPlatformType() }
    if (platform != PlatformType.WasmJs && platform != PlatformType.Js) return

    val urlLauncher: UrlLauncher = koinInject()
    val osType = remember { getOsType() }
    var showNonSignedNote by remember { mutableStateOf(false) }

    val downloadBase = "https://glee-ai.web.app/download"
    val macUrl = "$downloadBase/mac"
    val winUrl = "$downloadBase/windows"
    val linuxUrl = "$downloadBase/linux"
    val playStoreUrl = "https://play.google.com/store/apps/details?id=in.ssverma.glee"
    val iosUrl = "https://testflight.apple.com/join/placeholder" // TODO: Replace with real link

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(Res.string.get_glee_app)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(Res.string.web_performance_warning),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (showNonSignedNote) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Desktop and iOS builds are currently non-signed. You may need to bypass system security warnings to install them.",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                @OptIn(ExperimentalLayoutApi::class)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Android - Primary Priority
                    Button(
                        onClick = { urlLauncher.launchUrl(playStoreUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(Icons.Default.Shop, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(Res.string.download_android_app),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val showExpandButton = true
                    var showOtherDesktops by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Desktop - Outlined
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = {
                                    val url = when (osType) {
                                        OsType.Windows -> winUrl
                                        OsType.Linux -> linuxUrl
                                        else -> macUrl
                                    }
                                    urlLauncher.launchUrl(url)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                val icon = when (osType) {
                                    OsType.Windows -> Icons.Default.Window
                                    OsType.Linux -> Icons.Default.Computer
                                    else -> Icons.Default.Computer
                                }
                                Icon(icon, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.get_for_desktop), fontSize = 13.sp)
                            }

                            if (showExpandButton) {
                                TextButton(
                                    onClick = { showOtherDesktops = !showOtherDesktops },
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    contentPadding = PaddingValues(
                                        vertical = 4.dp,
                                        horizontal = 8.dp
                                    )
                                ) {
                                    val icon =
                                        if (showOtherDesktops) Icons.Default.ExpandLess else Icons.Default.ExpandMore
                                    Icon(icon, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Other systems",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        // iOS - Outlined
                        OutlinedButton(
                            onClick = { urlLauncher.launchUrl(iosUrl) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Icon(Icons.Default.IosShare, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.get_on_iphone), fontSize = 13.sp)
                        }
                    }

                    AnimatedVisibility(
                        visible = showOtherDesktops,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (osType != OsType.Mac) {
                                SecondaryDownloadButton(
                                    text = "Mac (.dmg)",
                                    onClick = { urlLauncher.launchUrl(macUrl) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (osType != OsType.Windows) {
                                SecondaryDownloadButton(
                                    text = "Windows (.msi)",
                                    onClick = { urlLauncher.launchUrl(winUrl) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (osType != OsType.Linux) {
                                SecondaryDownloadButton(
                                    text = "Linux (.deb)",
                                    onClick = { urlLauncher.launchUrl(linuxUrl) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Info link for non-signed warning
                    TextButton(
                        onClick = { showNonSignedNote = !showNonSignedNote },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(Res.string.non_signed_warning),
                            style = MaterialTheme.typography.labelSmall
                        )
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

@Composable
private fun SecondaryDownloadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text, fontSize = 10.sp, maxLines = 1)
    }
}
