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
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString

class LocalFileSystemSkill(
    private val fileSystem: FileSystem
) : AiSkill {
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
        emit(SkillResult.Progress(getString(Res.string.reading_file)))
        try {
            // Minimal implementation: checking if file exists
            val path = input.toPath()
            if (fileSystem.exists(path)) {
                emit(SkillResult.Success(getString(Res.string.file_exists_at, path.toString())))
            } else {
                emit(SkillResult.Error(getString(Res.string.file_not_found_at, path.toString())))
            }
        } catch (e: Exception) {
            emit(SkillResult.Error(getString(Res.string.failed_access_file, e.message ?: "")))
        }
    }
}
