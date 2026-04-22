package `in`.ssverma.glee.domain

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LiteRtLmJniException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

actual class LiteRtEngine actual constructor() {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private val mutex = Mutex()

    actual suspend fun loadModel(config: ModelConfig): Result<Unit> = mutex.withLock {
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
                            backend = Backend.GPU()
                        )
                        val e = Engine(gpuConfig)
                        e.initialize()
                        e
                    } catch (e: Throwable) {
                        Log.w(
                            "LiteRtEngine",
                            "GPU initialization failed, falling back to CPU: ${e.message}"
                        )
                        initializeCpuEngine(config.modelPath)
                    }
                } else {
                    Log.d("LiteRtEngine", "CPU requested, bypassing GPU")
                    initializeCpuEngine(config.modelPath)
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

    private fun initializeCpuEngine(modelPath: String): Engine {
        val cpuConfig = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU()
        )
        val e = Engine(cpuConfig)
        e.initialize()
        return e
    }

    actual fun generateResponse(prompt: String): Flow<AiChunk> = flow {
        val conv = conversation
        if (conv == null) {
            Log.e("LiteRtEngine", "Conversation is null during generation request")
            emit(
                AiChunk(
                    text = "Error: Engine not ready. Please try reloading the model.",
                    isFinal = false
                )
            )
            emit(AiChunk(text = "", isFinal = true))
            return@flow
        }

        Log.d("LiteRtEngine", "Generating response for prompt length: ${prompt.length}")

        try {
            val cleanPrompt = prompt.trim()
            if (cleanPrompt.isEmpty()) {
                emit(AiChunk(text = "Please enter a valid message.", isFinal = true))
                return@flow
            }

            // Gemma 4 specific markers
            val formattedPrompt = if (cleanPrompt.contains("<start_of_turn>")) cleanPrompt else {
                "<start_of_turn>user\n$cleanPrompt<end_of_turn>\n<start_of_turn>model\n"
            }

            var receivedChunks = 0
            conv.sendMessageAsync(formattedPrompt).collect { message ->
                receivedChunks++
                val text = message.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }

                if (text.isNotEmpty()) {
                    Log.v("LiteRtEngine", "Emitting chunk $receivedChunks")
                    emit(AiChunk(text = text, isFinal = false))
                }
            }

            if (receivedChunks == 0) {
                Log.w(
                    "LiteRtEngine",
                    "Model produced zero chunks. This often indicates a native backend failure or low memory."
                )
                emit(
                    AiChunk(
                        text = "The model produced no response. Try toggling GPU off in settings.",
                        isFinal = false
                    )
                )
            }

            Log.d("LiteRtEngine", "Response generation complete. Total chunks: $receivedChunks")
        } catch (e: LiteRtLmJniException) {
            Log.e("LiteRtEngine", "JNI Error during inference: ${e.message}", e)
            val msg = if (e.message?.contains("OpenCL") == true) {
                "Hardware Error: GPU not supported (No OpenCL). Please turn OFF GPU in the Tools menu."
            } else {
                "Engine Error: ${e.message}. Try switching to CPU."
            }
            emit(AiChunk(text = msg, isFinal = false))
        } catch (e: Exception) {
            Log.e("LiteRtEngine", "Error during generation flow", e)
            emit(AiChunk(text = "System Error: ${e.message}", isFinal = false))
        }
        emit(AiChunk(text = "", isFinal = true))
    }.flowOn(Dispatchers.Default)

    actual fun setSkills(skills: List<Skill>) {
        // bridge logic
    }

    actual fun close() {
        // loading already calls closeInternal
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
