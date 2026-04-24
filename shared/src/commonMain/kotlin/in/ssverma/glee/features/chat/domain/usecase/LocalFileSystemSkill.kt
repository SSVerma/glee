package `in`.ssverma.glee.features.chat.domain.usecase

import `in`.ssverma.glee.core.common.platform.FileSystem
import `in`.ssverma.glee.features.chat.domain.model.SkillResult
import `in`.ssverma.glee.features.chat.domain.model.AiSkill
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.failed_access_file
import glee.shared.generated.resources.file_exists_at
import glee.shared.generated.resources.file_not_found_at
import glee.shared.generated.resources.reading_file
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.compose.resources.getString

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class LocalFileSystemSkill(
    private val fileSystem: FileSystem,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : AiSkill {
    override val id = "local_file_system"
    override val name = "Local File System"
    override val description = "Check if a file exists in the application directory. Safety: Restricted to app-specific folders."
    override val parameterSchema = """
        {
          "type": "object",
          "properties": {
            "fileName": { "type": "string", "description": "The name of the file to check inside the app directory" }
          },
          "required": ["fileName"]
        }
    """.trimIndent()

    @Serializable
    private data class Input(val fileName: String)

    override suspend fun execute(input: String): Flow<SkillResult> = flow {
        emit(SkillResult.Progress(getString(Res.string.reading_file)))
        try {
            val skillInput = try {
                json.decodeFromString<Input>(input)
            } catch (_: Exception) {
                // Fallback for simple string input if LLM fails to provide JSON
                Input(fileName = input.trim().removeSurrounding("\""))
            }

            val path = fileSystem.appDataDir.resolve(skillInput.fileName)
            
            if (fileSystem.exists(path)) {
                emit(SkillResult.Success(getString(Res.string.file_exists_at, path.name)))
            } else {
                emit(SkillResult.Error(getString(Res.string.file_not_found_at, path.name)))
            }
        } catch (e: Exception) {
            emit(SkillResult.Error(getString(Res.string.failed_access_file, e.message ?: "")))
        }
    }
}
