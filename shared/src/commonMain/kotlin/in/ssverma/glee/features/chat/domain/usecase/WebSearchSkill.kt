package `in`.ssverma.glee.features.chat.domain.usecase

import `in`.ssverma.glee.features.chat.domain.model.AiSkill
import `in`.ssverma.glee.features.chat.domain.model.SkillResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class WebSearchSkill(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : AiSkill {
    override val id = "web_search"
    override val name = "Web Search"
    override val description = "Search the web for up-to-date information."
    override val parameterSchema = """
        {
          "type": "object",
          "properties": {
            "query": { "type": "string", "description": "The search query" }
          },
          "required": ["query"]
        }
    """.trimIndent()

    @Serializable
    private data class Input(val query: String)

    override suspend fun execute(input: String): Flow<SkillResult> = flow {
        val skillInput = try {
            json.decodeFromString<Input>(input)
        } catch (_: Exception) {
            Input(query = input.trim().removeSurrounding("\""))
        }

        emit(SkillResult.Progress("Searching the web for: ${skillInput.query}..."))
        // Placeholder for real search API
        emit(SkillResult.Success("Information found for '${skillInput.query}': Glee is a private AI assistant. (Simulated Search)"))
    }
}
