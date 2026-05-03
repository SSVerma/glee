package `in`.ssverma.glee.features.chat.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import `in`.ssverma.glee.core.common.platform.PermissionType

@Composable
actual fun rememberPermissionLauncher(
    permissionType: PermissionType,
    onResult: (Boolean) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val updatedOnResult = rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updatedOnResult.value(isGranted)
    }

    val permission = when (permissionType) {
        PermissionType.RecordAudio -> android.Manifest.permission.RECORD_AUDIO
        PermissionType.Notifications -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.POST_NOTIFICATIONS
            } else {
                null
            }
        }
    }

    return {
        if (permission != null) {
            launcher.launch(permission)
        } else {
            onResult(true)
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
