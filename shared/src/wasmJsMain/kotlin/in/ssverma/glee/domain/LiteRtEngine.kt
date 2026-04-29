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
async function createMediaPipeLlmInferenceJs(fileName) {
    console.log("[Glee] Initializing MediaPipe GenAI...");
    
    try {
        if (!navigator.gpu) {
            const msg = "WebGPU is NOT supported or enabled in your browser. MediaPipe GenAI requires WebGPU.";
            console.error("[Glee] " + msg);
            throw new Error(msg);
        }
        
        console.log("[Glee] Importing MediaPipe GenAI library from esm.sh...");
        // Use 0.10.20 for better WebGPU compatibility
        const mpGenAi = await eval('import("https://esm.sh/@mediapipe/tasks-genai@0.10.20")');
        const { FilesetResolver, LlmInference } = mpGenAi;
        
        console.log("[Glee] Resolving GenAI WASM files from jsdelivr...");
        const genaiFileset = await FilesetResolver.forGenAiTasks(
            "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-genai@0.10.20/wasm"
        );
        
        console.log("[Glee] Fetching model from OPFS: " + fileName);
        const dir = await navigator.storage.getDirectory();
        const fileHandle = await dir.getFileHandle(fileName);
        const file = await fileHandle.getFile();
        console.log("[Glee] Model file size: " + (file.size / (1024 * 1024)).toFixed(2) + " MB");
        
        const url = URL.createObjectURL(file);
        
        console.log("[Glee] Initializing LlmInference instance...");
        const llmInference = await LlmInference.createFromOptions(genaiFileset, {
            baseOptions: {
                modelAssetPath: url
            }
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
internal external fun createMediaPipeLlmInferenceJs(fileName: String): Promise<JsAny?>

@JsFun(
    """
async function generateResponseJs(llmInference, prompt, onChunk) {
    await llmInference.generateResponse(prompt, (partialResult, done) => {
        onChunk(partialResult, done);
    });
}
"""
)
internal external fun generateResponseJs(
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
internal external fun closeLlmInferenceJs(llmInference: JsAny)


actual class LiteRtEngine actual constructor() : AiEngine {

    private var llmInference: JsAny? = null

    actual override suspend fun loadModel(config: ModelConfig): Result<Unit> {
        return try {
            val fileName = config.modelPath.toPath().name
            llmInference = createMediaPipeLlmInferenceJs(fileName).await<JsAny?>()
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
