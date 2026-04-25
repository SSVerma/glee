package `in`.ssverma.glee.features.chat.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    return {
        onResult(true) // No-op for other platforms
    }
}
