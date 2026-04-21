package `in`.ssverma.glee

import android.app.Application
import `in`.ssverma.glee.di.initKoin
import org.koin.android.ext.koin.androidContext

class GleeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@GleeApplication)
        }
    }
}
