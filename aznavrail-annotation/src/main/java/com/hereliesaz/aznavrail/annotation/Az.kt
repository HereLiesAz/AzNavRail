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
    val parent: String = "" // Added to link to Hosts or NestedParents
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class RailHost(
    val isValid: Boolean = true,
    val id: String = "",
    val text: String = "",
    val icon: Int = 0
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
annotation class Background(
    val isValid: Boolean = true,
    val weight: Int = 0
)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Az(
    val app: App = App(isValid = false),
    val rail: RailItem = RailItem(isValid = false),
    val host: RailHost = RailHost(isValid = false),
    val nested: NestedRail = NestedRail(isValid = false),
    val background: Background = Background(isValid = false)
)
