package `in`.ssverma.glee.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import `in`.ssverma.glee.features.chat.ui.ChatScreen
import `in`.ssverma.glee.features.chat.ui.ChatViewModel
import `in`.ssverma.glee.features.models.ModelManagementScreen
import `in`.ssverma.glee.features.splash.SplashScreen
import `in`.ssverma.glee.features.settings.SettingsScreen
import `in`.ssverma.glee.features.skills.ManageSkillsScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val GleeNavConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Splash::class)
            subclass(ModelManagement::class)
            subclass(Chat::class)
            subclass(ManageTools::class)
            subclass(Settings::class)
        }
    }
}

@Composable
fun RootNavHost(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(configuration = GleeNavConfig, Splash)

    NavDisplay(
        backStack = backStack,
        modifier = modifier
    ) { key ->
        val entry: NavEntry<NavKey> = when (key) {
            Splash -> NavEntry(key) {
                SplashScreen(onSplashComplete = {
                    backStack.clear()
                    backStack.add(Chat)
                })
            }
            ModelManagement -> NavEntry(key) {
                ModelManagementScreen(onClose = { 
                    if (backStack.size > 1) {
                        backStack.removeAt(backStack.size - 1)
                    } else { 
                        backStack.clear()
                        backStack.add(Chat) 
                    } 
                })
            }
            Chat -> NavEntry(key) {
                ChatScreen(
                    viewModel = viewModel,
                    onModelManagement = { backStack.add(ModelManagement) },
                    onManageSkills = { backStack.add(ManageTools) },
                    onSettings = { backStack.add(Settings) }
                )
            }
            ManageTools -> NavEntry(key) {
                ManageSkillsScreen(onBack = { backStack.removeAt(backStack.size - 1) })
            }
            Settings -> NavEntry(key) {
                SettingsScreen(
                    onBack = { backStack.removeAt(backStack.size - 1) },
                    onModelManagement = { backStack.add(ModelManagement) },
                    onManageSkills = { backStack.add(ManageTools) }
                )
            }
            else -> NavEntry(key) { }
        }
        entry
    }
}
