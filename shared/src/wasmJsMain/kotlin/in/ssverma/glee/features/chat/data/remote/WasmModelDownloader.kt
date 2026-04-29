@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package `in`.ssverma.glee.features.chat.data.remote

import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import okio.Path
import kotlin.js.Promise

@JsFun(
    """
async function downloadToOpfsJs(url, fileName, token, onProgress) {
    const headers = {};
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    
    const response = await window.fetch(url, { headers });
    if (!response.ok) throw new Error("HTTP " + response.status);
    const contentLength = response.headers.get('content-length');
    const total = contentLength ? parseInt(contentLength, 10) : 0;
    
    const dir = await navigator.storage.getDirectory();
    const fileHandle = await dir.getFileHandle(fileName, { create: true });
    const writable = await fileHandle.createWritable();
    
    const reader = response.body.getReader();
    let loaded = 0;
    while(true) {
        const { done, value } = await reader.read();
        if (done) break;
        loaded += value.length;
        if (total > 0) {
            onProgress(loaded / total);
        }
        await writable.write(value);
    }
    await writable.close();
}
"""
)

external fun downloadToOpfsJs(
    url: String,
    fileName: String,
    token: String?,
    onProgress: (Float) -> Unit
): Promise<JsAny?>

class WasmModelDownloader : ModelDownloader {
    override fun downloadModel(
        url: String,
        targetPath: Path,
        token: String?
    ): Flow<DownloadStatus> = channelFlow {
        send(DownloadStatus.Progress(0f))
        try {
            val fileName = targetPath.name
            downloadToOpfsJs(url, fileName, token) { progress ->
                trySend(DownloadStatus.Progress(progress))
            }.await<JsAny?>()

            send(DownloadStatus.Success(targetPath))
        } catch (e: Throwable) {
            send(DownloadStatus.Error(e.message ?: "Download failed"))
        }
    }
}
