package `in`.ssverma.glee.features.skills

import androidx.compose.runtime.Immutable
import `in`.ssverma.glee.features.chat.domain.model.AiTool

@Immutable
data class ManageSkillsState(
    val skills: List<AiTool> = emptyList(),
    val activeSkills: Map<String, Boolean> = emptyMap()
)

sealed interface ManageSkillsIntent {
    data class ToggleSkill(val skillId: String, val enabled: Boolean) : ManageSkillsIntent
}
