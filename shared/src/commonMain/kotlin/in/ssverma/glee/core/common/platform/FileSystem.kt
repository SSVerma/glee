package `in`.ssverma.glee.core.common.platform

import okio.Path

interface GleeFileSystem {
    val appDataDir: Path
    suspend fun readFile(path: Path): Result<String>
    suspend fun writeFile(path: Path, content: String): Result<Unit>
    suspend fun writeBytes(path: Path, bytes: ByteArray, onProgress: ((Float) -> Unit)? = null): Result<Unit>
    suspend fun copyFile(source: Path, target: Path, onProgress: ((Float) -> Unit)? = null): Result<Unit>
    suspend fun listFiles(path: Path): Result<List<Path>>
    suspend fun exists(path: Path): Boolean
    suspend fun delete(path: Path): Result<Unit>
}
