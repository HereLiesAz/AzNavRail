package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzUnattachedAnchor

/**
 * Declares an unattached host with a developer-defined maximum visible height.
 *
 * This is an overload of [AzNavRailScope.azUnattachedHostItem] that adds [maxHeight] without
 * changing the existing source contract. The rail grows naturally until either its content is
 * exhausted or [maxHeight] is reached. When its host, sub-items, or nested sub-hosts need more room,
 * the unattached rail becomes vertically scrollable automatically. The same cap follows the rail
 * while it is free-floating, screen-edge docked, or docked to another floating rail.
 *
 * [maxHeight] must be a positive, specified [Dp]. The library still applies the current safe
 * viewport as an upper bound, so a requested height can never force the rail off-screen.
 */
fun AzNavRailScope.azUnattachedHostItem(
    id: String,
    text: String,
    maxHeight: Dp,
    anchor: AzUnattachedAnchor = AzUnattachedAnchor.OPPOSITE,
    route: String? = null,
    content: Any? = null,
    color: Color? = null,
    shape: AzButtonShape? = null,
    disabled: Boolean = false,
    screenTitle: String? = null,
    info: String? = null,
    classifiers: Set<String> = emptySet(),
    menuText: String? = null,
    textColor: Color? = null,
    fillColor: Color? = null,
    translucentBackgroundColor: Color? = null,
    badge: String? = null,
    persistentBadge: Boolean = false,
    isLoading: Boolean = false,
    initiallyExpanded: Boolean = false,
    expandWhen: (() -> Boolean)? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    forceHiddenMenuOpen: Boolean = false,
    onHiddenMenuDismiss: (() -> Unit)? = null,
    hiddenMenu: HiddenMenuScope.() -> Unit = {},
) {
    require(maxHeight.isSpecified && maxHeight > 0.dp) {
        "maxHeight must be a positive, specified Dp"
    }

    azUnattachedHostItem(
        id = id,
        text = text,
        anchor = anchor,
        route = route,
        content = content,
        color = color,
        shape = shape,
        disabled = disabled,
        screenTitle = screenTitle,
        info = info,
        classifiers = classifiers,
        menuText = menuText,
        textColor = textColor,
        fillColor = fillColor,
        translucentBackgroundColor = translucentBackgroundColor,
        badge = badge,
        persistentBadge = persistentBadge,
        isLoading = isLoading,
        initiallyExpanded = initiallyExpanded,
        expandWhen = expandWhen,
        onExpandedChange = onExpandedChange,
        onClick = onClick,
        forceHiddenMenuOpen = forceHiddenMenuOpen,
        onHiddenMenuDismiss = onHiddenMenuDismiss,
        hiddenMenu = hiddenMenu,
    )

    val implementation = this as? AzNavRailScopeImpl
        ?: error("AzNavRailScope implementation does not support unattached max-height state")
    val index = implementation.navItems.indexOfLast { it.id == id }
    check(index >= 0) { "Unattached host '$id' was not registered" }
    implementation.navItems[index] = implementation.navItems[index].copy(
        unattachedMaxHeightDp = maxHeight.value,
    )
}