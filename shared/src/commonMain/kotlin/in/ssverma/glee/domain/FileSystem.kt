package `in`.ssverma.glee.domain

import okio.Path

/**
 * Domain interface for interacting with the local file system.
 */
interface FileSystem {
    /**
     * The root directory for app-specific data (e.g., models).
     */
    val appDataDir: Path

    suspend fun readFile(path: Path): Result<String>
    suspend fun writeFile(path: Path, content: String): Result<Unit>
    suspend fun listFiles(path: Path): Result<List<Path>>
    suspend fun exists(path: Path): Boolean
    suspend fun delete(path: Path): Result<Unit>
}
