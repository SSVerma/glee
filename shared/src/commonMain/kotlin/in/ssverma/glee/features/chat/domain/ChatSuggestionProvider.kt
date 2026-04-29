package `in`.ssverma.glee.features.chat.domain

import glee.shared.generated.resources.Res
import glee.shared.generated.resources.suggestion_book
import glee.shared.generated.resources.suggestion_budget
import glee.shared.generated.resources.suggestion_cleaning
import glee.shared.generated.resources.suggestion_coding
import glee.shared.generated.resources.suggestion_email
import glee.shared.generated.resources.suggestion_gift
import glee.shared.generated.resources.suggestion_history
import glee.shared.generated.resources.suggestion_joke
import glee.shared.generated.resources.suggestion_language
import glee.shared.generated.resources.suggestion_meditation
import glee.shared.generated.resources.suggestion_productivity
import glee.shared.generated.resources.suggestion_recipe
import glee.shared.generated.resources.suggestion_travel
import glee.shared.generated.resources.suggestion_trip
import glee.shared.generated.resources.suggestion_workout
import org.jetbrains.compose.resources.getString

class ChatSuggestionProvider {
    private val allSuggestionResources = listOf(
        Res.string.suggestion_trip,
        Res.string.suggestion_recipe,
        Res.string.suggestion_email,
        Res.string.suggestion_workout,
        Res.string.suggestion_book,
        Res.string.suggestion_gift,
        Res.string.suggestion_productivity,
        Res.string.suggestion_coding,
        Res.string.suggestion_history,
        Res.string.suggestion_travel,
        Res.string.suggestion_language,
        Res.string.suggestion_joke,
        Res.string.suggestion_meditation,
        Res.string.suggestion_budget,
        Res.string.suggestion_cleaning
    )

    suspend fun getSuggestions(count: Int = 4): List<String> {
        val shuffled = allSuggestionResources.shuffled().take(count)
        return shuffled.map { getString(it) }
    }
}
