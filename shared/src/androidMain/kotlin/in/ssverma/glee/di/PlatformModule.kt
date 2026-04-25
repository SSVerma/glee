package `in`.ssverma.glee.di

import `in`.ssverma.glee.core.database.GleeDatabase
import `in`.ssverma.glee.core.database.GleeDatabaseConstructor
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import androidx.room3.Room
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import org.koin.core.qualifier.named
import `in`.ssverma.glee.core.common.platform.UrlLauncher
import `in`.ssverma.glee.core.common.platform.SpeechRecognizerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

class AndroidUrlLauncher(private val context: Context) : UrlLauncher {
    override fun launchUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}

class AndroidSpeechRecognizerManager(private val context: Context) : SpeechRecognizerManager {
    private var recognizer: SpeechRecognizer? = null

    override val isSupported: Boolean
        get() {
            val supported = SpeechRecognizer.isRecognitionAvailable(context)
            android.util.Log.d("SpeechRecognizer", "isSupported: $supported")
            return supported
        }

    override fun startListening(): Flow<String> = callbackFlow {
        if (!isSupported) {
            close()
            return@callbackFlow
        }

        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        
        mainHandler.post {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        android.util.Log.e("SpeechRecognizer", "Error code: $error")
                        trySend("") 
                        close()
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            trySend(matches[0])
                        }
                        close()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            trySend(matches[0])
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                
                startListening(intent)
            }
        }

        awaitClose {
            mainHandler.post {
                recognizer?.stopListening()
                recognizer?.destroy()
                recognizer = null
            }
        }
    }

    override fun stopListening() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            recognizer?.stopListening()
        }
    }
}

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual val platformModule: Module = module {
    single { platformFileSystem }
    single<UrlLauncher> { AndroidUrlLauncher(get()) }
    single<SpeechRecognizerManager> { AndroidSpeechRecognizerManager(get()) }

    single {
        val context: Context = get()
        val dbFile = context.filesDir.absolutePath.toPath().resolve("glee.db")
        Room.databaseBuilder<GleeDatabase>(
            context = context,
            name = dbFile.toString(),
            factory = { GleeDatabaseConstructor.initialize() }
        ).setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    
    single<Path>(named("appDataDir")) {
        val context: Context = get()
        context.filesDir.absolutePath.toPath()
    }
}
