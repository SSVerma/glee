@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package `in`.ssverma.glee.core.common.platform

import kotlinx.coroutines.await
import okio.Path
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

class WasmGleeFileSystem(
    override val appDataDir: Path
) : GleeFileSystem {
    override suspend fun readFile(path: Path): Result<String> {
        return Result.failure(UnsupportedOperationException("readFile not supported in WasmGleeFileSystem yet"))
    }

    override suspend fun writeFile(path: Path, content: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("writeFile not supported in WasmGleeFileSystem yet"))
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
}
