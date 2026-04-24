package `in`.ssverma.glee.core.network.di

import `in`.ssverma.glee.core.network.HttpClientFactory
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single { Json { ignoreUnknownKeys = true } }
    single { HttpClientFactory.create(json = get()) }
}
