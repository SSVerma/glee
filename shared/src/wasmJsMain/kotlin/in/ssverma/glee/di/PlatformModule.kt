package `in`.ssverma.glee.di

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.qualifier.named
import kotlinx.browser.window
import `in`.ssverma.glee.core.common.platform.UrlLauncher
import `in`.ssverma.glee.core.common.platform.SpeechRecognizerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class WebUrlLauncher : UrlLauncher {
    override fun launchUrl(url: String): Boolean {
        return try {
            window.open(url, "_blank")
            true
        } catch (e: Exception) {
            false
        }
    }
}

class WebSpeechRecognizerManager : SpeechRecognizerManager {
    override val isSupported: Boolean = false
    override fun startListening(): Flow<String> = emptyFlow()
    override fun stopListening() {}
}

actual val platformFileSystem: FileSystem = object : FileSystem() {
    // This is a stub for Wasm, as it doesn't have a real FS like this.
    // In a real app, you'd use IndexedDB or another persistence layer.
    override fun appendingSink(file: Path, mustExist: Boolean) = throw UnsupportedOperationException()
    override fun atomicMove(source: Path, target: Path) = throw UnsupportedOperationException()
    override fun canonicalize(path: Path) = path
    override fun createDirectory(dir: Path, mustCreate: Boolean) = throw UnsupportedOperationException()
    override fun createSymlink(source: Path, target: Path) = throw UnsupportedOperationException()
    override fun delete(path: Path, mustExist: Boolean) = throw UnsupportedOperationException()
    override fun list(dir: Path) = emptyList<Path>()
    override fun listOrNull(dir: Path) = null
    override fun metadataOrNull(path: Path) = null
    override fun openReadOnly(file: Path) = throw UnsupportedOperationException()
    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean) = throw UnsupportedOperationException()
    override fun sink(file: Path, mustCreate: Boolean) = throw UnsupportedOperationException()
    override fun source(file: Path) = throw UnsupportedOperationException()
}

actual val platformModule: Module = module {
    single { platformFileSystem }
    single<UrlLauncher> { WebUrlLauncher() }
    single<SpeechRecognizerManager> { WebSpeechRecognizerManager() }
    single<Path>(named("appDataDir")) { "/tmp".toPath() }
}
