package `in`.ssverma.glee.features.chat.domain.usecase

import `in`.ssverma.glee.core.common.platform.UrlLauncher
import `in`.ssverma.glee.features.chat.domain.model.AiTool
import kotlinx.serialization.json.Json

/**
 * A central place to define and manage available AI skills/tools.
 */
object GleeTools {
    /**
     * Returns the list of default skills available in Glee.
     */
    fun getDefaultSkills(
        urlLauncher: UrlLauncher,
        json: Json
    ): List<AiTool> {
        return listOf(
            OpenMapTool(json, urlLauncher)
        )
    }
}
