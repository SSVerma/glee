package `in`.ssverma.glee.core.preferences.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import `in`.ssverma.glee.core.preferences.GleeSettings
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val preferencesModule = module {
    single<ObservableSettings> { Settings() as ObservableSettings }
    singleOf(::GleeSettings)
}
