package com.hereliesaz.aznavrail.demo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

// Desktop entry point. Run with: ./gradlew :aznavrail-cmp-demo:run
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "AzNavRail Demo") {
        App()
    }
}
