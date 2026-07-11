package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hereliesaz.aznavrail.AzAppMeta
import kotlinx.browser.document
import org.w3c.dom.HTMLLinkElement

/**
 * Web (wasmJs): use the document title as the app name and the page's favicon (`<link rel="icon">`,
 * resolved to an absolute URL) as the icon — a plain URL string, which Coil3 loads over fetch. Both
 * degrade to defaults if the page doesn't set them.
 */
@Composable
internal actual fun rememberPlatformAppMeta(): AzAppMeta = remember {
    val title = document.title.ifBlank { AzAppMeta().name }
    val faviconEl = document.querySelector("link[rel~='icon']") as? HTMLLinkElement
    val favicon = faviconEl?.href?.ifBlank { null }
    AzAppMeta(name = title, icon = favicon, packageId = null)
}
