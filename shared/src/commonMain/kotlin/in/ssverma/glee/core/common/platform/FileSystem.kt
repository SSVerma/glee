package `in`.ssverma.glee.core.common.platform

import okio.Path

interface FileSystem {
    val appDataDir: Path
    suspend fun readFile(path: Path): Result<String>
    suspend fun writeFile(path: Path, content: String): Result<Unit>
    suspend fun listFiles(path: Path): Result<List<Path>>
    suspend fun exists(path: Path): Boolean
    suspend fun delete(path: Path): Result<Unit>
}
