package `in`.ssverma.glee.features.chat.di

import `in`.ssverma.glee.core.preferences.GleeSettings
import `in`.ssverma.glee.domain.AiEngine
import `in`.ssverma.glee.domain.LiteRtEngine
import `in`.ssverma.glee.features.chat.data.remote.ModelDownloader
import `in`.ssverma.glee.features.chat.domain.model.AiSkill
import `in`.ssverma.glee.features.chat.domain.usecase.AiChatManager
import `in`.ssverma.glee.features.chat.domain.usecase.AgentProcessor
import `in`.ssverma.glee.features.chat.domain.usecase.GleeSkills
import `in`.ssverma.glee.features.chat.ui.ChatViewModel
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import `in`.ssverma.glee.core.common.platform.FileSystem as GleeFileSystem

val chatModule = module {
    single { ModelDownloader(client = get<HttpClient>(), okioFs = get<FileSystem>()) }

    single<AiEngine> { LiteRtEngine() }
    singleOf(::AgentProcessor)
    singleOf(::AiChatManager)

    single { GleeSkills.getDefaultSkills(urlLauncher = get(), json = get()) }

    single {
        ChatViewModel(
            chatManager = get<AiChatManager>(),
            skills = get<List<AiSkill>>(),
            modelDownloader = get<ModelDownloader>(),
            engine = get<AiEngine>(),
            settings = get<GleeSettings>(),
            fileSystem = get<GleeFileSystem>(),
            appDataDir = get<Path>(named("appDataDir"))
        )
    }
}
