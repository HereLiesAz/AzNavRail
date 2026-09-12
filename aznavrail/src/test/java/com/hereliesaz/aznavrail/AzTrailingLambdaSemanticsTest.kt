package com.hereliesaz.aznavrail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AzTrailingLambdaSemanticsTest {

    @Test
    fun `rail subitem trailing lambda is deferred click callback`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailHostItem(id = "host", text = "Host")
        var calls = 0

        scope.azRailSubItem(id = "child", hostId = "host", text = "Child") {
            calls++
        }

        assertEquals("registration must not execute the trailing lambda", 0, calls)
        assertNotNull(scope.onClickMap["child"])
        scope.onClickMap["child"]?.invoke()
        assertEquals(1, calls)
    }

    @Test
    fun `hidden menu remains explicit and separate from click`() {
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
    fun `click lambda not executed during recomposition`() {
        val scope = AzNavRailScopeImpl()
        var calls = 0

        // Simulate three recompositions (reset + re-register each time)
        repeat(3) {
            scope.reset()
            scope.azRailHostItem(id = "host", text = "Host")
            scope.azRailSubItem(id = "child", hostId = "host", text = "Child") { calls++ }
        }

        assertEquals("lambda must not have run during any of the three registrations", 0, calls)
        assertNotNull(scope.onClickMap["child"])
        scope.onClickMap["child"]?.invoke()
        assertEquals(1, calls)
    }

    @Test
    fun `hidden menu builder runs at registration but does not execute its actions`() {
        val scope = AzNavRailScopeImpl()
        var actionCalls = 0

        scope.azRailItem(id = "item", text = "Item", hiddenMenu = {
            listItem("Do Thing") { actionCalls++ }
        })

        assertEquals(
            "hidden menu action must not execute during DSL registration",
            0, actionCalls
        )
        // Action is stored in hiddenMenuOnClickMap and fires only on explicit invocation
        val key = scope.navItems.first().hiddenMenuItems!!.first().id
        scope.hiddenMenuOnClickMap[key]?.invoke()
        assertEquals(1, actionCalls)
    }

    @Test
    fun `rail item top-level trailing lambda is click`() {
        val scope = AzNavRailScopeImpl()
        var clicks = 0

        scope.azRailItem(id = "home", text = "Home") { clicks++ }

        assertEquals(0, clicks)
        assertNotNull(scope.onClickMap["home"])
        scope.onClickMap["home"]?.invoke()
        assertEquals(1, clicks)
    }

    @Test
    fun `rail host item trailing lambda is click`() {
        val scope = AzNavRailScopeImpl()
        var clicks = 0

        scope.azRailHostItem(id = "host", text = "Host") { clicks++ }

        assertEquals(0, clicks)
        assertNotNull(scope.onClickMap["host"])
        scope.onClickMap["host"]?.invoke()
        assertEquals(1, clicks)
    }
}
