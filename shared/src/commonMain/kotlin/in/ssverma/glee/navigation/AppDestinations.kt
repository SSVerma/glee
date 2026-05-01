package `in`.ssverma.glee.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface GleeDestination : NavKey

@Serializable
data object Splash : GleeDestination

@Serializable
data object ModelManagement : GleeDestination

@Serializable
data object Chat : GleeDestination

@Serializable
data object ManageTools : GleeDestination

@Serializable
data object Settings : GleeDestination
