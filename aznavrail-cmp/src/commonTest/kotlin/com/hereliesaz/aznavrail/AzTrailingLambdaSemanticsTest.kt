package com.hereliesaz.aznavrail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AzTrailingLambdaSemanticsTest {

    @Test
    fun trailingLambdaIsDeferredClickCallback() {
        val scope = AzNavRailScopeImpl()
        scope.azRailHostItem(id = "host", text = "Host")
        var calls = 0

        scope.azRailSubItem(id = "child", hostId = "host", text = "Child") { calls++ }

        assertEquals(0, calls)
        assertNotNull(scope.onClickMap["child"])
        scope.onClickMap["child"]?.invoke()
        assertEquals(1, calls)
    }

    @Test
    fun hiddenMenuRemainsExplicitAndSeparateFromClick() {
        val scope = AzNavRailScopeImpl()
        scope.azRailHostItem(id = "host", text = "Host")
        var clicks = 0
        var hiddenActions = 0

        scope.azRailSubItem(
            id = "child",
            hostId = "host",
            text = "Child",
            hiddenMenu = { listItem("Hidden") { hiddenActions++ } },
        ) { clicks++ }

        assertEquals(0, clicks)
        assertEquals(0, hiddenActions)
        scope.onClickMap["child"]?.invoke()
        assertEquals(1, clicks)
        assertEquals(0, hiddenActions)
    }

    @Test
    fun clickLambdaNotExecutedDuringRecomposition() {
        val scope = AzNavRailScopeImpl()
        scope.azRailHostItem(id = "host", text = "Host")
        var calls = 0

        // Simulate three recompositions (reset + re-register each time)
        repeat(3) {
            scope.reset()
            scope.azRailHostItem(id = "host", text = "Host")
            scope.azRailSubItem(id = "child", hostId = "host", text = "Child") { calls++ }
        }

        // Three registrations — none should have executed the lambda
        assertEquals(0, calls)
        // The map holds the most-recently registered lambda
        assertNotNull(scope.onClickMap["child"])
        scope.onClickMap["child"]?.invoke()
        assertEquals(1, calls)
    }

    @Test
    fun hiddenMenuBuilderRunsButDoesNotExecuteItsActions() {
        val scope = AzNavRailScopeImpl()
        var actionCalls = 0

        scope.azRailItem(id = "item", text = "Item", hiddenMenu = {
            listItem("Do Thing") { actionCalls++ }
        })

        // The hidden-menu builder runs during registration to populate the item list,
        // but the action inside listItem must not execute during registration.
        assertEquals(0, actionCalls)

        // The action is stored and executes only when invoked via the map
        val key = scope.navItems.first().hiddenMenuItems!!.first().id
        scope.hiddenMenuOnClickMap[key]?.invoke()
        assertEquals(1, actionCalls)
    }

    @Test
    fun railItemTopLevelTrailingLambdaIsClick() {
        val scope = AzNavRailScopeImpl()
        var clicks = 0

        scope.azRailItem(id = "home", text = "Home") { clicks++ }

        assertEquals(0, clicks)
        assertNotNull(scope.onClickMap["home"])
        scope.onClickMap["home"]?.invoke()
        assertEquals(1, clicks)
    }

    @Test
    fun railHostItemTrailingLambdaIsClick() {
        val scope = AzNavRailScopeImpl()
        var clicks = 0

        scope.azRailHostItem(id = "host", text = "Host") { clicks++ }

        assertEquals(0, clicks)
        assertNotNull(scope.onClickMap["host"])
        scope.onClickMap["host"]?.invoke()
        assertEquals(1, clicks)
    }
}
