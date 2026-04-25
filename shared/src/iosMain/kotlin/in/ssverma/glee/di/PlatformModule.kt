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

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual val platformModule: Module = module {
    single { platformFileSystem }
    single<UrlLauncher> { IosUrlLauncher() }

    single<Path>(named("appDataDir")) {
        NSHomeDirectory().toPath().resolve("Documents")
    }
}
