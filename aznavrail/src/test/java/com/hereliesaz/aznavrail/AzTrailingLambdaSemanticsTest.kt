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
}
