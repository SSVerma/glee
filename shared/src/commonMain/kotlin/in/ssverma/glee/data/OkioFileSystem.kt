package `in`.ssverma.glee.data

import `in`.ssverma.glee.domain.FileSystem
import okio.FileSystem as OkioFS
import okio.Path

/**
 * Production-ready implementation of FileSystem using Okio.
 */
class OkioFileSystem(
    private val okioFs: OkioFS,
    override val appDataDir: Path
) : FileSystem {

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

    override suspend fun listFiles(path: Path): Result<List<Path>> = runCatching {
        okioFs.list(path)
    }

    override suspend fun exists(path: Path): Boolean {
        return okioFs.exists(path)
    }

    override suspend fun delete(path: Path): Result<Unit> = runCatching {
        okioFs.delete(path)
    }
}
