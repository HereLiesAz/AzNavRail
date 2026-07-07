package com.hereliesaz.aznavrail.demo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

// Web (wasmJs) entry point. Run with: ./gradlew :aznavrail-cmp-demo:wasmJsBrowserDevelopmentRun
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App()
    }
}
