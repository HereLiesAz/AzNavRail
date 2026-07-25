package com.hereliesaz.aznavrail.model

/**
 * What the user taps to open an [com.hereliesaz.aznavrail.AzDropdownMenu].
 *
 * In AzNavRail tradition the trigger is not a free composable slot — it is one of the sanctioned
 * shapes below. The default is [MoreVert] (the three vertical dots), so a drop-down declared with no
 * configuration reads as a menu affordance rather than as the app's launcher icon.
 */
sealed interface AzDropdownTrigger {
    /** The Material "more" glyph — three vertical dots. The default. */
    data object MoreVert : AzDropdownTrigger

    /** The hamburger glyph (three stacked bars). */
    data object Hamburger : AzDropdownTrigger

    /**
     * The host app's launcher icon, drawn exactly like the rail's header. This was the only trigger
     * before triggers were configurable; pass it to keep that look.
     */
    data object AppIcon : AzDropdownTrigger

    /** A word (or two) that hosts the menu, drawn in the screen title's accent. */
    data class Text(val text: String) : AzDropdownTrigger

    /**
     * A developer-supplied icon. [model] accepts the same values a rail item's `content` does — a
     * Compose `ImageVector` or `Painter`, an image URL, or any other Coil-compatible model.
     */
    data class Icon(val model: Any) : AzDropdownTrigger
}

/** Where an [com.hereliesaz.aznavrail.AzDropdownMenu]'s trigger is drawn. */
enum class AzDropdownTriggerPlacement {
    /**
     * [TITLE] when the drop-down is declared inside an `AzHostActivityLayout` (the usual case — a
     * drop-down declared in an `onscreen { … }` block), [INLINE] otherwise. The default.
     */
    AUTO,

    /**
     * The trigger is lifted out of the call site and drawn next to the big screen title, above the
     * onscreen content area. Several drop-downs hosted this way line their triggers up beside each
     * other in declaration order.
     */
    TITLE,

    /** The trigger is drawn where the composable is called, like any other widget. */
    INLINE,
}
