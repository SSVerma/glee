package `in`.ssverma.glee.features.skills

import androidx.lifecycle.ViewModel
import `in`.ssverma.glee.features.chat.domain.model.AiTool
import `in`.ssverma.glee.features.chat.domain.usecase.AiChatManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ManageToolsViewModel(
    private val chatManager: AiChatManager,
    private val skills: List<AiTool>
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ManageSkillsState(
        skills = skills,
        activeSkills = skills.associate { it.id to true }
    ))
    val uiState: StateFlow<ManageSkillsState> = _uiState.asStateFlow()

    fun onIntent(intent: ManageSkillsIntent) {
        when (intent) {
            is ManageSkillsIntent.ToggleSkill -> {
                chatManager.toggleSkill(intent.skillId, intent.enabled)
                _uiState.update { it.copy(activeSkills = it.activeSkills + (intent.skillId to intent.enabled)) }
            }
        }
    }
}
