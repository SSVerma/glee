package `in`.ssverma.glee.features.chat.data.remote

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path

/**
 * iOS implementation of ModelDownloader that supports observing and cancelling active downloads.
 * Uses KtorModelDownloader internally but manages the download lifecycle to persist across 
 * UI navigations within the app.
 */
class IosModelDownloader(
    private val client: HttpClient,
    private val okioFs: FileSystem
) : ModelDownloader {
    
    private val activeDownloads = mutableMapOf<String, DownloadState>()
    private val ktorDownloader = KtorModelDownloader(client, okioFs)
    private val downloaderScope = CoroutineScope(Dispatchers.Main + Job())

    private data class DownloadState(
        val job: Job,
        val flow: MutableStateFlow<DownloadStatus>
    )

    override fun downloadModel(
        url: String,
        targetPath: Path,
        token: String?
    ): Flow<DownloadStatus> {
        val modelId = targetPath.name.removeSuffix(".litertlm")
        
        // If already downloading, return the existing flow to avoid starting multiple downloads
        activeDownloads[modelId]?.let { return it.flow.asStateFlow() }

        val statusFlow = MutableStateFlow<DownloadStatus>(DownloadStatus.Progress(0f))
        
        val job = downloaderScope.launch(Dispatchers.Default) {
            ktorDownloader.downloadModel(url, targetPath, token)
                .onCompletion { 
                    activeDownloads.remove(modelId)
                }
                .collect { status ->
                    statusFlow.value = status
                }
        }

        activeDownloads[modelId] = DownloadState(job, statusFlow)
        return statusFlow.asStateFlow()
    }

    override fun observeDownload(modelId: String): Flow<DownloadStatus>? {
        return activeDownloads[modelId]?.flow?.asStateFlow()
    }

    override fun cancelDownload(modelId: String) {
        activeDownloads[modelId]?.job?.cancel()
        activeDownloads.remove(modelId)
    }
}
