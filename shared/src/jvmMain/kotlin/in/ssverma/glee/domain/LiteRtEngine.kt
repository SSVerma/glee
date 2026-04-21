package `in`.ssverma.glee.domain

import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class LiteRtEngine actual constructor() {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private val mutex = Mutex()

    actual suspend fun loadModel(config: ModelConfig): Result<Unit> = mutex.withLock {
        runCatching {
            closeInternal()

            val engineConfig = EngineConfig(
                modelPath = config.modelPath,
                backend = Backend.CPU()
            )
            val newEngine = Engine(engineConfig)
            newEngine.initialize()
            engine = newEngine
            
            conversation = newEngine.createConversation()
            Unit
        }
    }

    actual fun generateResponse(prompt: String): Flow<AiChunk> = flow {
        mutex.withLock {
            val conv = conversation ?: throw IllegalStateException("Model not loaded")
            
            conv.sendMessageAsync(prompt).collect { message ->
                val text = message.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
                
                if (text.isNotEmpty()) {
                    emit(AiChunk(text = text, isFinal = false))
                }
            }
            emit(AiChunk(text = "", isFinal = true))
        }
    }

    actual fun setSkills(skills: List<Skill>) {
        // bridge logic
    }

    actual fun close() {
        // close logic
    }
    
    private fun closeInternal() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
    }
}
