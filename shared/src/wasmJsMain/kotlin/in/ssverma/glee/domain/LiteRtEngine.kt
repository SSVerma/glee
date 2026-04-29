@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package `in`.ssverma.glee.domain

import `in`.ssverma.glee.features.chat.domain.model.AiSkill
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okio.Path.Companion.toPath
import kotlin.js.Promise

@JsFun(
    """
async function createMediaPipeLlmInferenceJs(fileName, temperature, topK, topP, maxNumImages) {
    console.log("[Glee] Initializing MediaPipe GenAI...");
    
    try {
        if (!navigator.gpu) {
            throw new Error("WebGPU is NOT supported or enabled in your browser.");
        }
        
        console.log("[Glee] Importing MediaPipe GenAI library...");
        const mpGenAi = await eval('import("https://esm.sh/@mediapipe/tasks-genai")');
        const { FilesetResolver, LlmInference } = mpGenAi;
        
        console.log("[Glee] Resolving GenAI WASM files...");
        const genaiFileset = await FilesetResolver.forGenAiTasks(
            "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-genai/wasm"
        );
        
        const dir = await navigator.storage.getDirectory();
        const fileHandle = await dir.getFileHandle(fileName);
        const file = await fileHandle.getFile();
        const url = URL.createObjectURL(file);
        
        console.log("[Glee] Creating LlmInference instance...");
        const llmInference = await LlmInference.createFromOptions(genaiFileset, {
            baseOptions: {
                modelAssetPath: url
            },
            maxTokens: 1024, // Reasonable default for better performance
            temperature: temperature,
            topK: topK,
            topP: topP || 1.0,
            maxNumImages: maxNumImages || 1
        });
        
        console.log("[Glee] LlmInference initialized successfully!");
        URL.revokeObjectURL(url);
        return llmInference;
    } catch (e) {
        console.error("[Glee] LiteRtEngine Load Error:", e);
        throw e;
    }
}
"""
)

external fun createMediaPipeLlmInferenceJs(
    fileName: String,
    temperature: Float,
    topK: Int,
    topP: Float,
    maxNumImages: Int
): Promise<JsAny?>

@JsFun(
    """
async function generateResponseJs(llmInference, prompt, onChunk) {
    let lastLength = 0;
    let buffer = "";
    let lastSendTime = Date.now();
    
    await llmInference.generateResponse(prompt, (partialResult, done) => {
        const delta = partialResult.substring(lastLength);
        lastLength = partialResult.length;
        buffer += delta;
        
        const now = Date.now();
        // Send every 50ms or if it's the final chunk
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

actual class LiteRtEngine actual constructor() : AiEngine {

    private var llmInference: JsAny? = null

    actual override suspend fun loadModel(config: ModelConfig): Result<Unit> {
        return try {
            val fileName = config.modelPath.toPath().name
            llmInference = createMediaPipeLlmInferenceJs(
                fileName = fileName,
                temperature = config.temperature,
                topK = config.topK,
                topP = 0.95f, // Default topP for smoother generation
                maxNumImages = config.maxNumImages
            ).await()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    actual override fun generateResponse(
        prompt: String,
        files: List<AttachedFile>
    ): Flow<AiChunk> = callbackFlow {
        val instance = llmInference
        if (instance == null) {
            trySend(AiChunk("Model not loaded.", isFinal = true))
            close()
            return@callbackFlow
        }

        try {
            generateResponseJs(instance, prompt) { partialResult, done ->
                trySend(AiChunk(text = partialResult, isFinal = done))
                if (done) {
                    close()
                }
            }
        } catch (e: Throwable) {
            trySend(AiChunk("Error: ${e.message}", isFinal = true))
            close(e)
        }

        awaitClose { }
    }

    actual override fun setSystemPrompt(prompt: String) {
        // MediaPipe Tasks GenAI currently doesn't expose system prompt explicitly.
        // It relies on standard formatted chat prompt (which AiChatManager usually formats).
    }

    actual override fun setSkills(skills: List<AiSkill>) {
    }

    actual override suspend fun clearConversation() {
    }

    actual override suspend fun close() {
        llmInference?.let {
            closeLlmInferenceJs(it)
        }
        llmInference = null
    }
}
