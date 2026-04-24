package `in`.ssverma.glee.features.chat.domain.usecase

import `in`.ssverma.glee.features.chat.domain.model.AiSkill
import `in`.ssverma.glee.core.common.platform.FileSystem
import kotlinx.serialization.json.Json

/**
 * A central place to define and manage available AI skills/tools.
 */
object GleeSkills {
    /**
     * Returns the list of default skills available in Glee.
     */
    fun getDefaultSkills(fileSystem: FileSystem, json: Json): List<AiSkill> {
        return listOf(
            LocalFileSystemSkill(fileSystem, json),
            WebSearchSkill(json)
        )
    }
}
