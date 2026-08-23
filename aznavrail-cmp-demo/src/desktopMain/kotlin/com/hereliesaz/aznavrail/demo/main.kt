package com.hereliesaz.aznavrail.demo

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.skia.Image as SkiaImage

// icon.png (src/desktopMain/resources/) is on the JVM classpath at
// runtime via the desktopMain source set's own resources dir — no
// Compose Resources Gradle plugin/codegen needed for a single JVM-only
// icon, unlike a Compose Multiplatform commonMain drawable would.
private fun loadWindowIcon() =
    object {}.javaClass.getResourceAsStream("/icon.png")?.use { stream ->
        BitmapPainter(SkiaImage.makeFromEncoded(stream.readBytes()).toComposeImageBitmap())
    }

// Desktop entry point. Run with: ./gradlew :aznavrail-cmp-demo:run
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "AzNavRail Demo", icon = loadWindowIcon()) {
        App()
    }
}
