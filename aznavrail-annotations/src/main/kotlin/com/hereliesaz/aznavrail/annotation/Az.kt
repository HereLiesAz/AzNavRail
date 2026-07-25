package com.hereliesaz.aznavrail.annotation

/**
 * Declares part of an AzNavRail navigation graph.
 *
 * Put it on the Activity to configure the rail, and on that Activity's **functions and properties**
 * to declare items. The KSP processor reads them and generates a `<ActivityName>AzGraph` object
 * implementing `com.hereliesaz.aznavrail.AzGraphInterface`, which `AzActivity` runs for you.
 *
 * ```
 * @Az(app = App(displayAppName = true, startDestination = "home"))
 * class MainActivity : AzActivity() {
 *     override val graph = MainActivityAzGraph
 *
 *     var unread by mutableIntStateOf(0)
 *
 *     @Az(rail = RailItem(id = "home", text = "Home", route = "home", badgeProperty = "unread"))
 *     fun goHome() { … }
 * }
 * ```
 *
 * Each nested member is "declared" when its `id` is non-blank (or, for [Divider], when `enabled` is
 * true), so one `@Az` carries exactly the one kind of thing you filled in.
 *
 * The DSL remains the primary and complete API; this is sugar over it. Anything the annotations
 * can't express, write in `AzActivity.configureRail()`, which the generated graph calls inside the
 * same DSL block.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Az(
    val app: App = App(),
    val advanced: Advanced = Advanced(),
    val rail: RailItem = RailItem(),
    val menu: MenuItem = MenuItem(),
    val host: RailHost = RailHost(),
    val sub: SubItem = SubItem(),
    val toggle: Toggle = Toggle(),
    val cycler: Cycler = Cycler(),
    val divider: Divider = Divider(),
)

/**
 * Class-level rail configuration. Maps onto `azConfig`/`azTheme`.
 *
 * @param startDestination Route the generated `AzNavHost` starts at. Blank means no `AzNavHost` is
 *   generated — declare your own onscreen content in `configureRail()`.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class App(
    val displayAppName: Boolean = false,
    val packRailButtons: Boolean = false,
    val vibrate: Boolean = false,
    val dockingSide: Side = Side.LEFT,
    val startDestination: String = "",
)

/** Which edge the rail docks to. Mirrors `AzDockingSide`. */
enum class Side { LEFT, RIGHT }

/**
 * Class-level advanced configuration. Maps onto `azAdvanced`.
 *
 * @param isLoadingProperty Name of a `Boolean` property on the Activity bound to the global loading
 *   overlay. Prefer a per-item `badgeProperty`/`loadingProperty` — see `azItemState`.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class Advanced(
    val isLoadingProperty: String = "",
    val helpEnabled: Boolean = false,
    val enableRailDragging: Boolean = false,
    val autoGuidanceEdges: Boolean = false,
)

/**
 * An item on the always-visible rail. On a **function**, the function is its `onClick`; on a
 * **property**, the property's value supplies the item's text.
 *
 * The `*Property` parameters name a property on the Activity and bind it reactively — the DSL block
 * re-runs on every recomposition, so a `mutableStateOf` property keeps the item in sync with no
 * further wiring.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class RailItem(
    val id: String = "",
    val text: String = "",
    val route: String = "",
    val screenTitle: String = "",
    val info: String = "",
    val textProperty: String = "",
    val badgeProperty: String = "",
    val loadingProperty: String = "",
    val disabledProperty: String = "",
)

/** An item that only appears in the expanded drawer. Parameters mirror [RailItem]. */
@Retention(AnnotationRetention.SOURCE)
annotation class MenuItem(
    val id: String = "",
    val text: String = "",
    val route: String = "",
    val screenTitle: String = "",
    val info: String = "",
    val textProperty: String = "",
    val badgeProperty: String = "",
    val loadingProperty: String = "",
    val disabledProperty: String = "",
)

/** A rail host item; sub-items attach to it by naming its [id] as their `hostId`. */
@Retention(AnnotationRetention.SOURCE)
annotation class RailHost(
    val id: String = "",
    val text: String = "",
    val route: String = "",
    val screenTitle: String = "",
    val info: String = "",
    val textProperty: String = "",
    val initiallyExpanded: Boolean = false,
)

/** A sub-item of a [RailHost]. */
@Retention(AnnotationRetention.SOURCE)
annotation class SubItem(
    val id: String = "",
    val hostId: String = "",
    val text: String = "",
    val route: String = "",
    val info: String = "",
    val textProperty: String = "",
)

/**
 * A two-state item whose label *is* its state — there is no separate switch.
 *
 * @param isCheckedProperty **Required.** Names a `Boolean` property; the generated `onClick` flips it.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class Toggle(
    val id: String = "",
    val toggleOnText: String = "",
    val toggleOffText: String = "",
    val isCheckedProperty: String = "",
    val rail: Boolean = true,
    val info: String = "",
)

/**
 * A rotating item. Supply either a static [options] list or an [optionsProperty].
 *
 * @param selectedOptionProperty **Required.** Names a `String` property; the generated `onClick`
 *   advances it through the options.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class Cycler(
    val id: String = "",
    val options: Array<String> = [],
    val optionsProperty: String = "",
    val selectedOptionProperty: String = "",
    val rail: Boolean = true,
    val info: String = "",
)

/** Visual silence in the drawer. Set [enabled] to declare one. */
@Retention(AnnotationRetention.SOURCE)
annotation class Divider(val enabled: Boolean = false)
