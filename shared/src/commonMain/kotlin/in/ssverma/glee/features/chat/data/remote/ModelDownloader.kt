package `in`.ssverma.glee.features.chat.data.remote

import glee.shared.generated.resources.Res
import glee.shared.generated.resources.download_failed
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import okio.FileSystem
import okio.Path
import okio.buffer
import org.jetbrains.compose.resources.getString

interface ModelDownloader {
    fun downloadModel(url: String, targetPath: Path, token: String? = null): Flow<DownloadStatus>
    fun observeDownload(modelId: String): Flow<DownloadStatus>? = null
    fun cancelDownload(modelId: String) {}
}

class KtorModelDownloader(
    private val client: HttpClient,
    private val okioFs: FileSystem
) : ModelDownloader {
    /**
     * Downloads a model file using streaming to avoid memory issues with large files.
     */
    override fun downloadModel(
        url: String,
        targetPath: Path,
        token: String?
    ): Flow<DownloadStatus> = channelFlow {
        send(DownloadStatus.Progress(0f))

        var success = false
        try {
            client.prepareGet(url) {
                // IMPORTANT: Overwrite Accept header for binary download
                header(HttpHeaders.Accept, "*/*")
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

                onDownload { bytesSentTotal, contentLength ->
                    if (contentLength != null && contentLength > 0) {
                        trySend(DownloadStatus.Progress(bytesSentTotal.toFloat() / contentLength))
                    }
                }
            }.execute { response ->
                if (response.status == HttpStatusCode.OK) {
                    targetPath.parent?.let { parent ->
                        if (!okioFs.exists(parent)) {
                            okioFs.createDirectories(parent)
                        }
                    }

                    val channel = response.bodyAsChannel()
                    val sink = okioFs.sink(targetPath).buffer()
                    try {
                        val buffer = ByteArray(128 * 1024) // 128KB buffer
                        while (!channel.isClosedForRead) {
                            val read = channel.readAvailable(buffer)
                            if (read <= 0) break
                            sink.write(buffer, 0, read)
                        }
                        sink.flush()
                        success = true
                        send(DownloadStatus.Success(targetPath))
                    } finally {
                        sink.close()
                    }
                } else {
                    send(DownloadStatus.Error(getString(Res.string.download_failed)))
                }
            }
        } catch (e: Exception) {
            // Re-throw CancellationException to allow flow cancellation
            if (e is kotlinx.coroutines.CancellationException) throw e
            send(DownloadStatus.Error(getString(Res.string.download_failed)))
        } finally {
            if (!success) {
                try {
                    if (okioFs.exists(targetPath)) {
                        okioFs.delete(targetPath)
                    }
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }
}

sealed interface DownloadStatus {
    data class Progress(val progress: Float) : DownloadStatus
    data class Success(val path: Path) : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}
