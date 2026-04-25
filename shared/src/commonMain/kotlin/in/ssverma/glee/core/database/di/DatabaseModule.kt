package `in`.ssverma.glee.core.database.di

import `in`.ssverma.glee.features.chat.data.repository.RealChatRepository
import `in`.ssverma.glee.features.chat.domain.repository.ChatRepository
import org.koin.dsl.module

val databaseModule = module {
    single<ChatRepository> { RealChatRepository(db = get(), json = get()) }
}
