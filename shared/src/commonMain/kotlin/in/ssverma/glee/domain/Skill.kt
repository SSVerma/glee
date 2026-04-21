package `in`.ssverma.glee.domain

import `in`.ssverma.glee.domain.model.SkillResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining a "Skill" (Tool) that the AI can invoke.
 * This is used for LLM Tool Calling (Function Calling).
 */
interface Skill {
    val id: String
    val name: String
    val description: String
    
    /**
     * JSON Schema describing the input parameters for the LLM.
     */
    val parameterSchema: String

    suspend fun execute(input: String): Flow<SkillResult>
}
