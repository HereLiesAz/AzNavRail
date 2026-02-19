// aznavrail-annotation/src/main/java/com/hereliesaz/aznavrail/annotation/Az.kt
package com.hereliesaz.aznavrail.annotation

import com.hereliesaz.aznavrail.model.AzDockingSide

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class App(
    val isValid: Boolean = true,
    val dock: AzDockingSide = AzDockingSide.LEFT,
    val packButtons: Boolean = false,
    val noMenu: Boolean = false,
    val vibrate: Boolean = false,
    val displayAppName: Boolean = false,
    val activeClassifiers: Array<String> = [],
    val usePhysicalDocking: Boolean = false,
    val showFooter: Boolean = true
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class RailItem(
    val isValid: Boolean = true,
    val id: String = "",
    val text: String = "",
    val icon: Int = 0,
    val home: Boolean = false,
    val parent: String = "",
    val disabled: Boolean = false,
    val screenTitle: String = "",
    val info: String = "",
    val classifiers: Array<String> = []
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class MenuItem(
    val isValid: Boolean = true,
    val id: String = "",
    val text: String = "",
    val icon: Int = 0,
    val parent: String = "",
    val disabled: Boolean = false,
    val screenTitle: String = "",
    val info: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class RailHost(
    val isValid: Boolean = true,
    val id: String = "",
    val text: String = "",
    val icon: Int = 0,
    val isMenu: Boolean = false,
    val disabled: Boolean = false,
    val screenTitle: String = "",
    val info: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class NestedRail(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val text: String = "",
    val icon: Int = 0,
    val disabled: Boolean = false,
    val screenTitle: String = "",
    val info: String = "",
    val classifiers: Array<String> = []
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Toggle(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val toggleOnText: String = "",
    val toggleOffText: String = "",
    val isMenu: Boolean = false,
    val disabled: Boolean = false,
    val screenTitle: String = "",
    val info: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Cycler(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val options: Array<String> = [],
    val isMenu: Boolean = false,
    val disabled: Boolean = false,
    val disabledOptions: Array<String> = [],
    val screenTitle: String = "",
    val info: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class RelocItem(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val text: String = "",
    val hiddenMenuRoutes: Array<String> = [],
    val disabled: Boolean = false,
    val screenTitle: String = "",
    val info: String = "",
    val classifiers: Array<String> = []
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Background(
    val isValid: Boolean = true,
    val weight: Int = 0
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Divider(
    val isValid: Boolean = true
)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Az(
    val app: App = App(isValid = false),
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
