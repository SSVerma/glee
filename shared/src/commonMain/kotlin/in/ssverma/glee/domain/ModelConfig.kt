package `in`.ssverma.glee.domain

import kotlinx.serialization.Serializable

/**
 * Configuration for the LiteRT-LM (Gemma) model.
 *
 * @param modelPath The local path to the .bin model file.
 * @param temperature Sampling temperature (higher = more creative).
 * @param topK Top-K sampling parameter.
 * @param maxTokens Maximum number of tokens to generate.
 */
@Serializable
data class ModelConfig(
    val modelPath: String,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val maxTokens: Int = 1024
)
