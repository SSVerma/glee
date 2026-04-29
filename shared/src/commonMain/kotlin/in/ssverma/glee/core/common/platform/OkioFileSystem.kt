package `in`.ssverma.glee.core.common.platform

import okio.FileSystem as OkioFS
import okio.Path
import okio.buffer
import okio.use

/**
 * Production-ready implementation of FileSystem using Okio.
 */
class OkioFileSystem(
    private val okioFs: OkioFS,
    override val appDataDir: Path
) : GleeFileSystem {

    override suspend fun readFile(path: Path): Result<String> = runCatching {
        okioFs.read(path) {
            readUtf8()
        }
    }

    override suspend fun writeFile(path: Path, content: String): Result<Unit> = runCatching {
        okioFs.write(path) {
            writeUtf8(content)
        }
    }

    override suspend fun writeBytes(
        path: Path,
        bytes: ByteArray,
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> = runCatching {
        okioFs.write(path) {
            val chunkSize = 64 * 1024 // 64KB chunks
            var offset = 0
            while (offset < bytes.size) {
                kotlinx.coroutines.yield()
                val length = minOf(chunkSize, bytes.size - offset)
                write(bytes, offset, length)
                emit() // Push data to the sink
                offset += length
                if (bytes.isNotEmpty()) {
                    onProgress?.invoke(offset.toFloat() / bytes.size)
                }
            }
        }
    }

    override suspend fun listFiles(path: Path): Result<List<Path>> = runCatching {
        okioFs.list(path)
    }

    override suspend fun exists(path: Path): Boolean {
        return okioFs.exists(path)
    }

    override suspend fun delete(path: Path): Result<Unit> = runCatching {
        okioFs.delete(path)
    }

    override suspend fun copyFile(
        source: Path,
        target: Path,
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> = runCatching {
        val fileSize = okioFs.metadata(source).size ?: 0L
        okioFs.source(source).use { sourceStream ->
            okioFs.sink(target).buffer().use { sinkStream ->
                val buffer = okio.Buffer()
                var totalBytesRead = 0L
                val chunkSize = 1024L * 1024L // 1MB chunks
                
                while (true) {
                    kotlinx.coroutines.yield()
                    val bytesRead = sourceStream.read(buffer, chunkSize)
                    if (bytesRead == -1L) break
                    
                    sinkStream.write(buffer, bytesRead)
                    sinkStream.flush()
                    
                    totalBytesRead += bytesRead
                    if (fileSize > 0) {
                        onProgress?.invoke(totalBytesRead.toFloat() / fileSize)
                    }
                }
            }
        }
    }
}
