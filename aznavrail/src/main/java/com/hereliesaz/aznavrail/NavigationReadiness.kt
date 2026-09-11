package com.hereliesaz.aznavrail

import androidx.navigation.NavController

/**
 * Navigates immediately once this controller owns a graph, or defers the request until the graph
 * produces its first destination.
 *
 * A [NavController] can be attached to [AzHostActivityLayout] one composition before [AzNavHost]
 * installs its graph. Calling [NavController.navigate] during that window throws
 * `IllegalArgumentException: Navigation graph has not been set`. AzNavRail's own route dispatchers
 * all use this function so a cold-start timing race cannot crash the host application.
 *
 * Requests made before readiness are kept in call order and are dispatched exactly once when the
 * first destination arrives. Invalid routes still fail normally after the graph is ready; this does
 * not swallow navigation errors.
 */
fun NavController.azNavigateWhenReady(route: String) {
    if (currentBackStackEntry != null) {
        navigate(route)
        return
    }

    AzPendingNavigation.enqueue(this, route)
}

private object AzPendingNavigation {
    private val routes = mutableMapOf<NavController, MutableList<String>>()
    private val listeners = mutableMapOf<NavController, NavController.OnDestinationChangedListener>()

    fun enqueue(controller: NavController, route: String) {
        routes.getOrPut(controller) { mutableListOf() }.add(route)
        if (listeners.containsKey(controller)) return

        lateinit var listener: NavController.OnDestinationChangedListener
        listener = NavController.OnDestinationChangedListener { readyController, _, _ ->
            readyController.removeOnDestinationChangedListener(listener)
            listeners.remove(readyController)
            val pending = routes.remove(readyController).orEmpty()
            pending.forEach { readyController.navigate(it) }
        }
        listeners[controller] = listener
        controller.addOnDestinationChangedListener(listener)
    }
}
