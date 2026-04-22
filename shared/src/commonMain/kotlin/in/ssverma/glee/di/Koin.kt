package `in`.ssverma.glee.di

import `in`.ssverma.glee.GleeConfig
import `in`.ssverma.glee.data.ChatRepository
import `in`.ssverma.glee.data.ModelDownloader
import `in`.ssverma.glee.domain.AiChatManager
import `in`.ssverma.glee.domain.LiteRtEngine
import `in`.ssverma.glee.domain.LocalFileSystemSkill
import `in`.ssverma.glee.ui.chat.ChatViewModel
import `in`.ssverma.glee.data.settings.GleeSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.ObservableSettings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okio.FileSystem
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.core.KoinApplication

val commonModule = module {
    single<ObservableSettings> { Settings() as ObservableSettings }
    singleOf(::GleeSettings)

    single {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 3600_000 // 1 hour
                connectTimeoutMillis = 60_000
                socketTimeoutMillis = 3600_000
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("Ktor: $message")
                    }
                }
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
        }

        client.plugin(HttpSend).intercept { request ->
            if (request.url.host.endsWith("huggingface.co") && GleeConfig.HF_TOKEN.isNotBlank()) {
                request.header(HttpHeaders.Authorization, "Bearer ${GleeConfig.HF_TOKEN}")
            }
            execute(request)
        }
        
        client
    }
    
    single<FileSystem> { platformFileSystem }
    single { ModelDownloader(client = get(), okioFs = get()) }
    
    singleOf(::ChatRepository)
    singleOf(::LiteRtEngine)
    singleOf(::AiChatManager)
    singleOf(::LocalFileSystemSkill)
    
    single {
        ChatViewModel(
            chatManager = get(),
            fileSystemSkill = get(),
            modelDownloader = get(),
            engine = get(),
            settings = get(),
            appDataDir = get(named("appDataDir"))
        )
    }
}

fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}) {
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule)
    }
}
