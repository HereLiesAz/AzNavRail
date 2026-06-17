package com.hereliesaz.aznavrail.internal

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDocEntry
import com.hereliesaz.aznavrail.service.GithubDocsRepository
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzDivider
import com.hereliesaz.aznavrail.AzLoad
import com.hereliesaz.aznavrail.LocalAzSafeZones

/** UI state for the About reader's table-of-contents fetch. */
private sealed interface DocsUi {
    data object Loading : DocsUi
    data class Loaded(val entries: List<AzDocEntry>, val offline: Boolean) : DocsUi
    data object Error : DocsUi
}

/**
 * Full-screen, themed in-app About reader. Auto-discovers the consuming app's markdown docs (root +
 * `docs/`) from [repoUrl], lists them as a table of contents, and renders the selected doc inline via
 * [AzMarkdown]. A GitHub repo button sits at the bottom with extra spacing, and an optional
 * "More from Az" entry opens the author's other-apps carousel.
 *
 * Built from the rail's own components ([AzButton], [AzLoad], [AzDivider]) and tokens
 * ([AzNavRailScopeImpl.activeColor], [AzNavRailScopeImpl.translucentBackground]) so it matches the
 * rail's aesthetic. Dismissed via the close button or the system back button.
 */
@Composable
internal fun AboutOverlay(
    repoUrl: String,
    scope: AzNavRailScopeImpl,
    onOpenMoreFromAz: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val accent = scope.activeColor.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.primary
    val surface = scope.translucentBackground.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.surface
    val safe = LocalAzSafeZones.current
    var selected by remember { mutableStateOf<AzDocEntry?>(null) }

    BackHandler(enabled = true) { if (selected != null) selected = null else onDismiss() }

    val docs by produceState<DocsUi>(initialValue = DocsUi.Loading, repoUrl) {
        value = GithubDocsRepository.listDocs(context, repoUrl).fold(
            onSuccess = { DocsUi.Loaded(it.entries, it.rateLimitedOrOffline) },
            onFailure = { DocsUi.Error },
        )
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
                        Icons.AutoMirrored.Filled.ArrowBack,
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
                    Icons.Filled.Close,
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
                when (val state = docs) {
                    is DocsUi.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { AzLoad() }
                    is DocsUi.Error -> ErrorState(accent) { /* retry via recomposition key bump not needed */ onDismiss() }
                    is DocsUi.Loaded -> {
                        if (state.entries.isEmpty()) {
                            Box(Modifier.fillMaxSize().weight(1f), Alignment.Center) {
                                Text(
                                    "No documentation found in this repository.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }
                        } else {
                            if (state.offline) {
                                Text(
                                    "Showing cached docs (offline or rate-limited).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(state.entries, key = { it.path }) { entry ->
                                    TocRow(entry.title, accent) { selected = entry }
                                }
                                if (onOpenMoreFromAz != null) {
                                    item {
                                        TocRow("More from Az", accent, emphasized = true) { onOpenMoreFromAz() }
                                    }
                                }
                            }
                        }
                        // GitHub link pinned at the bottom with extra separation.
                        Spacer(Modifier.height(32.dp))
                        AzDivider()
                        Spacer(Modifier.height(16.dp))
                        AzButton(
                            onClick = { openUrl(context, repoUrl) },
                            text = "View on GitHub",
                            color = accent,
                            activeColor = accent,
                            shape = AzButtonShape.RECTANGLE,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocReader(entry: AzDocEntry, accent: Color) {
    val context = LocalContext.current
    val body by produceState<String?>(initialValue = null, entry.path) {
        value = GithubDocsRepository.fetchDoc(context, entry).getOrNull() ?: "_Could not load this document._"
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

private fun openUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}
