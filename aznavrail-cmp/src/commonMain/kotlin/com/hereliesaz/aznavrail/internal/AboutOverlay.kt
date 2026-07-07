package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDocEntry
import com.hereliesaz.aznavrail.model.AzMoreFromApp
import com.hereliesaz.aznavrail.service.GithubDocsRepository
import com.hereliesaz.aznavrail.service.MoreFromAzRepository
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzDivider
import com.hereliesaz.aznavrail.AzLoad
import com.hereliesaz.aznavrail.LocalAzSafeZones
import kotlin.math.abs

/** UI state for the About reader's table-of-contents fetch. */
private sealed interface DocsUi {
    data object Loading : DocsUi
    data class Loaded(val entries: List<AzDocEntry>, val offline: Boolean) : DocsUi
    data object Error : DocsUi
}

/**
 * Full-screen, themed in-app About reader.
 *
 * Layout is two vertically-stacked halves:
 *  - **Top half** — auto-generated table of contents of the app's markdown docs (`.md` files in the
 *    repo root and `docs/`). Selecting a row swaps in the [DocReader] inline.
 *  - **Bottom half** — a **focus-based "More from Az" carousel** with a 5-item size pattern
 *    (small · medium · LARGE · medium · small). The LARGE (center-most) item is the currently active
 *    app; its banner (when present), name, description, and link buttons are rendered below the row.
 */
@Composable
internal fun AboutOverlay(
    repoUrl: String,
    scope: AzNavRailScopeImpl,
    onOpenMoreFromAz: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val accent = scope.activeColor.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.primary
    val surface = scope.translucentBackground.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.surface
    // Safe-zone insets come from the host (rail) or a dropdown-supplied default; rail-offset padding,
    // when present, is applied by the caller's wrapper.
    val safe = LocalAzSafeZones.current
    var selected by remember { mutableStateOf<AzDocEntry?>(null) }

    AzBackHandler(enabled = true) { if (selected != null) selected = null else onDismiss() }

    val docs by produceState<DocsUi>(initialValue = DocsUi.Loading, repoUrl) {
        value = GithubDocsRepository.listDocs(repoUrl).fold(
            onSuccess = { DocsUi.Loaded(it.entries, it.rateLimitedOrOffline) },
            onFailure = { DocsUi.Error },
        )
    }

    // Fetch More-from-Az apps for the bottom-half carousel. We render eagerly so the developer can
    // scroll the carousel while a doc is loading in the top half.
    val moreApps by produceState<List<AzMoreFromApp>?>(
        initialValue = null,
        scope.advancedConfig.moreFromAzEnabled,
        scope.advancedConfig.moreFromAzJsonUrl,
    ) {
        value = if (scope.advancedConfig.moreFromAzEnabled) {
            MoreFromAzRepository.fetch(scope.advancedConfig.moreFromAzJsonUrl)
                .getOrNull()?.apps ?: emptyList()
        } else emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = safe.top, bottom = safe.bottom)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header: back (in reader) / title / close
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (selected != null) {
                    Icon(
                        AzIcons.ArrowBack,
                        contentDescription = "Back to contents",
                        tint = accent,
                        modifier = Modifier.clickable { selected = null }.padding(end = 12.dp)
                    )
                }
                Text(
                    text = selected?.title ?: "About",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    AzIcons.Close,
                    contentDescription = "Close",
                    tint = accent,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
            Spacer(Modifier.height(12.dp))

            if (selected != null) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    DocReader(entry = selected!!, accent = accent)
                }
            } else {
                // TOP HALF — docs TOC. Always occupies the top ~50% of the overlay.
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (val state = docs) {
                        is DocsUi.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { AzLoad() }
                        is DocsUi.Error -> ErrorState(accent) { onDismiss() }
                        is DocsUi.Loaded -> {
                            Column(Modifier.fillMaxSize()) {
                                if (state.offline) {
                                    Text(
                                        "Showing cached docs (offline or rate-limited).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                if (state.entries.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                                        Text(
                                            "No documentation found in this repository.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        items(state.entries, key = { it.path }) { entry ->
                                            TocRow(entry.title, accent) { selected = entry }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // BOTTOM HALF — More-from-Az focused-hero carousel + active-app info panel.
                if (scope.advancedConfig.moreFromAzEnabled) {
                    Spacer(Modifier.height(12.dp))
                    AzDivider(color = accent)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        MoreFromAzHeroCarousel(
                            apps = moreApps,
                            accent = accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocReader(entry: AzDocEntry, accent: Color) {
    val uriHandler = LocalUriHandler.current
    val body by produceState<String?>(initialValue = null, entry.path) {
        value = GithubDocsRepository.fetchDoc(entry).getOrNull() ?: "_Could not load this document._"
    }
    val current = body
    if (current == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { AzLoad() }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            AzMarkdown(markdown = current, activeColor = accent)
        }
    }
}

@Composable
private fun TocRow(title: String, accent: Color, emphasized: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (emphasized) 2.dp else 1.dp,
                color = if (emphasized) accent else accent.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ErrorState(accent: Color, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Couldn't load documentation.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(16.dp))
            AzButton(onClick = onClose, text = "Close", color = accent, activeColor = accent, shape = AzButtonShape.RECTANGLE)
        }
    }
}

// --- More-from-Az focused-hero carousel ---------------------------------------------------------

private val HERO_LARGE = 132.dp
private val HERO_MEDIUM = 96.dp
private val HERO_SMALL = 64.dp
private val HERO_SPACING = 12.dp

/**
 * Horizontal LazyRow with center-snap fling and per-item scaling. Sizes derive from each item's
 * distance from the row's visual center:
 *   0 tiles away  → LARGE (the active app)
 *   1 tile away   → MEDIUM
 *   2+ tiles away → SMALL
 * The active app's banner (when present), name, description, and link buttons render below the row.
 */
@Composable
private fun MoreFromAzHeroCarousel(
    apps: List<AzMoreFromApp>?,
    accent: Color,
) {
    when {
        apps == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { AzLoad() }
        apps.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(
                "No apps to show right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        else -> {
            val listState = rememberLazyListState()
            val fling = rememberSnapFlingBehavior(lazyListState = listState)
            val uriHandler = LocalUriHandler.current

            // Active index = the item whose center is closest to the row's visual center.
            val activeIndex by remember(apps) {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
                    info.visibleItemsInfo.minByOrNull {
                        abs((it.offset + it.size / 2) - viewportCenter)
                    }?.index ?: listState.firstVisibleItemIndex
                }
            }
            val activeApp = apps.getOrNull(activeIndex)

            Column(Modifier.fillMaxSize()) {
                BoxWithConstraints(Modifier.fillMaxWidth().height(HERO_LARGE + 24.dp)) {
                    val density = LocalDensity.current
                    val edgePadding = with(density) { ((maxWidth - HERO_LARGE) / 2).coerceAtLeast(0.dp) }
                    LazyRow(
                        state = listState,
                        flingBehavior = fling,
                        contentPadding = PaddingValues(horizontal = edgePadding),
                        horizontalArrangement = Arrangement.spacedBy(HERO_SPACING),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(apps.size, key = { it }) { index ->
                            val distance = kotlin.math.abs(index - activeIndex)
                            val target: Dp = when (distance) {
                                0 -> HERO_LARGE
                                1 -> HERO_MEDIUM
                                else -> HERO_SMALL
                            }
                            val size by animateDpAsState(target, label = "heroSize")
                            HeroCard(
                                app = apps[index],
                                size = size,
                                accent = accent,
                                active = index == activeIndex,
                                onClick = {
                                    if (index == activeIndex) {
                                        apps[index].primaryUrl?.let { openUrl(uriHandler, it) }
                                    } else {
                                        // Tapping a non-center card would ideally scroll it to center; the
                                        // snapping-fling behavior will pull it in on the next drag.
                                    }
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                activeApp?.let { ActiveAppPanel(it, accent) }
            }
        }
    }
}

@Composable
private fun HeroCard(
    app: AzMoreFromApp,
    size: Dp,
    accent: Color,
    active: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) accent else accent.copy(alpha = 0.4f),
                shape = shape,
            )
            .clickable { onClick() },
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
}

@Composable
private fun ActiveAppPanel(app: AzMoreFromApp, accent: Color) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    ) {
        if (!app.bannerUrl.isNullOrBlank()) {
            Image(
                painter = rememberAsyncImagePainter(app.bannerUrl),
                contentDescription = "${app.name} banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(
            app.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (app.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                app.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            app.playStoreUrl?.let {
                AzButton(
                    onClick = { openUrl(uriHandler, it) },
                    text = "Play",
                    color = accent,
                    activeColor = accent,
                    shape = AzButtonShape.RECTANGLE,
                )
            }
            if (!app.webUrl.isNullOrBlank()) {
                AzButton(
                    onClick = { openUrl(uriHandler, app.webUrl!!) },
                    text = if (app.isPwa) "Open" else "Website",
                    color = accent,
                    activeColor = accent,
                    shape = AzButtonShape.RECTANGLE,
                )
            }
            app.githubUrl?.let {
                AzButton(
                    onClick = { openUrl(uriHandler, it) },
                    text = "GitHub",
                    color = accent,
                    activeColor = accent,
                    shape = AzButtonShape.RECTANGLE,
                )
            }
        }
    }
}

/** The URL a card opens when tapped: prefer the website/PWA, then Play, then the GitHub repo. */
private val AzMoreFromApp.primaryUrl: String?
    get() = webUrl ?: playStoreUrl ?: githubUrl

/** True only for a genuine app icon — never the owner's GitHub avatar. */
private fun isAppIcon(url: String): Boolean =
    url.isNotBlank() && !url.contains("avatars.githubusercontent.com", ignoreCase = true)

private fun openUrl(uriHandler: androidx.compose.ui.platform.UriHandler, url: String) {
    try {
        runCatching { uriHandler.openUri(url) }
    } catch (_: Exception) {
    }
}
