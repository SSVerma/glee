package `in`.ssverma.glee.di

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory
import org.koin.core.qualifier.named

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual val platformModule: Module = module {
    single { platformFileSystem }

    single<Path>(named("appDataDir")) {
        NSHomeDirectory().toPath().resolve("Documents")
    }
}
