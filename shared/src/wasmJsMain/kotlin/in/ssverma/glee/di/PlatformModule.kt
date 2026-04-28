package `in`.ssverma.glee.di

import `in`.ssverma.glee.core.common.platform.SpeechRecognizerManager
import `in`.ssverma.glee.core.common.platform.UrlLauncher
import `in`.ssverma.glee.core.database.ChatDao
import `in`.ssverma.glee.core.database.ConversationEntity
import `in`.ssverma.glee.core.database.GleeDatabase
import `in`.ssverma.glee.core.database.MessageEntity
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SettingsListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

class WebUrlLauncher : UrlLauncher {
    override fun launchUrl(url: String): Boolean {
        return try {
            kotlinx.browser.window.open(url, "_blank")
            true
        } catch (e: Exception) {
            false
        }
    }
}

class WebSpeechRecognizerManager : SpeechRecognizerManager {
    override val isSupported: Boolean = false
    override fun startListening(): Flow<String> = emptyFlow()
    override fun stopListening() {}
}

actual val platformFileSystem: FileSystem = object : FileSystem() {
    override fun appendingSink(file: Path, mustExist: Boolean) = throw UnsupportedOperationException()
    override fun atomicMove(source: Path, target: Path) = throw UnsupportedOperationException()
    override fun canonicalize(path: Path) = path
    override fun createDirectory(dir: Path, mustCreate: Boolean) = throw UnsupportedOperationException()
    override fun createSymlink(source: Path, target: Path) = throw UnsupportedOperationException()
    override fun delete(path: Path, mustExist: Boolean) = throw UnsupportedOperationException()
    override fun list(dir: Path) = emptyList<Path>()
    override fun listOrNull(dir: Path) = null
    override fun metadataOrNull(path: Path) = null
    override fun openReadOnly(file: Path) = throw UnsupportedOperationException()
    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean) = throw UnsupportedOperationException()
    override fun sink(file: Path, mustCreate: Boolean) = throw UnsupportedOperationException()
    override fun source(file: Path) = throw UnsupportedOperationException()
}

class WasmGleeDatabase : GleeDatabase() {
    override fun chatDao(): ChatDao = object : ChatDao {
        override suspend fun getConversations(): List<ConversationEntity> = emptyList()
        override suspend fun insertConversation(conversation: ConversationEntity) {}
        override suspend fun deleteConversation(conversationId: String) {}
        override suspend fun getMessages(conversationId: String): List<MessageEntity> = emptyList()
        override suspend fun insertMessage(message: MessageEntity) {}
        override suspend fun deleteMessages(conversationId: String) {}
    }

    override fun createInvalidationTracker(): androidx.room3.InvalidationTracker {
        throw UnsupportedOperationException()
    }

    override suspend fun clearAllTables() {
    }
}

/**
 * A manual implementation of ObservableSettings that uses localStorage (Settings())
 * but avoids delegation issues in Wasm.
 */
class WasmObservableSettings : ObservableSettings {
    private val delegate = Settings()

    override val keys: Set<String> get() = delegate.keys
    override val size: Int get() = delegate.size

    override fun clear() = delegate.clear()
    override fun remove(key: String) = delegate.remove(key)
    override fun hasKey(key: String): Boolean = delegate.hasKey(key)

    override fun putInt(key: String, value: Int) = delegate.putInt(key, value)
    override fun getInt(key: String, defaultValue: Int): Int = delegate.getInt(key, defaultValue)
    override fun getIntOrNull(key: String): Int? = delegate.getIntOrNull(key)

    override fun putLong(key: String, value: Long) = delegate.putLong(key, value)
    override fun getLong(key: String, defaultValue: Long): Long = delegate.getLong(key, defaultValue)
    override fun getLongOrNull(key: String): Long? = delegate.getLongOrNull(key)

    override fun putString(key: String, value: String) = delegate.putString(key, value)
    override fun getString(key: String, defaultValue: String): String = delegate.getString(key, defaultValue)
    override fun getStringOrNull(key: String): String? = delegate.getStringOrNull(key)

    override fun putFloat(key: String, value: Float) = delegate.putFloat(key, value)
    override fun getFloat(key: String, defaultValue: Float): Float = delegate.getFloat(key, defaultValue)
    override fun getFloatOrNull(key: String): Float? = delegate.getFloatOrNull(key)

    override fun putDouble(key: String, value: Double) = delegate.putDouble(key, value)
    override fun getDouble(key: String, defaultValue: Double): Double = delegate.getDouble(key, defaultValue)
    override fun getDoubleOrNull(key: String): Double? = delegate.getDoubleOrNull(key)

    override fun putBoolean(key: String, value: Boolean) = delegate.putBoolean(key, value)
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = delegate.getBoolean(key, defaultValue)
    override fun getBooleanOrNull(key: String): Boolean? = delegate.getBooleanOrNull(key)

    override fun addIntListener(key: String, defaultValue: Int, callback: (Int) -> Unit): SettingsListener = StubListener
    override fun addLongListener(key: String, defaultValue: Long, callback: (Long) -> Unit): SettingsListener = StubListener
    override fun addStringListener(key: String, defaultValue: String, callback: (String) -> Unit): SettingsListener = StubListener
    override fun addFloatListener(key: String, defaultValue: Float, callback: (Float) -> Unit): SettingsListener = StubListener
    override fun addDoubleListener(key: String, defaultValue: Double, callback: (Double) -> Unit): SettingsListener = StubListener
    override fun addBooleanListener(key: String, defaultValue: Boolean, callback: (Boolean) -> Unit): SettingsListener = StubListener
    
    override fun addIntOrNullListener(key: String, callback: (Int?) -> Unit): SettingsListener = StubListener
    override fun addLongOrNullListener(key: String, callback: (Long?) -> Unit): SettingsListener = StubListener
    override fun addStringOrNullListener(key: String, callback: (String?) -> Unit): SettingsListener = StubListener
    override fun addFloatOrNullListener(key: String, callback: (Float?) -> Unit): SettingsListener = StubListener
    override fun addDoubleOrNullListener(key: String, callback: (Double?) -> Unit): SettingsListener = StubListener
    override fun addBooleanOrNullListener(key: String, callback: (Boolean?) -> Unit): SettingsListener = StubListener

    private object StubListener : SettingsListener {
        override fun deactivate() {}
    }
}

actual val platformModule: Module = module {
    single { platformFileSystem }
    single<UrlLauncher> { WebUrlLauncher() }
    single<SpeechRecognizerManager> { WebSpeechRecognizerManager() }
    single<Path>(named("appDataDir")) { "/tmp".toPath() }
    single<GleeDatabase> { WasmGleeDatabase() }
    single<ObservableSettings> { WasmObservableSettings() }
}
