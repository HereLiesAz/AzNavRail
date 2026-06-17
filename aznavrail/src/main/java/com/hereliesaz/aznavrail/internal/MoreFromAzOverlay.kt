package com.hereliesaz.aznavrail.internal

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzDivider
import com.hereliesaz.aznavrail.AzLoad
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzMoreFromApp
import com.hereliesaz.aznavrail.service.MoreFromAzRepository
import com.hereliesaz.aznavrail.LocalAzSafeZones

/**
 * Full-screen "More from Az" overlay: a Material 3 expressive carousel of the library author's other
 * apps with a detail pane below. The focused/selected card drives the pane (name, description, and
 * GitHub/Play [AzButton]s). Data comes from the CI-versioned `more-from-az.json` via
 * [MoreFromAzRepository]; the card visuals reuse the rail's transparent-shape-with-colored-stroke
 * language and Coil for icon fill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoreFromAzOverlay(
    jsonUrl: String,
    scope: AzNavRailScopeImpl,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val accent = scope.activeColor.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.primary
    val surface = scope.translucentBackground.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.surface
    val safe = LocalAzSafeZones.current

    BackHandler(enabled = true) { onDismiss() }

    val apps by produceState<List<AzMoreFromApp>?>(initialValue = null, jsonUrl) {
        value = MoreFromAzRepository.fetch(context, jsonUrl).getOrNull()?.apps ?: emptyList()
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
                    var selected by remember(list) { mutableIntStateOf(0) }
                    val carouselState = rememberCarouselState { list.size }
                    HorizontalMultiBrowseCarousel(
                        state = carouselState,
                        preferredItemWidth = 132.dp,
                        itemSpacing = 12.dp,
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                    ) { index ->
                        AppCard(
                            app = list[index],
                            accent = accent,
                            focused = index == selected,
                            onClick = { selected = index },
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    AzDivider()
                    Spacer(Modifier.height(16.dp))

                    val app = list[selected.coerceIn(0, list.size - 1)]
                    DetailPane(app = app, accent = accent, openUrl = { openUrl(context, it) })
                }
            }
        }
    }
}

@Composable
private fun AppCard(app: AzMoreFromApp, accent: Color, focused: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .border(width = if (focused) 3.dp else 1.dp, color = if (focused) accent else accent.copy(alpha = 0.4f), shape = shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (app.iconUrl.isNotBlank()) {
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
private fun DetailPane(app: AzMoreFromApp, accent: Color, openUrl: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(app.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (app.description.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(app.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            app.webUrl?.let { url ->
                AzButton(onClick = { openUrl(url) }, text = if (app.isPwa) "Open" else "Website", color = accent, activeColor = accent, shape = AzButtonShape.RECTANGLE)
            }
            app.playStoreUrl?.let { url ->
                AzButton(onClick = { openUrl(url) }, text = "Play Store", color = accent, activeColor = accent, shape = AzButtonShape.RECTANGLE)
            }
            app.githubUrl?.let { url ->
                AzButton(onClick = { openUrl(url) }, text = "GitHub", color = accent, activeColor = accent, shape = AzButtonShape.RECTANGLE)
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}
