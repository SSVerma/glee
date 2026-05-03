package `in`.ssverma.glee.di

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory
import org.koin.core.qualifier.named
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import `in`.ssverma.glee.core.common.platform.UrlLauncher
import `in`.ssverma.glee.core.common.platform.SpeechRecognizerManager
import `in`.ssverma.glee.core.database.GleeDatabase
import `in`.ssverma.glee.core.database.GleeDatabaseConstructor
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import `in`.ssverma.glee.features.chat.data.remote.ModelDownloader
import `in`.ssverma.glee.features.chat.data.remote.IosModelDownloader
import `in`.ssverma.glee.core.common.platform.GleeFileSystem
import `in`.ssverma.glee.core.common.platform.OkioFileSystem
import `in`.ssverma.glee.core.common.platform.PermissionManager
import `in`.ssverma.glee.core.common.platform.NoOpPermissionManager

class IosUrlLauncher : UrlLauncher {
    override fun launchUrl(url: String): Boolean {
        return try {
            val nsUrl = NSURL(string = url)
            if (nsUrl != null) {
                UIApplication.sharedApplication.openURL(nsUrl)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

class IosSpeechRecognizerManager : SpeechRecognizerManager {
    override val isSupported: Boolean = false
    override fun startListening(): Flow<String> = emptyFlow()
    override fun stopListening() {}
}

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual val platformModule: Module = module {
    single { platformFileSystem }
    single<UrlLauncher> { IosUrlLauncher() }
    single<PermissionManager> { NoOpPermissionManager() }
    single<SpeechRecognizerManager> { IosSpeechRecognizerManager() }
    single<ModelDownloader> { IosModelDownloader(get(), get()) }
    single<GleeFileSystem> { OkioFileSystem(get(), get(named("appDataDir"))) }

    single {
        val dbFile = NSHomeDirectory().toPath().resolve("Documents").resolve("glee.db")
        Room.databaseBuilder<GleeDatabase>(
            name = dbFile.toString(),
            factory = { GleeDatabaseConstructor.initialize() }
        ).setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    single<Path>(named("appDataDir")) {
        NSHomeDirectory().toPath().resolve("Documents")
    }
}
