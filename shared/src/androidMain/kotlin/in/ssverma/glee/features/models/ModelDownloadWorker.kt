package `in`.ssverma.glee.features.models

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import `in`.ssverma.glee.features.chat.data.remote.DownloadStatus
import `in`.ssverma.glee.features.chat.data.remote.KtorModelDownloader
import okio.Path.Companion.toPath
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val modelDownloader: KtorModelDownloader by inject()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_URL = "url"
        const val KEY_TARGET_PATH = "target_path"
        const val KEY_TOKEN = "token"
        const val KEY_MODEL_NAME = "model_name"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"

        private const val CHANNEL_ID = "model_download"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val targetPath = inputData.getString(KEY_TARGET_PATH)?.toPath() ?: return Result.failure()
        val token = inputData.getString(KEY_TOKEN)
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: "Model"

        createNotificationChannel()

        runCatching {
            setForeground(createForegroundInfo(modelName = modelName, progress = 0))
        }.onFailure {
            Log.e("ModelDownloadWorker", "Failed to set foreground", it)
        }

        var lastNotifiedProgress = -1

        return try {
            modelDownloader.downloadModel(url, targetPath, token).collect { status ->
                when (status) {
                    is DownloadStatus.Progress -> {
                        val progress = (status.progress * 100).toInt()

                        // Sync both UI and Notification at 1% increments
                        // This reduces log pollution while remaining smooth and perfectly in sync
                        if (progress > lastNotifiedProgress || progress >= 100) {
                            lastNotifiedProgress = progress
                            setProgress(workDataOf(KEY_PROGRESS to status.progress))

                            val notification = createNotification(modelName, progress)
                            runCatching {
                                setForeground(createForegroundInfo(modelName, progress))
                            }
                            notificationManager.notify(NOTIFICATION_ID, notification)
                        }
                    }

                    is DownloadStatus.Success -> {
                        // Success handled by progress observers or repository
                    }

                    is DownloadStatus.Error -> {
                        throw Exception(status.message)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    private fun createForegroundInfo(modelName: String, progress: Int): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                createNotification(modelName, progress),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, createNotification(modelName, progress))
        }
    }

    private fun createNotification(modelName: String, progress: Int): Notification {
        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        val mainActivityIntent = applicationContext.packageManager.getLaunchIntentForPackage(
            applicationContext.packageName
        )?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = applicationContext.applicationInfo.icon
        val smallIcon = if (icon != 0) icon else android.R.drawable.stat_sys_download

        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading $modelName")
            .setContentText("$progress%")
            .setSmallIcon(smallIcon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(100, progress, false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Download",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
