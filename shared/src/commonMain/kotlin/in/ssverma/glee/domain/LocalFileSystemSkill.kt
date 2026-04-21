package `in`.ssverma.glee.domain

import `in`.ssverma.glee.di.platformFileSystem
import `in`.ssverma.glee.domain.model.SkillResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.Path.Companion.toPath

class LocalFileSystemSkill : Skill {
    override val id = "local_file_system"
    override val name = "Local File System"
    override val description = "Read and analyze files from the device storage."
    override val parameterSchema = """
        {
          "type": "object",
          "properties": {
            "path": { "type": "string", "description": "The absolute path to the file" }
          },
          "required": ["path"]
        }
    """.trimIndent()

    override suspend fun execute(input: String): Flow<SkillResult> = flow {
        emit(SkillResult.Progress("Reading file..."))
        try {
            // Minimal implementation: checking if file exists
            val path = input.toPath()
            if (platformFileSystem.exists(path)) {
                emit(SkillResult.Success("File exists at $path"))
            } else {
                emit(SkillResult.Error("File not found at $path"))
            }
        } catch (e: Exception) {
            emit(SkillResult.Error("Failed to access file: ${e.message}"))
        }
    }
}
