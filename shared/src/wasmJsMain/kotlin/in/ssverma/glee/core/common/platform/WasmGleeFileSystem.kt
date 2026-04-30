@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package `in`.ssverma.glee.core.common.platform

import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.await
import okio.Path
import org.khronos.webgl.Int8Array
import org.khronos.webgl.set
import kotlin.js.Promise

@JsFun(
    """
async function checkOpfsFileExistsJs(fileName) {
    try {
        const dir = await navigator.storage.getDirectory();
        await dir.getFileHandle(fileName);
        return true;
    } catch(e) {
        return false;
    }
}
"""
)
internal external fun checkOpfsFileExistsJs(fileName: String): Promise<JsAny?>

@JsFun(
    """
async function deleteOpfsFileJs(fileName) {
    try {
        const dir = await navigator.storage.getDirectory();
        await dir.removeEntry(fileName);
    } catch(e) {
        console.error(e);
    }
}
"""
)
internal external fun deleteOpfsFileJs(fileName: String): Promise<JsAny?>

@JsFun(
    """
async function writeToOpfsJs(fileName, content, onProgress) {
    const dir = await navigator.storage.getDirectory();
    const fileHandle = await dir.getFileHandle(fileName, { create: true });
    const writable = await fileHandle.createWritable();
    
    if (content instanceof Blob) {
        const total = content.size;
        const chunkSize = 4 * 1024 * 1024; // 4MB chunks
        let offset = 0;
        
        while (offset < total) {
            const chunk = content.slice(offset, offset + chunkSize);
            await writable.write(chunk);
            offset += chunkSize;
            if (onProgress) {
                onProgress(Math.min(offset / total, 1.0));
            }
        }
    } else {
        await writable.write(content);
        if (onProgress) onProgress(1.0);
    }
    
    await writable.close();
}
"""
)
internal external fun writeToOpfsJs(
    fileName: String,
    content: JsAny,
    onProgress: ((Float) -> Unit)? = definedExternally
): Promise<JsAny?>

@JsFun(
    """
async function readFromOpfsJs(fileName) {
    const dir = await navigator.storage.getDirectory();
    const fileHandle = await dir.getFileHandle(fileName);
    const file = await fileHandle.getFile();
    return await file.text();
}
"""
)
internal external fun readFromOpfsJs(fileName: String): Promise<JsString>


class WasmGleeFileSystem(
    override val appDataDir: Path
) : GleeFileSystem {
    override suspend fun readFile(path: Path): Result<String> {
        return try {
            val content = readFromOpfsJs(path.name).await<JsString>()
            Result.success(content.toString())
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun writeFile(path: Path, content: String): Result<Unit> {
        return try {
            writeToOpfsJs(path.name, content.toJsString()).await<JsAny?>()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun writeBytes(
        path: Path,
        bytes: ByteArray,
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> {
        return try {
            val jsArray = Int8Array(bytes.size)
            for (i in bytes.indices) {
                // Yield periodically to keep UI responsive during memory copy
                if (i % (1024 * 1024) == 0) {
                    kotlinx.coroutines.yield()
                    onProgress?.invoke((i.toFloat() / bytes.size) * 0.4f) // Use first 40% for memory copy
                }
                jsArray[i] = bytes[i]
            }

            kotlinx.coroutines.yield()
            onProgress?.invoke(0.5f)
            writeToOpfsJs(path.name, jsArray) { progress ->
                onProgress?.invoke(0.5f + (progress * 0.5f))
            }.await<JsAny?>()
            
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun listFiles(path: Path): Result<List<Path>> {
        return Result.success(emptyList())
    }

    override suspend fun exists(path: Path): Boolean {
        return try {
            val result = checkOpfsFileExistsJs(path.name).await<JsBoolean>()
            result.toBoolean()
        } catch (e: Throwable) {
            false
        }
    }

    override suspend fun delete(path: Path): Result<Unit> {
        return try {
            deleteOpfsFileJs(path.name).await<JsAny?>()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun copyFile(
        source: Path,
        target: Path,
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Path-based copy is not supported on WasmJs"))
    }

    override suspend fun importFile(
        file: PlatformFile,
        targetPath: Path,
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> {
        return try {
            val rawFile = file.toJsFile()
                ?: return Result.failure(Exception("Platform file is null"))

            // In Kotlin/Wasm, we can't pass 'Any' directly to @JsFun.
            // Since toJsFile() returns the underlying JS object (File), we cast it.
            val jsFile = rawFile.asJsAny()

            writeToOpfsJs(targetPath.name, jsFile, onProgress).await<JsAny?>()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun Any.asJsAny(): JsAny = this as JsAny
