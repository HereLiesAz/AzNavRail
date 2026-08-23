package com.hereliesaz.aznavrail.internal

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.hereliesaz.aznavrail.R
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzAuthorProfile
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDocEntry
import com.hereliesaz.aznavrail.model.AzMoreFromApp
import com.hereliesaz.aznavrail.service.AzAboutPrefetch
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzDivider
import com.hereliesaz.aznavrail.AzLoad
import com.hereliesaz.aznavrail.LocalAzSafeZones
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.hereliesaz.aznavrail.model.AzMotion
import kotlinx.coroutines.launch
import kotlin.math.abs

/** UI state for the About reader's table-of-contents fetch. */
private sealed interface DocsUi {
    data object Loading : DocsUi
    data class Loaded(val entries: List<AzDocEntry>, val offline: Boolean) : DocsUi
    data object Error : DocsUi
}

/**
 * The About reader's own palette: dark ground, light ink, in every theme.
 *
 * This is one of the few places in the library that does not follow the host's colour scheme. The
 * reader is a full-screen surface the user has stepped *aside* into — long-form prose, a document
 * list, a carousel — and long-form reading on a bright white field is the wrong call regardless of
 * what the surrounding app is doing. It used to take `MaterialTheme.colorScheme.surface`, which in
 * a light-themed host meant a full screen of white.
 *
 * The host's accent still comes through: headings, links, and the close affordance all use it.
 */
private object AzAboutColors {
    /** Near-black, very slightly cool, so the accent reads warm against it. */
    val Ground = Color(0xFF101014)

    /** Primary ink. Not pure white — pure white on near-black glares. */
    val Ink = Color(0xFFECECF0)

    /** Secondary ink for supporting lines. */
    val InkMuted = Color(0xFFB4B4BE)

    /** The grab handle and hairline rules. */
    val Hairline = Color(0xFF3A3A44)
}

/**
 * Full-screen, themed in-app About reader.
 *
 * Layout is vertically stacked, each section sharing the screen equally with [Modifier.weight]:
 *  - **Docs** — auto-generated table of contents of the app's markdown docs (`.md` files in the repo
 *    root and `docs/`). Selecting a row swaps in the [DocReader] inline.
 *  - **More from Az** (when enabled) — a **focus-based carousel** with a 5-item size pattern
 *    (small · medium · LARGE · medium · small). The LARGE (center-most) item is the currently active
 *    app; its banner (when present), name, description, and link buttons are rendered below the row.
 *  - **Tip jar, author, and contact** — the free-forever pitch and tip button ([AzTipJar]), the
 *    author's GitHub avatar/name/bio ([AzAuthorHeader]), and the @HereLiesAz/Feedback/website links
 *    ([AzAboutPageFooter]). This section scrolls independently so it always has room for all three.
 */
@Composable
internal fun AboutOverlay(
    repoUrl: String,
    scope: AzNavRailScopeImpl,
    onOpenMoreFromAz: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // The reader wears the rail's colour, not the app theme's. `activeColor` when the developer set
    // one; failing that, the colour the rail's own items are drawn in. A theme-primary fallback is
    // how the reader ended up as near-invisible grey-on-black beside a white rail.
    val accent = scope.railAccent.takeOrElse { MaterialTheme.colorScheme.primary }
    // A host-supplied translucent background still supplies the hue; its alpha is not honoured,
    // because a see-through full-screen reader is an unreadable one.
    val surface = scope.translucentBackground
        .takeIf { it != Color.Unspecified }
        ?.copy(alpha = 1f)
        ?: AzAboutColors.Ground
    // Safe-zone insets come from the host (rail) or a dropdown-supplied default; rail-offset padding,
    // when present, is applied by the caller's wrapper.
    val safe = LocalAzSafeZones.current
    var selected by remember { mutableStateOf<AzDocEntry?>(null) }

    BackHandler(enabled = true) { if (selected != null) selected = null else onDismiss() }

    // The reader reads the content the rail warmed in the background (see [AzAboutPrefetch]) rather
    // than starting its own fetch and making the user watch it. `warmedDocs` is snapshot state, so a
    // page opened while the warm-up is still in flight fills itself in the moment it lands.
    val warmedDocs = AzAboutPrefetch.docsFor(repoUrl)
    var docsFailed by remember(repoUrl) { mutableStateOf(false) }
    LaunchedEffect(repoUrl) {
        // A no-op when the warm-up already ran; the fallback path when it didn't (or failed).
        runCatching { AzAboutPrefetch.warmDocs(context, repoUrl) }
        docsFailed = AzAboutPrefetch.docsFor(repoUrl) == null
    }
    val docs: DocsUi = when {
        warmedDocs != null -> DocsUi.Loaded(warmedDocs.entries, warmedDocs.rateLimitedOrOffline)
        docsFailed -> DocsUi.Error
        else -> DocsUi.Loading
    }

    // Same story for the bottom-half carousel: read what was warmed, and only fetch here if nothing
    // was. Null still means "not here yet" and draws the spinner.
    val moreFromAzEnabled = scope.advancedConfig.moreFromAzEnabled
    val moreFromAzUrl = scope.advancedConfig.moreFromAzJsonUrl
    val moreApps: List<AzMoreFromApp>? =
        if (!moreFromAzEnabled) emptyList() else AzAboutPrefetch.moreAppsFor(moreFromAzUrl)
    LaunchedEffect(moreFromAzEnabled, moreFromAzUrl) {
        if (moreFromAzEnabled) runCatching { AzAboutPrefetch.warmMoreFromAz(context, moreFromAzUrl) }
    }

    // Drag-down-to-dismiss. A full-screen reader that can only be left through one 24dp icon is a
    // room with a keyhole for a door; this gives the whole surface a way out. The sheet follows the
    // finger, springs back if the pull was not committed, and leaves if it was.
    val dismissDrag = remember { Animatable(0f) }
    val dismissScope = rememberCoroutineScope()
    val dismissThresholdPx = with(LocalDensity.current) { 140.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dismissDrag.value
                // Fades as it goes, so a half-committed pull reads as "on its way out" rather than
                // as the screen having simply slipped.
                alpha = 1f - (dismissDrag.value / (dismissThresholdPx * 3f)).coerceIn(0f, 0.4f)
            }
            .background(surface)
            .pointerInput(selected) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dismissDrag.value > dismissThresholdPx) onDismiss()
                        else dismissScope.launch { dismissDrag.animateTo(0f, tween(AzMotion.PanelDurationMs)) }
                    },
                    onDragCancel = {
                        dismissScope.launch { dismissDrag.animateTo(0f, tween(AzMotion.PanelDurationMs)) }
                    },
                ) { _, dragAmount ->
                    // Downward only, and never above the resting position.
                    dismissScope.launch {
                        dismissDrag.snapTo((dismissDrag.value + dragAmount).coerceAtLeast(0f))
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = safe.top, bottom = safe.bottom, start = safe.start, end = safe.end)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // The grab handle — the visible half of the drag-to-dismiss gesture. Without it the
            // gesture exists but nothing announces it.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AzAboutColors.Hairline)
            )

            // Header: back (in reader) / title / close
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (selected != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to contents",
                        tint = accent,
                        // A 48dp target, not a bare 24dp glyph — the minimum a finger can be asked
                        // to find.
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { selected = null }
                            .size(48.dp)
                            .padding(12.dp)
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
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onDismiss() }
                        .size(48.dp)
                        .padding(12.dp)
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
                                        color = AzAboutColors.InkMuted.copy(alpha = 0.6f),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                if (state.entries.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                                        Text(
                                            "No documentation found in this repository.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = AzAboutColors.InkMuted.copy(alpha = 0.7f),
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

                // MIDDLE — More-from-Az focused-hero carousel + active-app info panel ("the app links").
                if (moreFromAzEnabled) {
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

                // BOTTOM — the tip pitch, the author, and the way to reach them. Scrolls on its own
                // so it always has room for all three regardless of how much the panes above claim;
                // the page ends where every other surface in this library ends: a way to write to the
                // author, and the author. The About page is where someone goes to find out who made
                // this and how to complain about it — making them hunt through a menu for that would
                // be a joke at their expense.
                Spacer(Modifier.height(12.dp))
                AzDivider(color = accent)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(16.dp))
                        AzTipJar(accent = accent)
                        Spacer(Modifier.height(28.dp))
                        AzAuthorHeader(accent = accent)
                        AzAboutPageFooter(accent = accent)
                    }
                }
            }
        }
    }
}

/**
 * The free-forever pitch and its tip button — sits between the app carousel and the author header,
 * where "here's more of my work" naturally turns into "here's how you can thank me for it".
 */
@Composable
private fun AzTipJar(accent: Color) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Every single one of my apps is available on Github in full, without ads or " +
                "conditions of any kind, for free and forever. But I never say no to just a the-tip.",
            style = MaterialTheme.typography.bodyLarge,
            color = AzAboutColors.InkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        AzButton(
            onClick = { openUrl(context, AZ_DONATE_URL) },
            text = "Leave a Tip",
            color = accent,
            activeColor = accent,
            shape = AzButtonShape.RECTANGLE,
        )
    }
}

/**
 * The author header: GitHub avatar, name, and bio. The avatar and bio are fetched live from the
 * GitHub users API (see [AzAboutPrefetch.warmAuthorProfile]) rather than baked in, so this stays
 * current with no release needed; the name is fixed — it is always "Az" regardless of what GitHub's
 * `name` field says.
 */
@Composable
private fun AzAuthorHeader(accent: Color) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { runCatching { AzAboutPrefetch.warmAuthorProfile(context) } }
    val profile: AzAuthorProfile? = AzAboutPrefetch.authorProfile

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .border(2.dp, accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val avatarUrl = profile?.avatarUrl
            if (!avatarUrl.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(avatarUrl),
                    contentDescription = "Az",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text("AZ", style = MaterialTheme.typography.headlineMedium, color = accent)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Az",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AzAboutColors.Ink,
        )
        val bio = profile?.bio
        if (!bio.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyLarge,
                color = AzAboutColors.InkMuted,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(14.dp))
        // The brush-stroke "A" mark — the same signature used across hereliesaz.github.io and
        // its own /admin PWA — as a quiet watermark under the author identity, not a competing
        // logo lockup.
        Image(
            painter = painterResource(R.drawable.ic_az_signature),
            contentDescription = null,
            colorFilter = ColorFilter.tint(AzAboutColors.InkMuted),
            modifier = Modifier.height(28.dp),
        )
    }
}

/**
 * The About page's own footer: @HereLiesAz, Feedback, and hereliesaz.com, stacked so each gets the
 * full width to be set in large type — the reader has already stepped aside into a full page for
 * this; three cramped columns of small text would undersell the invitation.
 */
@Composable
private fun AzAboutPageFooter(accent: Color) {
    val context = LocalContext.current
    val appName = remember(context) {
        runCatching {
            context.applicationInfo.loadLabel(context.packageManager).toString()
        }.getOrDefault(context.packageName)
    }
    Spacer(Modifier.height(20.dp))
    AzDivider(color = accent)
    Spacer(Modifier.height(20.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        AzAboutFooterLink(
            text = "@HereLiesAz",
            color = accent,
            onClick = { openUrl(context, AZ_INSTAGRAM_URL) },
        )
        AzAboutFooterLink(
            text = "Feedback",
            color = accent,
            onClick = {
                try {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:hereliesaz@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, appName)
                    }
                    context.startActivity(Intent.createChooser(emailIntent, "Send feedback"))
                } catch (_: Exception) {
                }
            },
        )
        AzAboutFooterLink(
            text = "hereliesaz.com",
            color = accent,
            onClick = { openUrl(context, AZ_SITE_URL) },
        )
    }
}

/** One big, centered link row in the About page's own footer — see [AzAboutPageFooter]. */
@Composable
private fun AzAboutFooterLink(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.clickable { onClick() }.padding(vertical = 4.dp),
    )
}

@Composable
private fun DocReader(entry: AzDocEntry, accent: Color) {
    val context = LocalContext.current
    // The warmed body when there is one (the first doc always is), so the common case renders on the
    // frame the row is tapped.
    val body by produceState<String?>(initialValue = AzAboutPrefetch.bodyFor(entry), entry.path) {
        if (value != null) return@produceState
        runCatching { AzAboutPrefetch.warmDoc(context, entry) }
        value = AzAboutPrefetch.bodyFor(entry) ?: "_Could not load this document._"
    }
    val current = body
    if (current == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { AzLoad() }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            AzMarkdown(markdown = current, activeColor = accent, ink = AzAboutColors.Ink)
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
                color = AzAboutColors.InkMuted.copy(alpha = 0.8f),
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

/** How far off-centre a settled card may sit before the row pulls it in. */
private const val SETTLE_TOLERANCE_PX = 2

/**
 * Fraction of a fling's velocity the carousel actually acts on.
 *
 * The carousel is a focus list, not a scroller: one flick should hand focus to the next app or two,
 * and the card should arrive with some weight rather than gliding. Damping the incoming velocity
 * before the snap behaviour sees it is what produces both — a shorter search for a landing card, and
 * a shorter, firmer settle onto it.
 */
private const val FLING_DAMPING = 0.22f

/** Wraps [delegate] so it only ever sees [FLING_DAMPING] of the velocity the user actually threw. */
private class AzStickyFlingBehavior(
    private val delegate: FlingBehavior,
    private val damping: Float,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float =
        with(delegate) { performFling(initialVelocity * damping) }
}

@Composable
private fun rememberAzStickyFling(delegate: FlingBehavior): FlingBehavior =
    remember(delegate) { AzStickyFlingBehavior(delegate, FLING_DAMPING) }

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
                color = AzAboutColors.InkMuted.copy(alpha = 0.6f),
            )
        }
        else -> {
            val listState = rememberLazyListState()
            // A flick should move the carousel a card or two, not spin it. The snap behaviour picks
            // the landing card; damping the velocity on the way in decides how far it is allowed to
            // look for one. Undamped, this row coasted past a dozen apps on one flick and the "focus"
            // the design is built around never had a chance to land on anything.
            val fling = rememberAzStickyFling(rememberSnapFlingBehavior(lazyListState = listState))
            val carouselScope = rememberCoroutineScope()
            val context = LocalContext.current

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

            // Settle. When the gesture ends anywhere between two cards, the nearest one is pulled
            // fully into the centre instead of being left hanging half off the edge — the row always
            // comes to rest ON an app, never between two.
            LaunchedEffect(listState, apps.size) {
                snapshotFlow { listState.isScrollInProgress }
                    .collect { scrolling ->
                        if (scrolling) return@collect
                        val info = listState.layoutInfo
                        val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
                        val nearest = info.visibleItemsInfo.minByOrNull {
                            abs((it.offset + it.size / 2) - viewportCenter)
                        } ?: return@collect
                        val drift = abs((nearest.offset + nearest.size / 2) - viewportCenter)
                        // Already centred (within a hair) — leave it alone, or this re-triggers itself.
                        if (drift <= SETTLE_TOLERANCE_PX) return@collect
                        runCatching { listState.animateScrollToItem(nearest.index) }
                    }
            }

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
                                        apps[index].primaryUrl?.let { openUrl(context, it) }
                                    } else {
                                        // Tapping a card off-centre brings it to the centre. It was
                                        // already the obvious meaning of the tap; it just used to do
                                        // nothing and wait for a drag.
                                        carouselScope.launch { listState.animateScrollToItem(index) }
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
    val context = LocalContext.current
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
            color = AzAboutColors.Ink,
        )
        if (app.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                app.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AzAboutColors.InkMuted.copy(alpha = 0.75f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            app.playStoreUrl?.let {
                AzButton(
                    onClick = { openUrl(context, it) },
                    text = "Play",
                    color = accent,
                    activeColor = accent,
                    shape = AzButtonShape.RECTANGLE,
                )
            }
            if (!app.webUrl.isNullOrBlank()) {
                AzButton(
                    onClick = { openUrl(context, app.webUrl!!) },
                    text = if (app.isPwa) "Open" else "Website",
                    color = accent,
                    activeColor = accent,
                    shape = AzButtonShape.RECTANGLE,
                )
            }
            app.githubUrl?.let {
                AzButton(
                    onClick = { openUrl(context, it) },
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

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}
