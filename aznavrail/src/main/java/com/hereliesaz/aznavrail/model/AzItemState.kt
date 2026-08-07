package com.hereliesaz.aznavrail.model

/**
 * Per-item state layered on top of an already-declared item — the badge it carries, whether it is
 * spinning its own loading animation, and whether a popup has flagged it.
 *
 * Every field is nullable and means "leave whatever the item already had". That is what lets
 * `azItemState(id = "sync", isLoading = true)` set the spinner without also clearing the badge, and
 * what lets an [com.hereliesaz.aznavrail.AzPopupController]'s override sit on top of a declared
 * state without erasing the parts it doesn't care about.
 */
data class AzItemState(
    val badge: String? = null,
    val persistentBadge: Boolean? = null,
    val isLoading: Boolean? = null,
    val alert: AzItemAlert? = null,
    /**
     * Whether the item's **secondary** highlight is lit — the third highlight, the one the library
     * never sets on its own. See [AzHighlight].
     */
    val secondary: Boolean? = null,
    /**
     * Whether the item's **tertiary** highlight is lit — the fourth highlight, ranked below
     * secondary. The library never sets this on its own. See [AzHighlight].
     */
    val tertiary: Boolean? = null,
)
