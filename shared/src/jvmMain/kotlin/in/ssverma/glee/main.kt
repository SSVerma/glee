package `in`.ssverma.glee

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import `in`.ssverma.glee.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Glee",
        ) {
            App()
        }
    }
}
