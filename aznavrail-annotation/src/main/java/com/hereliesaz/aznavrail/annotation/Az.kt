package com.hereliesaz.aznavrail.annotation

import com.hereliesaz.aznavrail.model.AzDockingSide

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class App(
    val isValid: Boolean = true,
    val dock: AzDockingSide = AzDockingSide.LEFT
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class RailItem(
    val isValid: Boolean = true,
    val id: String = "",
    val text: String = "",
    val icon: Int = 0,
    val home: Boolean = false,
    val parent: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class MenuItem(
    val isValid: Boolean = true,
    val id: String = "",
    val text: String = "",
    val icon: Int = 0,
    val parent: String = ""
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class RailHost(
    val isValid: Boolean = true,
    val id: String = "",
    val text: String = "",
    val icon: Int = 0,
    val isMenu: Boolean = false
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class NestedRail(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val text: String = "",
    val icon: Int = 0
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Toggle(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val toggleOnText: String = "",
    val toggleOffText: String = "",
    val isMenu: Boolean = false
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Cycler(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val options: Array<String> = [],
    val isMenu: Boolean = false
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class RelocItem(
    val isValid: Boolean = true,
    val parent: String = "",
    val id: String = "",
    val text: String = "",
    val hiddenMenuRoutes: Array<String> = [] // Statically bridging the hidden menu list
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
