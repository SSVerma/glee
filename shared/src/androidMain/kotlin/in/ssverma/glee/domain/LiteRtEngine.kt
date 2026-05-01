package `in`.ssverma.glee.domain

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import `in`.ssverma.glee.features.chat.domain.model.AiTool
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
                val file = java.io.File(config.modelPath)
                Log.d(
                    "LiteRtEngine",
                    "Model file exists: ${file.exists()}, size: ${file.length()} bytes"
                )

                Log.d(
                    "LiteRtEngine",
                    "Loading model (GPU: ${config.useGpu}) from: ${config.modelPath}"
                )
                closeInternal()

                // Robust initialization with fallback
                val newEngine = if (config.useGpu) {
                    try {
                        Log.d("LiteRtEngine", "Attempting GPU initialization...")
                        val gpuConfig = EngineConfig(
                            modelPath = config.modelPath,
                            backend = Backend.GPU(),
                            visionBackend = if (config.maxNumImages > 0) Backend.GPU() else null,
                            maxNumImages = if (config.maxNumImages > 0) config.maxNumImages else null
                        )
                        val e = Engine(gpuConfig)
                        e.initialize()
                        e
                    } catch (e: Throwable) {
                        Log.w(
                            "LiteRtEngine",
                            "GPU initialization failed, falling back to CPU: ${e.message}"
                        )
                        initializeCpuEngine(config.modelPath, config.maxNumImages)
                    }
                } else {
                    Log.d("LiteRtEngine", "CPU requested, bypassing GPU")
                    initializeCpuEngine(config.modelPath, config.maxNumImages)
                }

                engine = newEngine
                conversation = newEngine.createConversation()
                Log.d("LiteRtEngine", "Model and Conversation initialized successfully")
                Unit
            }.onFailure {
                Log.e("LiteRtEngine", "Critical failure during model load", it)
                closeInternal() // Ensure state is clean on failure
            }
        }
    }

    private fun initializeCpuEngine(modelPath: String, maxNumImages: Int): Engine {
        val cpuConfig = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU(),
            visionBackend = if (maxNumImages > 0) Backend.CPU() else null,
            maxNumImages = if (maxNumImages > 0) maxNumImages else null
        )
        val e = Engine(cpuConfig)
        e.initialize()
        return e
    }

    actual override suspend fun clearConversation() = mutex.withLock {
        withContext(Dispatchers.Default) {
            try {
                conversation?.close()
            } catch (e: Exception) {
                Log.e("LiteRtEngine", "Error closing conversation", e)
            }
            conversation = engine?.createConversation()
            isSystemPromptSent = false
        }
    }

    actual override fun generateResponse(
        prompt: String,
        files: List<AttachedFile>
    ): Flow<AiChunk> = flow {
        mutex.withLock {
            val conv = conversation
            if (conv == null) {
                emit(
                    AiChunk(
                        text = "Error: Engine not ready. Please try reloading the model.",
                        isFinal = false
                    )
                )
                emit(AiChunk(text = "", isFinal = true))
                return@withLock
            }

            try {
                val cleanPrompt = prompt.trim()
                if (cleanPrompt.isEmpty()) {
                    emit(AiChunk(text = "Please enter a valid message.", isFinal = true))
                    return@withLock
                }

                // Optimization: Only send system prompt on the first turn of a conversation.
                // LiteRT's Conversation object maintains history internally.
                val formattedPrompt = buildString {
                    if (systemPrompt.isNotEmpty() && !isSystemPromptSent) {
                        append("<start_of_turn>system\n$systemPrompt<end_of_turn>\n")
                        isSystemPromptSent = true
                    }
                    append("<start_of_turn>user\n$cleanPrompt<end_of_turn>\n<start_of_turn>model\n")
                }

                // Only use multimodal Contents path if we actually have image files.
                // Plain text prompts must use the string overload to avoid native crashes.
                val imageContents = mutableListOf<Content>()
                for (file in files) {
                    try {
                        val bytes = file.platformFile.readBytes()
                        imageContents.add(Content.ImageBytes(bytes))
                    } catch (e: Exception) {
                        Log.e("LiteRtEngine", "Failed to read attached file: ${file.name}", e)
                    }
                }

                var receivedChunks = 0
                if (imageContents.isNotEmpty()) {
                    imageContents.add(Content.Text(formattedPrompt))
                    val contents = Contents.of(imageContents)
                    conv.sendMessageAsync(contents).collect { message ->
                        receivedChunks++
                        val text = message.contents.contents
                            .filterIsInstance<Content.Text>()
                            .joinToString("") { it.text }
                        if (text.isNotEmpty()) {
                            emit(AiChunk(text = text, isFinal = false))
                        }
                    }
                } else {
                    conv.sendMessageAsync(formattedPrompt).collect { message ->
                        receivedChunks++
                        val text = message.contents.contents
                            .filterIsInstance<Content.Text>()
                            .joinToString("") { it.text }
                        if (text.isNotEmpty()) {
                            emit(AiChunk(text = text, isFinal = false))
                        }
                    }
                }

                if (receivedChunks == 0) {
                    emit(AiChunk(text = "The model produced no response.", isFinal = false))
                }
            } catch (e: Exception) {
                emit(AiChunk(text = "Error: ${e.message}", isFinal = false))
            }
            emit(AiChunk(text = "", isFinal = true))
        }
    }.flowOn(Dispatchers.Default)

    actual override fun setSystemPrompt(prompt: String) {
        if (systemPrompt != prompt) {
            systemPrompt = prompt
            // We can't easily update system prompt in an active LiteRT conversation 
            // without specialized logic, so we reset to ensure the new prompt is used.
            isSystemPromptSent = false
        }
    }

    actual override fun setSkills(skills: List<AiTool>) {
        // Implementation for passing structured tool definitions to the engine if supported by LiteRT
    }

    actual override suspend fun close() = mutex.withLock {
        closeInternal()
    }

    private fun closeInternal() {
        try {
            conversation?.close()
            engine?.close()
        } catch (e: Exception) {
            Log.e("LiteRtEngine", "Error closing engine", e)
        }
        conversation = null
        engine = null
    }
}
