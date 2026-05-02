package `in`.ssverma.glee.features.chat.data.remote

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import `in`.ssverma.glee.features.models.ModelDownloadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import okio.Path

class AndroidModelDownloader(
    private val context: Context
) : ModelDownloader {

    private val workManager: WorkManager by lazy {
        WorkManager.getInstance(context.applicationContext)
    }

    override fun downloadModel(
        url: String,
        targetPath: Path,
        token: String?
    ): Flow<DownloadStatus> = channelFlow {
        val modelId = targetPath.name.removeSuffix(".litertlm")
        val uniqueWorkName = "download-$modelId"

        val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(
                workDataOf(
                    ModelDownloadWorker.KEY_URL to url,
                    ModelDownloadWorker.KEY_TARGET_PATH to targetPath.toString(),
                    ModelDownloadWorker.KEY_TOKEN to token,
                    ModelDownloadWorker.KEY_MODEL_NAME to modelId
                )
            )
            .build()

        // Use KEEP to avoid restarting if already running, allowing "re-attachment"
        runCatching {
            android.util.Log.d("AndroidModelDownloader", "Enqueuing unique work: $uniqueWorkName")
            workManager.enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }.onFailure {
            android.util.Log.e("AndroidModelDownloader", "Failed to enqueue unique work", it)
        }

        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName).collect { workInfos ->
            val workInfo = workInfos.firstOrNull { !it.state.isFinished } ?: workInfos.firstOrNull()
            android.util.Log.d(
                "AndroidModelDownloader",
                "Work status for $modelId: ${workInfo?.state}"
            )

            when (workInfo?.state) {
                WorkInfo.State.ENQUEUED,
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getFloat(ModelDownloadWorker.KEY_PROGRESS, 0f)
                    send(DownloadStatus.Progress(progress))
                }

                WorkInfo.State.SUCCEEDED -> {
                    send(DownloadStatus.Success(targetPath))
                    close()
                }

                WorkInfo.State.FAILED -> {
                    val error = workInfo.outputData.getString(ModelDownloadWorker.KEY_ERROR)
                        ?: "Download failed"
                    send(DownloadStatus.Error(error))
                    close()
                }

                WorkInfo.State.CANCELLED -> {
                    send(DownloadStatus.Error("Download cancelled"))
                    close()
                }

                else -> {}
            }
        }
    }

    override fun observeDownload(modelId: String): Flow<DownloadStatus> = channelFlow {
        val uniqueWorkName = "download-$modelId"

        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName).collect { workInfos ->
            val workInfo = workInfos.firstOrNull { !it.state.isFinished } ?: return@collect

            when (workInfo.state) {
                WorkInfo.State.ENQUEUED,
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getFloat(ModelDownloadWorker.KEY_PROGRESS, 0f)
                    send(DownloadStatus.Progress(progress))
                }

                WorkInfo.State.SUCCEEDED -> {
                    // We don't have the path here easily, but Success triggers repo refresh anyway
                    close()
                }

                WorkInfo.State.FAILED,
                WorkInfo.State.CANCELLED -> {
                    close()
                }

                else -> {}
            }
        }
    }

    override fun cancelDownload(modelId: String) {
        val uniqueWorkName = "download-$modelId"
        workManager.cancelUniqueWork(uniqueWorkName)
    }
}
