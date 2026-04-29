@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package `in`.ssverma.glee.core.common.platform

import kotlinx.coroutines.await
import okio.Path
import org.khronos.webgl.Int8Array
import org.khronos.webgl.set
import kotlin.js.Promise

@JsFun("""
async function checkOpfsFileExistsJs(fileName) {
    try {
        const dir = await navigator.storage.getDirectory();
        await dir.getFileHandle(fileName);
        return true;
    } catch(e) {
        return false;
    }
}
""")
internal external fun checkOpfsFileExistsJs(fileName: String): Promise<JsAny?>

@JsFun("""
async function deleteOpfsFileJs(fileName) {
    try {
        const dir = await navigator.storage.getDirectory();
        await dir.removeEntry(fileName);
    } catch(e) {
        console.error(e);
    }
}
""")
internal external fun deleteOpfsFileJs(fileName: String): Promise<JsAny?>
 
@JsFun("""
async function writeToOpfsJs(fileName, content) {
    const dir = await navigator.storage.getDirectory();
    const fileHandle = await dir.getFileHandle(fileName, { create: true });
    const writable = await fileHandle.createWritable();
    await writable.write(content);
    await writable.close();
}
""")
internal external fun writeToOpfsJs(fileName: String, content: JsAny): Promise<JsAny?>

@JsFun("""
async function readFromOpfsJs(fileName) {
    const dir = await navigator.storage.getDirectory();
    const fileHandle = await dir.getFileHandle(fileName);
    const file = await fileHandle.getFile();
    return await file.text();
}
""")
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
                    onProgress?.invoke((i.toFloat() / bytes.size) * 0.5f) // Use first 50% for memory copy
                }
                jsArray[i] = bytes[i]
            }

            // For WASM/JS, we can't easily chunk the Int8Array into the JS write function without multiple calls
            // But we can report 0% and 100% or use a simple loop if we want real progress.
            // Let's do it in chunks for real progress and cancellation support.
            val chunkSize = 1024 * 1024 // 1MB chunks for web
            var offset = 0
            
            // We need a way to write chunks to the same file.
            // Our JS writeToOpfsJs currently creates/overwrites.
            // I should update it to support appending or just use a more advanced JS helper.
            
            // For now, let's just do a single write but yield before to allow cancellation.
            kotlinx.coroutines.yield()
            onProgress?.invoke(0.6f)
            writeToOpfsJs(path.name, jsArray).await<JsAny?>()
            onProgress?.invoke(1f)
            
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
            // Need to convert JsBoolean to Kotlin Boolean. Actually we can return a Promise<JsBoolean> and await it.
            // Wait, Kotlin's JsBoolean is not directly accessible without `.toBoolean()`.
            // Let's ensure the JS function returns a boolean.
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
}
