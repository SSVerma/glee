@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package `in`.ssverma.glee.domain

import `in`.ssverma.glee.features.chat.domain.model.AiSkill
import `in`.ssverma.glee.features.chat.domain.model.AttachedFile
import `in`.ssverma.glee.features.chat.domain.model.ModelConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.await
import kotlin.js.Promise
import okio.Path.Companion.toPath

@JsFun("""
async function createMediaPipeLlmInferenceJs(fileName) {
    const { FilesetResolver, LlmInference } = await import('@mediapipe/tasks-genai');
    
    const genaiFileset = await FilesetResolver.forGenAiTasks(
        "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-genai/wasm"
    );
    
    const dir = await navigator.storage.getDirectory();
    const fileHandle = await dir.getFileHandle(fileName);
    const file = await fileHandle.getFile();
    const url = URL.createObjectURL(file);
    
    const llmInference = await LlmInference.createFromOptions(genaiFileset, {
        baseOptions: {
            modelAssetPath: url
        }
    });
    
    URL.revokeObjectURL(url);
    return llmInference;
}
""")
internal external fun createMediaPipeLlmInferenceJs(fileName: String): Promise<JsAny?>

@JsFun("""
async function generateResponseJs(llmInference, prompt, onChunk) {
    await llmInference.generateResponse(prompt, (partialResult, done) => {
        onChunk(partialResult, done);
    });
}
""")
internal external fun generateResponseJs(llmInference: JsAny, prompt: String, onChunk: (String, Boolean) -> Unit): Promise<JsAny?>

@JsFun("""
function closeLlmInferenceJs(llmInference) {
    if (llmInference && llmInference.close) {
        llmInference.close();
    }
}
""")
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
