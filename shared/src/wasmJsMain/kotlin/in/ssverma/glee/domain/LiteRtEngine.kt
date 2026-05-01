@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package `in`.ssverma.glee.domain

import `in`.ssverma.glee.features.chat.domain.model.AiSkill
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import kotlin.js.Promise

// Caching MediaPipe initialization to avoid redundant downloads/initialization
private var cachedMpGenAi: JsAny? = null
private var cachedGenaiFileset: JsAny? = null

@JsFun(
    """
async function createMediaPipeLlmInferenceJs(fileName, temperature, topK, topP, maxNumImages, cachedObjects) {
    console.log("[Glee] Initializing MediaPipe GenAI...");
    
    try {
        if (!navigator.gpu) {
            throw new Error("WebGPU is NOT supported or enabled in your browser.");
        }
        
        let mpGenAi = cachedObjects.mpGenAi;
        if (!mpGenAi) {
            console.log("[Glee] Importing MediaPipe GenAI library...");
            mpGenAi = await eval('import("https://esm.sh/@mediapipe/tasks-genai")');
            cachedObjects.mpGenAi = mpGenAi;
        }

        const { FilesetResolver, LlmInference } = mpGenAi;
        
        let genaiFileset = cachedObjects.genaiFileset;
        if (!genaiFileset) {
            console.log("[Glee] Resolving GenAI WASM files...");
            genaiFileset = await FilesetResolver.forGenAiTasks(
                "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-genai/wasm"
            );
            cachedObjects.genaiFileset = genaiFileset;
        }
        
        const dir = await navigator.storage.getDirectory();
        const fileHandle = await dir.getFileHandle(fileName);
        const file = await fileHandle.getFile();
        const url = URL.createObjectURL(file);
        
        const llmInference = await LlmInference.createFromOptions(genaiFileset, {
            baseOptions: {
                modelAssetPath: url
            },
            maxTokens: 1024,
            temperature: temperature,
            topK: topK,
            topP: topP,
            maxNumImages: maxNumImages || 1
        });
        
        URL.revokeObjectURL(url);
        return llmInference;
    } catch (e) {
        console.error("[Glee] LiteRtEngine Load Error:", e);
        let message = e.message || "Unknown error during model initialization";
        if (message.includes("No model format matched")) {
            message = "Unsupported model format (.task, .litertlm, or .bin required).";
        } else if (message.includes("WebGPU")) {
            message = "WebGPU not available. Check browser settings.";
        }
        throw new Error(message);
    }
}
"""
)
external fun createMediaPipeLlmInferenceJs(
    fileName: String,
    temperature: Float,
    topK: Int,
    topP: Float,
    maxNumImages: Int,
    cachedObjects: JsAny
): Promise<JsAny?>

@JsFun(
    """
async function generateResponseJs(llmInference, prompt, onChunk) {
    let lastLength = 0;
    let buffer = "";
    let lastSendTime = Date.now();
    
    // We await this so the Kotlin side can catch rejections
    await llmInference.generateResponse(prompt, (partialResult, done) => {
        const delta = partialResult.substring(lastLength);
        lastLength = partialResult.length;
        buffer += delta;
        
        const now = Date.now();
        // Throttle UI updates to 50ms to maintain 60fps
        if (now - lastSendTime > 50 || done) {
            onChunk(buffer, done);
            buffer = "";
            lastSendTime = now;
        }
    });
}
"""
)
external fun generateResponseJs(
    llmInference: JsAny,
    prompt: String,
    onChunk: (String, Boolean) -> Unit
): Promise<JsAny?>

@JsFun(
    """
function closeLlmInferenceJs(llmInference) {
    if (llmInference && llmInference.close) {
        llmInference.close();
    }
}
"""
)
external fun closeLlmInferenceJs(llmInference: JsAny)

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun jsPut(obj: JsAny, key: JsString, value: JsAny)

@JsFun("(obj, key) => obj[key]")
private external fun jsGet(obj: JsAny, key: JsString): JsAny?

@JsFun("() => ({})")
private external fun createJsObject(): JsAny

actual class LiteRtEngine actual constructor() : AiEngine {

    private var llmInference: JsAny? = null

    actual override suspend fun loadModel(config: ModelConfig): Result<Unit> {
        return try {
            val fileName = config.modelPath.toPath().name

            val cache = createJsObject()
            cachedMpGenAi?.let { jsPut(cache, "mpGenAi".toJsString(), it) }
            cachedGenaiFileset?.let { jsPut(cache, "genaiFileset".toJsString(), it) }

            val result = createMediaPipeLlmInferenceJs(
                fileName = fileName,
                temperature = config.temperature,
                topK = config.topK,
                topP = 0.95f,
                maxNumImages = config.maxNumImages,
                cachedObjects = cache
            ).await<JsAny?>()

            // Persist the libraries in the global cache
            cachedMpGenAi = jsGet(cache, "mpGenAi".toJsString())
            cachedGenaiFileset = jsGet(cache, "genaiFileset".toJsString())

            llmInference = result
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    actual override fun generateResponse(
        prompt: String,
        files: List<AttachedFile>
    ): Flow<AiChunk> = callbackFlow {
        val instance = llmInference ?: run {
            trySend(AiChunk("Model not loaded.", isFinal = true))
            close()
            return@callbackFlow
        }

        // We launch a coroutine to await the JS promise so we can catch crashes
        val job = launch {
            try {
                generateResponseJs(instance, prompt) { partialResult, done ->
                    val result = trySend(AiChunk(text = partialResult, isFinal = done))
                    if (done || result.isFailure) {
                        close()
                    }
                }.await<JsAny?>()
            } catch (e: Throwable) {
                trySend(AiChunk("Generation Error: ${e.message}", isFinal = true))
                close(e)
            }
        }

        awaitClose {
            job.cancel()
        }
    }

    actual override fun setSystemPrompt(prompt: String) {
        // MediaPipe Tasks GenAI currently doesn't expose system prompt explicitly.
    }

    actual override fun setSkills(skills: List<AiSkill>) {}

    actual override suspend fun clearConversation() {}

    actual override suspend fun close() {
        llmInference?.let {
            closeLlmInferenceJs(it)
        }
        llmInference = null
    }
}
