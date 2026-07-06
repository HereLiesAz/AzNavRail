package com.hereliesaz.aznavrail.internal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.AzLoad
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzMoreFromApp
import com.hereliesaz.aznavrail.service.MoreFromAzRepository
import com.hereliesaz.aznavrail.LocalAzSafeZones

/**
 * Full-screen "More from Az" overlay: a Material 3 expressive carousel of the library author's other
 * apps. The cards are not a selection model — **tapping a card opens that app** (its website/PWA, else
 * Play, else the GitHub repo). Data comes from the CI-versioned `more-from-az.json` via
 * [MoreFromAzRepository]; each card shows that app's own icon (never the owner's GitHub avatar — a
 * blank/avatar icon falls back to the app's initials) and its name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoreFromAzOverlay(
    jsonUrl: String,
    scope: AzNavRailScopeImpl,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val accent = scope.activeColor.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.primary
    val surface = scope.translucentBackground.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.surface
    val safe = LocalAzSafeZones.current

    BackHandler(enabled = true) { onDismiss() }

    val apps by produceState<List<AzMoreFromApp>?>(initialValue = null, jsonUrl) {
        value = MoreFromAzRepository.fetch(jsonUrl).getOrNull()?.apps ?: emptyList()
    }
    // Note: `.apps` is the resolved app list; link-only manifest entries are enriched at fetch time.

    Box(Modifier.fillMaxSize().background(surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = safe.top, bottom = safe.bottom)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = accent,
                    modifier = Modifier.clickable { onDismiss() }.padding(end = 12.dp)
                )
                Text(
                    text = "More from Az",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
            Spacer(Modifier.height(16.dp))

            val list = apps
            when {
                list == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { AzLoad() }
                list.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        "Couldn't load apps right now.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                else -> {
                    // The cards aren't a selection model: tapping one opens that app directly.
                    val carouselState = rememberCarouselState { list.size }
                    HorizontalMultiBrowseCarousel(
                        state = carouselState,
                        preferredItemWidth = 140.dp,
                        itemSpacing = 12.dp,
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(196.dp),
                    ) { index ->
                        val app = list[index]
                        AppCard(
                            app = app,
                            accent = accent,
                            onClick = { app.primaryUrl?.let { openUrl(uriHandler, it) } },
                        )
                    }
                }
            }
        }
    }
}

/** The app a card opens when tapped: prefer the website/PWA, then Play, then the GitHub repo. */
private val AzMoreFromApp.primaryUrl: String?
    get() = webUrl ?: playStoreUrl ?: githubUrl

/** True only for a genuine app icon — never the owner's GitHub avatar (which is not an app icon). */
private fun isAppIcon(url: String): Boolean =
    url.isNotBlank() && !url.contains("avatars.githubusercontent.com", ignoreCase = true)

@Composable
private fun AppCard(app: AzMoreFromApp, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(shape)
                .border(width = 1.dp, color = accent.copy(alpha = 0.4f), shape = shape),
            contentAlignment = Alignment.Center,
        ) {
            if (isAppIcon(app.iconUrl)) {
                Image(
                    painter = rememberAsyncImagePainter(app.iconUrl),
                    contentDescription = app.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(shape),
                )
            } else {
                Text(
                    app.name.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            app.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

private fun openUrl(uriHandler: androidx.compose.ui.platform.UriHandler, url: String) {
    try {
        runCatching { uriHandler.openUri(url) }
    } catch (_: Exception) {
    }
}
