package `in`.ssverma.glee.di

import `in`.ssverma.glee.core.database.GleeDatabase
import `in`.ssverma.glee.core.database.GleeDatabaseConstructor
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.core.qualifier.named
import `in`.ssverma.glee.core.common.platform.UrlLauncher
import java.awt.Desktop
import java.net.URI

class JvmUrlLauncher : UrlLauncher {
    override fun launchUrl(url: String): Boolean {
        return try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual val platformModule: Module = module {
    single { platformFileSystem }
    single<UrlLauncher> { JvmUrlLauncher() }

    single {
        val dbFile = System.getProperty("user.home").toPath().resolve(".glee").resolve("glee.db")
        Room.databaseBuilder<GleeDatabase>(
            name = dbFile.toString(),
            factory = { GleeDatabaseConstructor.initialize() }
        ).setDriver(BundledSQLiteDriver())
            .build()
    }

    single<Path>(named("appDataDir")) {
        System.getProperty("user.home").toPath().resolve(".glee")
    }
}
