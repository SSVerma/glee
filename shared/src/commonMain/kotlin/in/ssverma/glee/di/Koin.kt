package `in`.ssverma.glee.di

import `in`.ssverma.glee.core.database.di.databaseModule
import `in`.ssverma.glee.core.network.di.networkModule
import `in`.ssverma.glee.core.preferences.di.preferencesModule
import `in`.ssverma.glee.features.chat.di.chatModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

val commonModule = org.koin.dsl.module {

    includes(networkModule, databaseModule, preferencesModule, chatModule)
}

fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}) {
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule)
    }
}
