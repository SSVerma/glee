package `in`.ssverma.glee.domain

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
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
    private var systemPrompt: String = ""
    private var isSystemPromptSent: Boolean = false
    private val mutex = Mutex()

    actual override suspend fun loadModel(config: ModelConfig): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.Default) {
            runCatching {
                closeInternal()

                val engineConfig = EngineConfig(
                    modelPath = config.modelPath,
                    backend = Backend.CPU(),
                    visionBackend = if (config.maxNumImages > 0) Backend.CPU() else null,
                    maxNumImages = if (config.maxNumImages > 0) config.maxNumImages else null
                )
                val newEngine = Engine(engineConfig)
                newEngine.initialize()
                engine = newEngine
                
                conversation = newEngine.createConversation()
                Unit
            }
        }
    }

    actual override fun generateResponse(
        prompt: String, 
        files: List<`in`.ssverma.glee.features.chat.domain.model.AttachedFile>
    ): Flow<AiChunk> = flow {
        mutex.withLock {
            val conv = conversation ?: throw IllegalStateException("Model not loaded")
            
            val cleanPrompt = prompt.trim()
                // Optimization: Only send system prompt on the first turn of a conversation.
                val formattedPrompt = buildString {
                    if (systemPrompt.isNotEmpty() && !isSystemPromptSent) {
                        append("<start_of_turn>system\n$systemPrompt<end_of_turn>\n")
                        isSystemPromptSent = true
                    }
                    append("<start_of_turn>user\n$cleanPrompt<end_of_turn>\n<start_of_turn>model\n")
                }
                
                val imageContents = mutableListOf<Content>()
                for (file in files) {
                    try {
                        val bytes = file.platformFile.readBytes()
                        imageContents.add(Content.ImageBytes(bytes))
                    } catch (e: Exception) {
                        System.err.println("Failed to read attached file: ${file.name} - ${e.message}")
                    }
                }

                if (imageContents.isNotEmpty()) {
                    imageContents.add(Content.Text(formattedPrompt))
                    val contents = Contents.of(imageContents)
                    conv.sendMessageAsync(contents).collect { message ->
                        val text = message.contents.contents
                            .filterIsInstance<Content.Text>()
                            .joinToString("") { it.text }
                        if (text.isNotEmpty()) {
                            emit(AiChunk(text = text, isFinal = false))
                        }
                    }
                } else {
                    conv.sendMessageAsync(formattedPrompt).collect { message ->
                        val text = message.contents.contents
                            .filterIsInstance<Content.Text>()
                            .joinToString("") { it.text }
                        if (text.isNotEmpty()) {
                            emit(AiChunk(text = text, isFinal = false))
                        }
                    }
                }
            emit(AiChunk(text = "", isFinal = true))
        }
    }

    actual override fun setSkills(skills: List<AiSkill>) {
        // bridge logic
    }

    actual override fun setSystemPrompt(prompt: String) {
        if (systemPrompt != prompt) {
            systemPrompt = prompt
            isSystemPromptSent = false
        }
    }

    actual override suspend fun clearConversation() = mutex.withLock {
        withContext(Dispatchers.Default) {
            conversation?.close()
            conversation = engine?.createConversation()
            isSystemPromptSent = false
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
