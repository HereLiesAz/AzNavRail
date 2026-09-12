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
}
