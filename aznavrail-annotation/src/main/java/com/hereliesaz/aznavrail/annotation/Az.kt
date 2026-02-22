// aznavrail-annotation/src/main/java/com/hereliesaz/aznavrail/annotation/Az.kt
package com.hereliesaz.aznavrail.annotation

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class App(
    val isValid: Boolean = true,
    val dock: String = "LEFT", // Use String for literals in annotations to avoid cyclic dependency
    val packButtons: Boolean = false,
    val noMenu: Boolean = false,
    val vibrate: Boolean = false,
    val displayAppName: Boolean = false,
    val activeClassifiers: Array<String> = [],
    val usePhysicalDocking: Boolean = false,
    val showFooter: Boolean = true,
    val expandedWidth: Int = -1,
    val collapsedWidth: Int = -1,
    val initiallyExpanded: Boolean = false,
    val disableSwipeToOpen: Boolean = false
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Theme(
    val isValid: Boolean = true,
    val activeColorHex: String = "", 
    val defaultShape: String = "CIRCLE",
    val headerIconShape: String = "CIRCLE"
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Advanced(
    val isValid: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingProperty: String = "", // Dynamic binding for loading state
    val infoScreen: Boolean = false,
    val enableRailDragging: Boolean = false,
    val overlayServiceClass: String = "",
    val onUndock: String = "",
    val onRailDrag: String = "",
    val onOverlayDrag: String = "",
    val onItemGloballyPositioned: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class RailItem(
    val isValid: Boolean = true,
    val id: String = "",
    val text: String = "",
    val textProperty: String = "", // Dynamic binding for label
    val icon: Int = 0,
    val iconText: String = "", 
    val iconTextProperty: String = "", // Dynamic binding for badge/icon text
    val home: Boolean = false,
    val parent: String = "",
    val disabled: Boolean = false,
    val disabledProperty: String = "", // Dynamic binding for disabled state
    val visibleProperty: String = "", // Dynamic binding for visibility
    val screenTitle: String = "",
    val info: String = "",
    val classifiers: Array<String> = [],
    val onFocus: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class MenuItem(
    val isValid: Boolean = true,
    val id: String = "",
    val text: String = "",
    val textProperty: String = "",
    val icon: Int = 0,
    val iconText: String = "",
    val iconTextProperty: String = "",
    val parent: String = "",
    val disabled: Boolean = false,
    val disabledProperty: String = "",
    val visibleProperty: String = "",
    val screenTitle: String = "",
    val info: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class RailHost(
    val isValid: Boolean = true,
    val id: String = "",
    val text: String = "",
    val textProperty: String = "",
    val icon: Int = 0,
    val iconText: String = "",
    val iconTextProperty: String = "",
    val isMenu: Boolean = false,
    val disabled: Boolean = false,
    val disabledProperty: String = "",
    val visibleProperty: String = "",
    val screenTitle: String = "",
    val info: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class NestedRail(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val text: String = "",
    val textProperty: String = "",
    val icon: Int = 0,
    val iconText: String = "",
    val iconTextProperty: String = "",
    val disabled: Boolean = false,
    val disabledProperty: String = "",
    val visibleProperty: String = "",
    val screenTitle: String = "",
    val info: String = "",
    val classifiers: Array<String> = [],
    val onFocus: String = "",
    val alignment: String = "VERTICAL"
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Toggle(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val isCheckedProperty: String = "", // MANDATORY for dynamic toggles
    val toggleOnText: String = "",
    val toggleOffText: String = "",
    val isMenu: Boolean = false,
    val disabled: Boolean = false,
    val disabledProperty: String = "",
    val visibleProperty: String = "",
    val screenTitle: String = "",
    val info: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Cycler(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val options: Array<String> = [],
    val optionsProperty: String = "", // Dynamic list of options
    val selectedOptionProperty: String = "", // Dynamic selected option
    val isMenu: Boolean = false,
    val disabled: Boolean = false,
    val disabledOptionsProperty: String = "", // Dynamic disabled options
    val screenTitle: String = "",
    val info: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class RelocItem(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val text: String = "",
    val textProperty: String = "",
    val hiddenMenuRoutes: Array<String> = [],
    val hiddenMenuActions: Array<String> = [], 
    val hiddenMenuInputs: Array<String> = [],  
    val disabled: Boolean = false,
    val disabledProperty: String = "",
    val visibleProperty: String = "",
    val screenTitle: String = "",
    val info: String = "",
    val classifiers: Array<String> = [],
    val onFocus: String = "",
    val onRelocate: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Background(
    val isValid: Boolean = true,
    val weight: Int = 0
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Divider(
    val isValid: Boolean = true,
    val visibleProperty: String = ""
)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Az(
    val app: App = App(isValid = false),
    val theme: Theme = Theme(isValid = false),
    val advanced: Advanced = Advanced(isValid = false),
    val rail: RailItem = RailItem(isValid = false),
    val menu: MenuItem = MenuItem(isValid = false),
    val host: RailHost = RailHost(isValid = false),
    val nested: NestedRail = NestedRail(isValid = false),
    val toggle: Toggle = Toggle(isValid = false),
    val cycler: Cycler = Cycler(isValid = false),
    val reloc: RelocItem = RelocItem(isValid = false),
    val background: Background = Background(isValid = false),
    val divider: Divider = Divider(isValid = false)
)
