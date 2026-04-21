package `in`.ssverma.glee.di

import `in`.ssverma.glee.data.db.GleeDatabase
import `in`.ssverma.glee.data.db.GleeDatabaseConstructor
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import androidx.room3.Room
import android.content.Context
import org.koin.core.qualifier.named

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual val platformModule: Module = module {
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
