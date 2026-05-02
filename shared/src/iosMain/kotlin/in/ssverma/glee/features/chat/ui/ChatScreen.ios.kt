package `in`.ssverma.glee.features.chat.ui

import androidx.compose.runtime.Composable

import `in`.ssverma.glee.core.common.platform.PermissionType

@Composable
actual fun rememberPermissionLauncher(
    permissionType: PermissionType,
    onResult: (Boolean) -> Unit
): () -> Unit {
    return {
        onResult(true)
    }
}
