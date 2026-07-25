package com.hereliesaz.aznavrail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.internal.color
import com.hereliesaz.aznavrail.model.AzItemAlert
import com.hereliesaz.aznavrail.model.AzItemState
import com.hereliesaz.aznavrail.model.AzNavItem

/** How loud an [AzPopup] is, and what it does to the rail item that raised it. */
enum class AzPopupKind {
    /** Ordinary information. The source item is left looking like itself. */
    INFO,

    /** A notice. The source item becomes a yellow rounded-corner triangle outline while it is up. */
    NOTICE,

    /** A warning. Same triangle, in a louder yellow. */
    WARNING,
}

/** One live show request, describing what the popup is about and which item raised it. */
data class AzPopupRequest(
    /**
     * The id of the rail item that raised it. Null asks the rail for the **last touched** item,
     * which is what a warning raised from a background job — with no tap to name a source — binds to.
     */
    val itemId: String? = null,
    val kind: AzPopupKind = AzPopupKind.INFO,
    val title: String? = null,
    val message: String? = null,
    /** Anything else the popup body needs. Passed straight through, untouched. */
    val payload: Any? = null,
)

/**
 * The handle an [AzPopup] gets on the rail item that raised it — the shared channel between the two.
 *
 * The popup can read the item as the rail currently has it, and write back to it: a long-running
 * action started from a rail item can spin that item's own loading animation, drop a badge on it
 * when it finishes, or flag it. Writes are held on the rail scope and survive the DSL re-running, so
 * they last until the popup clears them (which it does automatically on dismiss).
 */
@Stable
interface AzPopupItemHandle {
    /** The bound item's id. */
    val id: String

    /** The bound item as the rail currently renders it, or null if it is no longer declared. */
    val item: AzNavItem?

    /** Sets (or, with a null/blank [text], clears) the item's badge. */
    fun setBadge(text: String?, persistent: Boolean = false)

    /** Starts or stops the item's own loading animation. */
    fun setLoading(loading: Boolean)

    /** Flags the item, or clears the flag with null. A flagged item draws as the warning triangle. */
    fun setAlert(alert: AzItemAlert?)

    /** Drops everything this popup pushed onto the item, restoring what the DSL declared. */
    fun clear()
}

/** What an [AzPopup]'s body is given: the request that opened it, its item, and a way out. */
@Stable
interface AzPopupScope {
    val kind: AzPopupKind
    val title: String?
    val message: String?
    val payload: Any?

    /** The rail item that raised this popup, or null when it was raised without one. */
    val item: AzPopupItemHandle?

    /** Closes the popup (and reverts anything it did to [item] that it did not make permanent). */
    fun dismiss()
}

/**
 * Opens and closes an [AzPopup], and names the rail item it belongs to.
 *
 * Create one with [rememberAzPopupController], register a popup for it with `azPopup(controller)`
 * in the host DSL, and raise it from anywhere — an item's `onClick`, a coroutine, a callback:
 *
 * ```
 * val alerts = rememberAzPopupController()
 * AzHostActivityLayout(navController = navController) {
 *     azRailItem(id = "sync", text = "Sync") { alerts.show(itemId = "sync", title = "Syncing…") }
 *     azPopup(alerts) {
 *         Text(message ?: "")
 *         AzButton(onClick = { item?.setLoading(false); dismiss() }, text = "Stop")
 *     }
 * }
 * ```
 */
@Stable
class AzPopupController {
    /** The live request, or null when nothing is showing. */
    var request: AzPopupRequest? by mutableStateOf(null)
        private set

    val isVisible: Boolean get() = request != null

    /**
     * Raises the popup.
     *
     * @param itemId The rail item this popup belongs to. Leave null to bind to the **last touched**
     *   rail item, which is what makes an unattributed warning land on whatever the user just did.
     */
    fun show(
        itemId: String? = null,
        kind: AzPopupKind = AzPopupKind.INFO,
        title: String? = null,
        message: String? = null,
        payload: Any? = null,
    ) {
        request = AzPopupRequest(itemId, kind, title, message, payload)
    }

    /** Closes the popup. */
    fun dismiss() {
        request = null
    }
}

/** Creates the [AzPopupController] for one popup, surviving recomposition. */
@Composable
fun rememberAzPopupController(): AzPopupController = remember { AzPopupController() }

/** Holds a popup registered via `AzNavHostScope.azPopup`. */
internal data class AzPopupItem(
    val controller: AzPopupController,
    val dismissOnOutsideTap: Boolean,
    val content: @Composable AzPopupScope.() -> Unit,
)

/** Writes an [AzPopup]'s changes into the rail scope's override map, keyed by item id. */
internal class AzPopupItemHandleImpl(
    override val id: String,
    private val scope: AzNavRailScopeImpl,
) : AzPopupItemHandle {

    override val item: AzNavItem? get() = scope.navItems.firstOrNull { it.id == id }

    private fun update(transform: (AzItemState) -> AzItemState) {
        val current = scope.itemOverrides[id] ?: AzItemState()
        scope.itemOverrides[id] = transform(current)
    }

    override fun setBadge(text: String?, persistent: Boolean) =
        update { it.copy(badge = text, persistentBadge = persistent) }

    override fun setLoading(loading: Boolean) = update { it.copy(isLoading = loading) }

    override fun setAlert(alert: AzItemAlert?) = update { it.copy(alert = alert) }

    override fun clear() {
        scope.itemOverrides.remove(id)
    }
}

/** The [AzPopupScope] handed to a popup's body. */
internal class AzPopupScopeImpl(
    private val request: AzPopupRequest,
    override val item: AzPopupItemHandle?,
    private val onDismiss: () -> Unit,
) : AzPopupScope {
    override val kind: AzPopupKind get() = request.kind
    override val title: String? get() = request.title
    override val message: String? get() = request.message
    override val payload: Any? get() = request.payload
    override fun dismiss() = onDismiss()
}

/**
 * The built-in popup body: the request's title and message in the rail's own type, with a single
 * "OK" that closes it. It is what `azPopup(controller)` renders when you don't supply a body.
 */
@Composable
fun AzPopupScope.AzPopupBody() {
    val accent = when (kind) {
        AzPopupKind.INFO -> MaterialTheme.colorScheme.primary
        AzPopupKind.NOTICE -> AzItemAlert.NOTICE.color()
        AzPopupKind.WARNING -> AzItemAlert.WARNING.color()
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        title?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = accent,
                textAlign = TextAlign.Center,
            )
        }
        message?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        AzButton(onClick = { dismiss() }, text = "OK", color = accent)
    }
}

/**
 * Renders one registered popup as a centred panel over a scrim.
 *
 * While a [AzPopupKind.NOTICE] / [AzPopupKind.WARNING] popup is up, the bound rail item is flagged
 * — it redraws as a yellow rounded-corner triangle outline — and the flag is lifted again when the
 * popup closes, so the item never keeps a warning it no longer owns.
 */
@Composable
internal fun AzPopupHost(
    popup: AzPopupItem,
    railScope: AzNavRailScopeImpl,
    modifier: Modifier = Modifier,
) {
    val request = popup.controller.request ?: return

    // Resolve the bound item once per request: the explicit id if there is one, else whatever the
    // user last touched.
    val boundId = request.itemId ?: railScope.lastTouchedItemId
    val handle = remember(boundId, railScope) {
        boundId?.let { AzPopupItemHandleImpl(it, railScope) }
    }

    // The flag is owned by the popup's lifetime, not by the developer: raised with the request and
    // dropped on dismissal (or when the popup leaves composition mid-flight).
    DisposableEffect(request, handle) {
        val alert = when (request.kind) {
            AzPopupKind.INFO -> null
            AzPopupKind.NOTICE -> AzItemAlert.NOTICE
            AzPopupKind.WARNING -> AzItemAlert.WARNING
        }
        if (alert != null) handle?.setAlert(alert)
        onDispose { if (alert != null) handle?.setAlert(null) }
    }

    val dismiss = { popup.controller.dismiss() }
    val scope = remember(request, handle) { AzPopupScopeImpl(request, handle, dismiss) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(popup.dismissOnOutsideTap) {
                detectTapGestures {
                    if (popup.dismissOnOutsideTap) dismiss()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                // Swallow taps on the panel itself so they don't reach the dismissing scrim.
                .pointerInput(Unit) { detectTapGestures { } },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
                width = 2.dp,
                color = when (request.kind) {
                    AzPopupKind.INFO -> railScope.activeColor.takeOrElse { MaterialTheme.colorScheme.primary }
                    AzPopupKind.NOTICE -> AzItemAlert.NOTICE.color()
                    AzPopupKind.WARNING -> AzItemAlert.WARNING.color()
                },
            ),
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                popup.content(scope)
            }
        }
    }
}
