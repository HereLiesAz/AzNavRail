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
 * first destination arrives. Duplicate routes for the same controller are coalesced — a route
 * already in the pending queue is not enqueued a second time. Invalid routes still fail normally
 * after the graph is ready; this does not swallow navigation errors.
 *
 * When a controller is replaced (e.g. Activity recreation), call [cancelPendingNavigation] on the
 * old controller to drop its queue and detach the listener so the old instance can be GC'd.
 */
fun NavController.azNavigateWhenReady(route: String) {
    if (currentBackStackEntry != null) {
        navigate(route)
        return
    }

    AzPendingNavigation.enqueue(this, route)
}

/**
 * Drops any pending routes for this controller and removes the readiness listener.
 * Call from [AzNavHostScopeImpl.setController] before replacing the held controller reference.
 */
fun NavController.cancelPendingNavigation() {
    AzPendingNavigation.cancel(this)
}

internal object AzPendingNavigation {
    private val routes = mutableMapOf<NavController, MutableList<String>>()
    private val listeners = mutableMapOf<NavController, NavController.OnDestinationChangedListener>()

    fun enqueue(controller: NavController, route: String) {
        val queue = routes.getOrPut(controller) { mutableListOf() }
        if (route in queue) return   // coalesce: identical pending route already waiting

        queue.add(route)
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

    fun cancel(controller: NavController) {
        routes.remove(controller)
        listeners.remove(controller)?.let { controller.removeOnDestinationChangedListener(it) }
    }

    /** Exposed for testing only — clears all state between test cases. */
    internal fun clearAllForTest() {
        listeners.forEach { (controller, listener) ->
            controller.removeOnDestinationChangedListener(listener)
        }
        routes.clear()
        listeners.clear()
    }
}
