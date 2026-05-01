package `in`.ssverma.glee.features.chat.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow

@Immutable
interface AiTool {
    val id: String
    val name: String
    val description: String
    val parameterSchema: String
    suspend fun execute(input: String): Flow<ToolResult>
}
