package `in`.ssverma.glee.features.chat.di

import `in`.ssverma.glee.domain.AiEngine
import `in`.ssverma.glee.domain.LiteRtEngine
import `in`.ssverma.glee.features.chat.data.repository.AiModelRepository
import `in`.ssverma.glee.features.chat.domain.ChatSuggestionProvider
import `in`.ssverma.glee.features.chat.domain.usecase.AgentProcessor
import `in`.ssverma.glee.features.chat.domain.usecase.AiChatManager
import `in`.ssverma.glee.features.chat.domain.usecase.GleeTools
import `in`.ssverma.glee.features.chat.ui.ChatViewModel
import `in`.ssverma.glee.features.models.ModelManagementViewModel
import `in`.ssverma.glee.features.settings.SettingsViewModel
import `in`.ssverma.glee.features.skills.ManageToolsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val chatModule = module {

    single<AiEngine> { LiteRtEngine() }
    singleOf(::AgentProcessor)
    singleOf(::AiChatManager)

    single { GleeTools.getDefaultSkills(urlLauncher = get(), json = get()) }
    single { AiModelRepository(fileSystem = get(), appDataDir = get(named("appDataDir"))) }
    singleOf(::ChatSuggestionProvider)

    single {
        ChatViewModel(
            chatManager = get(),
            skills = get(),
            engine = get(),
            settings = get(),
            appDataDir = get(named("appDataDir")),
            speechRecognizerManager = get(),
            modelRepository = get(),
            suggestionProvider = get()
        )
    }

    single {
        SettingsViewModel(
            settings = get(),
            chatManager = get()
        )
    }

    single {
        ModelManagementViewModel(
            settings = get(),
            modelRepository = get(),
            modelDownloader = get(),
            fileSystem = get(),
            appDataDir = get(named("appDataDir")),
            permissionManager = get(),
            urlLauncher = get()
        )
    }

    single {
        ManageToolsViewModel(
            chatManager = get(),
            skills = get()
        )
    }
}
