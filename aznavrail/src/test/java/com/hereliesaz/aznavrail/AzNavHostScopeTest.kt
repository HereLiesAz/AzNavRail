package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import android.content.Context
import androidx.navigation.NavHostController
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
    fun `resetHost should clear items and reset railScope`() {
        val scope = AzNavHostScopeImpl()
        scope.background(weight = 5) {}
        scope.onscreen {}

        // Add some state to the railScope
        scope.azMenuItem(id = "test", text = "Test")

        assertEquals(1, scope.backgrounds.size)
        assertEquals(1, scope.onscreenItems.size)
        assertEquals(1, scope.getRailScopeImpl().navItems.size)

        scope.resetHost()

        assertEquals(0, scope.backgrounds.size)
        assertEquals(0, scope.onscreenItems.size)
        // Verify that the inner railScope was also reset
        assertEquals(0, scope.getRailScopeImpl().navItems.size)
    }

    @Test
    fun `resetHost should maintain navController reference`() {
        val scope = AzNavHostScopeImpl()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavHostController(context)

        scope.setController(navController)
        assertNotNull(scope.navController)
        assertNotNull(scope.getRailScopeImpl().navController)

        scope.resetHost()

        assertEquals(navController, scope.navController)
        assertEquals(navController, scope.getRailScopeImpl().navController)
    }
}
