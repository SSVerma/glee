package `in`.ssverma.glee.features.chat.domain.usecase

import `in`.ssverma.glee.domain.AiChunk
import `in`.ssverma.glee.domain.AiEngine
import `in`.ssverma.glee.features.chat.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Handles the agentic loop: generating responses, detecting tool calls, 
 * executing skills, and feeding results back into the model.
 */
class AgentProcessor(
    private val engine: AiEngine,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val maxIterations = 5

    fun process(
        prompt: String,
        skills: Map<String, AiSkill>,
        systemPrompt: String
    ): Flow<AgenticEvent> = flow {
        val agenticSystemPrompt = buildAgenticSystemPrompt(systemPrompt, skills.values.toList())
        engine.setSystemPrompt(agenticSystemPrompt)

        var currentIteration = 0
        var currentPrompt = prompt
        var isLooping = true

        while (isLooping && currentIteration < maxIterations) {
            currentIteration++
            var collectedText = ""
            var toolCall: ToolCall? = null
            var emittedTextLength = 0

            engine.generateResponse(currentPrompt).collect { chunk ->
                collectedText += chunk.text
                
                if (chunk.isFinal && !collectedText.contains("<tool_call>")) {
                    val remainingText = collectedText.substring(emittedTextLength)
                    if (remainingText.isNotEmpty()) {
                        emit(AgenticEvent.ResponseChunk(AiChunk(text = remainingText)))
                    }
                    return@collect
                }

                if (!collectedText.contains("<tool_call>") && !isPartialTag(collectedText)) {
                    val textToEmit = collectedText.substring(emittedTextLength)
                    if (textToEmit.isNotEmpty()) {
                        emit(AgenticEvent.ResponseChunk(AiChunk(text = textToEmit)))
                        emittedTextLength = collectedText.length
                    }
                } else if (collectedText.contains("<tool_call>")) {
                    val tagIndex = collectedText.indexOf("<tool_call>")
                    if (emittedTextLength < tagIndex) {
                        val remainingNormalText = collectedText.substring(emittedTextLength, tagIndex)
                        if (remainingNormalText.isNotEmpty()) {
                            emit(AgenticEvent.ResponseChunk(AiChunk(text = remainingNormalText)))
                        }
                        emittedTextLength = tagIndex
                    }

                    // Inside or after a tool call tag
                    val detected = detectToolCall(collectedText)
                    if (detected != null && toolCall == null) {
                        toolCall = detected
                        emit(AgenticEvent.ToolCallDetected(detected))
                    }
                }
            }

            if (toolCall != null) {
                val currentToolCall = toolCall!!
                val skill = skills[currentToolCall.skillId]
                if (skill != null) {
                    val thoughtText = collectedText.substringBefore("<tool_call>").trim()
                    if (thoughtText.isNotEmpty()) {
                        emit(AgenticEvent.Thought(thoughtText))
                    }

                    emit(AgenticEvent.ToolExecutionStarted(currentToolCall.skillId))
                    
                    var resultText = ""
                    skill.execute(currentToolCall.input).collect { result ->
                        when (result) {
                            is SkillResult.Success -> {
                                resultText = result.message
                            }
                            is SkillResult.Error -> {
                                resultText = "Error: ${result.message}"
                            }
                            is SkillResult.Progress -> {
                                emit(AgenticEvent.ToolProgress(result.message))
                            }
                        }
                    }

                    emit(AgenticEvent.ToolResultReceived(currentToolCall.skillId, resultText))

                    // Prepare next turn
                    currentPrompt = "Observation: $resultText\n\nContinue to answer the user request."
                } else {
                    emit(AgenticEvent.Error("Skill not found: ${currentToolCall.skillId}"))
                    isLooping = false
                }
            } else {
                isLooping = false
            }
        }
        
        // Signal completion of the entire agentic turn
        emit(AgenticEvent.ResponseChunk(AiChunk(isFinal = true)))
    }

    private fun isPartialTag(text: String): Boolean {
        val tag = "<tool_call>"
        for (i in tag.length downTo 1) {
            if (text.endsWith(tag.substring(0, i))) return true
        }
        return false
    }

    private fun buildAgenticSystemPrompt(basePrompt: String, skills: List<AiSkill>): String {
        if (skills.isEmpty()) return basePrompt

        val toolDefinitions = skills.joinToString("\n") { skill ->
            "- ${skill.id}: ${skill.description}. Input schema: ${skill.parameterSchema}"
        }

        return """
            $basePrompt
            
            # IDENTITY
            You are Glee, an autonomous AI assistant capable of using tools to provide real-time information.
            
            # TOOLS
            You have access to the following local tools:
            $toolDefinitions
            
            # MANDATORY TOOL USE RULE
            1. If the user asks for real-time information (weather, news, local places, food, etc.), YOU MUST use 'web_search'.
            2. If you need to check files in the local system, YOU MUST use 'local_file_system'.
            3. To call a tool, you MUST output ONLY the following JSON block:
               <tool_call>{"name": "TOOL_ID", "input": "YOUR_INPUT"}</tool_call>
            4. DO NOT attempt to answer from your internal knowledge for real-time queries.
            5. ALWAYS think step-by-step before calling a tool.
            
            # CONTEXT
            Always remember previous details from this conversation (like locations, preferences, or names) when using tools.
            
            Example: If the user previously mentioned 'Delhi', and now asks for 'nearest food', search for 'food in Delhi'.
        """.trimIndent()
    }

    private fun detectToolCall(text: String): ToolCall? {
        val startTag = "<tool_call>"
        val endTag = "</tool_call>"
        
        if (text.contains(startTag) && text.contains(endTag)) {
            try {
                val jsonStr = text.substringAfter(startTag).substringBefore(endTag)
                val jsonElement = json.parseToJsonElement(jsonStr).jsonObject
                return ToolCall(
                    skillId = jsonElement["name"]?.jsonPrimitive?.content ?: return null,
                    input = jsonElement["input"]?.jsonPrimitive?.content ?: ""
                )
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }
}

sealed interface AgenticEvent {
    data class ResponseChunk(val chunk: AiChunk) : AgenticEvent
    data class Thought(val text: String) : AgenticEvent
    data class ToolCallDetected(val toolCall: ToolCall) : AgenticEvent
    data class ToolExecutionStarted(val skillId: String) : AgenticEvent
    data class ToolProgress(val message: String) : AgenticEvent
    data class ToolResultReceived(val skillId: String, val result: String) : AgenticEvent
    data class Error(val message: String) : AgenticEvent
}
