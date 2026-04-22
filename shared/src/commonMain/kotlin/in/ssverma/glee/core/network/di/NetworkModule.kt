package `in`.ssverma.glee.core.network.di

import `in`.ssverma.glee.core.network.HttpClientFactory
import org.koin.dsl.module

val networkModule = module {
    single { HttpClientFactory.create() }
}
