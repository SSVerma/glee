package `in`.ssverma.glee.domain

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import `in`.ssverma.glee.core.common.platform.getSystemMetrics
import `in`.ssverma.glee.features.chat.domain.model.AiTool
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.BackendType
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

actual class LiteRtEngine actual constructor() : AiEngine, KoinComponent {
    private val context: Context by inject()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var systemPrompt: String = ""
    private var isSystemPromptSent: Boolean = false
    private val mutex = Mutex()

    actual override val isLowConstraintDevice: Boolean by lazy { checkIsLowConstraintDevice() }

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
                    "Loading model with preferred backend: ${config.preferredBackend} from: ${config.modelPath}"
                )
                closeInternal()

                val engineInstance = when (config.preferredBackend) {
                    BackendType.Npu -> {
                        tryInitializeNpu(config) ?: tryInitializeGpu(config) ?: initializeCpuEngine(
                            config.modelPath,
                            config.maxNumImages
                        )
                    }

                    BackendType.Gpu -> {
                        tryInitializeGpu(config) ?: initializeCpuEngine(
                            config.modelPath,
                            config.maxNumImages
                        )
                    }

                    BackendType.Cpu -> {
                        initializeCpuEngine(config.modelPath, config.maxNumImages)
                    }

                    BackendType.Auto -> {
                        // Priority: Stability Check -> NPU -> GPU -> CPU
                        if (isLowConstraintDevice) {
                            initializeCpuEngine(config.modelPath, config.maxNumImages)
                        } else {
                            tryInitializeNpu(config)
                                ?: tryInitializeGpu(config)
                                ?: initializeCpuEngine(config.modelPath, config.maxNumImages)
                        }
                    }
                }

                engine = engineInstance
                conversation = engineInstance.createConversation()
                Log.d("LiteRtEngine", "Model and Conversation initialized successfully")
                Unit
            }.onFailure {
                Log.e("LiteRtEngine", "Critical failure during model load", it)
                closeInternal()
            }
        }
    }

    private fun tryInitializeNpu(config: ModelConfig): Engine? {
        if (android.os.Build.VERSION.SDK_INT < 31) {
            Log.d("LiteRtEngine", "NPU not supported on API < 31")
            return null
        }

        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val dispatchLibs = listOf(
            "libLiteRtDispatch_Qualcomm.so",
            "libLiteRtDispatch_MTK.so",
            "libLiteRtDispatch_Google.so"
        )

        val hasDispatchLib = dispatchLibs.any { File(nativeLibDir, it).exists() }

        if (!hasDispatchLib) {
            Log.d(
                "LiteRtEngine",
                "No NPU dispatch libraries found in $nativeLibDir. Skipping NPU to avoid native errors."
            )
            return null
        }

        return try {
            Log.d("LiteRtEngine", "Attempting NPU initialization...")
            val npuBackend = Backend.NPU(nativeLibraryDir = nativeLibDir)
            val npuConfig = EngineConfig(
                modelPath = config.modelPath,
                backend = npuBackend,
                visionBackend = if (config.maxNumImages > 0) npuBackend else null,
                maxNumImages = if (config.maxNumImages > 0) config.maxNumImages else null
            )
            val engine = Engine(npuConfig)
            engine.initialize()
            Log.d("LiteRtEngine", "NPU initialization successful")
            engine
        } catch (e: Throwable) {
            Log.w("LiteRtEngine", "NPU initialization failed: ${e.message}")
            null
        }
    }

    private fun tryInitializeGpu(config: ModelConfig): Engine? {
        return try {
            Log.d("LiteRtEngine", "Attempting GPU initialization...")
            val gpuBackend = Backend.GPU()
            val gpuConfig = EngineConfig(
                modelPath = config.modelPath,
                backend = gpuBackend,
                visionBackend = if (config.maxNumImages > 0) gpuBackend else null,
                maxNumImages = if (config.maxNumImages > 0) config.maxNumImages else null
            )
            val e = Engine(gpuConfig)
            e.initialize()
            Log.d("LiteRtEngine", "GPU initialization successful")
            e
        } catch (e: Throwable) {
            Log.w("LiteRtEngine", "GPU initialization failed: ${e.message}")
            null
        }
    }

    private fun initializeCpuEngine(modelPath: String, maxNumImages: Int): Engine {
        val processors = Runtime.getRuntime().availableProcessors()
        val optimalThreads = if (processors > 4) 4 else processors
        Log.d("LiteRtEngine", "Initializing CPU engine ($optimalThreads threads)...")
        val cpuConfig = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU(numOfThreads = optimalThreads),
            visionBackend = if (maxNumImages > 0) Backend.CPU() else null,
            maxNumImages = if (maxNumImages > 0) maxNumImages else null
        )
        val e = Engine(cpuConfig)
        e.initialize()
        Log.d("LiteRtEngine", "CPU initialization successful")
        return e
    }

    private fun checkIsLowConstraintDevice(): Boolean {
        // 1. RAM check: Devices with < 6.5GB RAM (effectively 6GB tier) struggle with LLM GPU allocation
        val totalRamGb = getSystemMetrics().getTotalRamGb()
        if (totalRamGb < 6.5f) {
            Log.d(
                "LiteRtEngine",
                "Low RAM device detected ($totalRamGb GB). Forcing CPU for stability."
            )
            return true
        }

        // 2. Hardware check: Detect chipsets with known GPU stability issues for LiteRT-LM
        val hardware = android.os.Build.HARDWARE.lowercase()
        val board = android.os.Build.BOARD.lowercase()
        val soc = hardware + board

        // Pixel 6 (G1), Pixel 7 (G2) frequently hang or produce gibberish in GPU mode
        val isTensorG1orG2 = soc.contains("gs101") || // Tensor G1
                soc.contains("gs201") || // Tensor G2
                soc.contains("whitechapel") // Early Tensor reference

        if (isTensorG1orG2) {
            Log.d("LiteRtEngine", "Tensor G1/G2 detected ($soc). Forcing CPU for stability.")
            return true
        }

        // 3. Mali GPU stability: Many mid-range Mali GPUs struggle with the heavy compute load of LLMs
        // and cause UI starvation/hangs.
        val isMali = hardware.contains("mali") || board.contains("mali")
        if (isMali && totalRamGb < 9f) { // Be conservative with Mali + < 12GB RAM
            Log.d(
                "LiteRtEngine",
                "Mid-range Mali GPU detected on $totalRamGb GB device. Forcing CPU."
            )
            return true
        }

        return false
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
