package `in`.ssverma.glee.features.chat.di

import `in`.ssverma.glee.features.chat.data.local.LiteRtEngine
import `in`.ssverma.glee.features.chat.data.remote.ModelDownloader
import `in`.ssverma.glee.features.chat.domain.usecase.AiChatManager
import `in`.ssverma.glee.features.chat.domain.usecase.LocalFileSystemSkill
import `in`.ssverma.glee.features.chat.ui.ChatViewModel
import io.ktor.client.HttpClient
import okio.FileSystem
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

import `in`.ssverma.glee.core.common.platform.FileSystem as GleeFileSystem

import `in`.ssverma.glee.core.preferences.GleeSettings
import okio.Path

val chatModule = module {
    single { ModelDownloader(client = get<HttpClient>(), okioFs = get<okio.FileSystem>()) }
    
    singleOf(::LiteRtEngine)
    singleOf(::AiChatManager)
    singleOf(::LocalFileSystemSkill)
    
    single {
        ChatViewModel(
            chatManager = get<AiChatManager>(),
            fileSystemSkill = get<LocalFileSystemSkill>(),
            modelDownloader = get<ModelDownloader>(),
            engine = get<LiteRtEngine>(),
            settings = get<GleeSettings>(),
            fileSystem = get<GleeFileSystem>(),
            appDataDir = get<Path>(named("appDataDir"))
        )
    }
}
