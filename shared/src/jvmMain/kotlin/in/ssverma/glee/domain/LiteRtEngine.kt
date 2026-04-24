package `in`.ssverma.glee.domain

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import `in`.ssverma.glee.features.chat.domain.model.AiSkill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

actual class LiteRtEngine actual constructor() : AiEngine {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private val mutex = Mutex()

    actual override suspend fun loadModel(config: ModelConfig): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.Default) {
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
    }

    actual override fun generateResponse(prompt: String): Flow<AiChunk> = flow {
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

    actual override fun setSkills(skills: List<AiSkill>) {
        // bridge logic
    }

    actual override fun setSystemPrompt(prompt: String) {
        // Implementation
    }

    actual override suspend fun clearConversation() = mutex.withLock {
        withContext(Dispatchers.Default) {
            conversation?.close()
            conversation = engine?.createConversation()
        }
    }

    actual override suspend fun close() = mutex.withLock {
        closeInternal()
    }
    
    private fun closeInternal() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
    }
}
