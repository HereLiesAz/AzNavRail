package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import org.junit.Assert.assertEquals
import org.junit.Test

class AzNavHostScopeTest {

    @Test
    fun `background should add background item`() {
        val scope = AzNavHostScopeImpl()
        scope.background(weight = 5) {}

        assertEquals(1, scope.backgrounds.size)
        assertEquals(5, scope.backgrounds[0].weight)
    }

    @Test
    fun `onscreen should add onscreen item`() {
        val scope = AzNavHostScopeImpl()
        scope.onscreen {}

        assertEquals(1, scope.onscreenItems.size)
    }

    @Test
    fun `resetHost should clear items`() {
        val scope = AzNavHostScopeImpl()
        scope.background(weight = 5) {}
        scope.onscreen {}

        assertEquals(1, scope.backgrounds.size)
        assertEquals(1, scope.onscreenItems.size)

        scope.resetHost()

        assertEquals(0, scope.backgrounds.size)
        assertEquals(0, scope.onscreenItems.size)
    }
}
