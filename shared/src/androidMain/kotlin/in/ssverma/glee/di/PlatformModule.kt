package `in`.ssverma.glee.di

import `in`.ssverma.glee.core.database.GleeDatabase
import `in`.ssverma.glee.core.database.GleeDatabaseConstructor
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import androidx.room3.Room
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.core.qualifier.named
import `in`.ssverma.glee.core.common.platform.UrlLauncher

class AndroidUrlLauncher(private val context: Context) : UrlLauncher {
    override fun launchUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual val platformModule: Module = module {
    single { platformFileSystem }
    single<UrlLauncher> { AndroidUrlLauncher(get()) }

    single {
        val context: Context = get()
        val dbFile = context.filesDir.absolutePath.toPath().resolve("glee.db")
        Room.databaseBuilder<GleeDatabase>(
            context = context,
            name = dbFile.toString(),
            factory = { GleeDatabaseConstructor.initialize() }
        ).setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
            .build()
    }
    
    single<Path>(named("appDataDir")) {
        val context: Context = get()
        context.filesDir.absolutePath.toPath()
    }
}
