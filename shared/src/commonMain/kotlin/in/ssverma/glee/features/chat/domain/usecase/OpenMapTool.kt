package `in`.ssverma.glee.features.chat.domain.usecase

import `in`.ssverma.glee.core.common.platform.UrlLauncher
import `in`.ssverma.glee.features.chat.domain.model.AiTool
import `in`.ssverma.glee.features.chat.domain.model.ToolResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenMapTool(
    private val json: Json,
    private val urlLauncher: UrlLauncher
) : AiTool {

    override val id: String = "open_map"
    override val name: String = "Open Map"
    override val description: String = "Opens a map application or website for a given location or query."
    
    override val parameterSchema: String = """
        {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "The location to search for on the map (e.g., 'Bangalore', 'Eiffel Tower', 'India')."
                }
            },
            "required": ["query"]
        }
    """.trimIndent()

    override suspend fun execute(input: String): Flow<ToolResult> = flow {
        emit(ToolResult.Progress("Parsing location query..."))
        try {
            val query = if (input.trim().startsWith("{")) {
                val element = json.parseToJsonElement(input).jsonObject
                element["query"]?.jsonPrimitive?.content ?: element["input"]?.jsonPrimitive?.content ?: input
            } else {
                input
            }
            
            emit(ToolResult.Progress("Opening map for: $query..."))
            
            // Provide a universally handled query string for maps
            val encodedQuery = query.replace(" ", "+")
            val url = "https://maps.google.com/?q=$encodedQuery"
            val success = urlLauncher.launchUrl(url)
            
            if (success) {
                emit(ToolResult.Success("Successfully opened the map application for $query."))
            } else {
                emit(ToolResult.Error("Failed to open the map application."))
            }
        } catch (e: Exception) {
            emit(ToolResult.Error("Error processing map query: ${e.message}"))
        }
    }
}
