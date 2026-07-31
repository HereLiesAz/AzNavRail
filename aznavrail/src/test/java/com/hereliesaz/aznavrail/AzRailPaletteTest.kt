package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rail publishes the colour it is actually wearing so every other AzNavRail surface can wear it
 * too. These pin the resolution order, because the failure they guard against — a drop-down or an
 * About reader arriving in the app theme's purple beside a white rail — is invisible to a type
 * checker and obvious to a user.
 */
class AzRailPaletteTest {

    private fun item(id: String, color: Color?) =
        AzNavItem(id = id, text = id, isRailItem = true, color = color)

    @Test
    fun `an explicit activeColor wins outright`() {
        val accent = azResolveRailAccent(
            activeColor = Color.Red,
            items = listOf(item("a", Color.White), item("b", Color.White)),
        )
        assertEquals(Color.Red, accent)
    }

    @Test
    fun `without an activeColor the rail reads as the colour most of its items are`() {
        val accent = azResolveRailAccent(
            activeColor = Color.Unspecified,
            items = listOf(
                item("a", Color.White),
                item("b", Color.White),
                item("c", Color.Green),
                item("d", null),
            ),
        )
        assertEquals(Color.White, accent)
    }

    @Test
    fun `a rail that states no colour leaves the theme in charge`() {
        val accent = azResolveRailAccent(
            activeColor = Color.Unspecified,
            items = listOf(item("a", null), item("b", null)),
        )
        assertEquals(Color.Unspecified, accent)
    }

    @Test
    fun `the scope derives its accent from the items it collected`() {
        val scope = AzNavRailScopeImpl()
        scope.reset()
        scope.azRailItem(id = "one", text = "One", color = Color.White)
        scope.azRailItem(id = "two", text = "Two", color = Color.White)
        assertEquals(Color.White, scope.railAccent)
    }
}

/** The borderless shape family keeps the footprint of the base shape it names. */
class AzButtonShapeBorderlessTest {

    @Test
    fun `only the NONE family is borderless`() {
        assertTrue(AzButtonShape.NONE.isBorderless)
        assertTrue(AzButtonShape.NONE_SQUARE.isBorderless)
        assertTrue(AzButtonShape.NONE_CIRCLE.isBorderless)
        assertFalse(AzButtonShape.CIRCLE.isBorderless)
        assertFalse(AzButtonShape.SQUARE.isBorderless)
        assertFalse(AzButtonShape.RECTANGLE.isBorderless)
        assertFalse(AzButtonShape.TRIANGLE.isBorderless)
    }

    @Test
    fun `each borderless shape keeps the footprint it names`() {
        assertEquals(AzButtonShape.RECTANGLE, AzButtonShape.NONE.baseShape)
        assertEquals(AzButtonShape.SQUARE, AzButtonShape.NONE_SQUARE.baseShape)
        assertEquals(AzButtonShape.CIRCLE, AzButtonShape.NONE_CIRCLE.baseShape)
    }

    @Test
    fun `a bordered shape is its own base`() {
        AzButtonShape.entries.filterNot { it.isBorderless }.forEach {
            assertEquals(it, it.baseShape)
        }
    }
}

/**
 * About is a rail item — appended last when the developer declared none, replaced entirely when they
 * declared their own. It persists; it is not fixed.
 */
class AzAboutRailItemTest {

    @Test
    fun `the rail item is opt-out and on by default`() {
        val scope = AzNavRailScopeImpl()
        scope.reset()
        assertTrue(scope.advancedConfig.aboutRailItem)
        scope.azAbout(aboutRailItem = false)
        assertFalse(scope.advancedConfig.aboutRailItem)
    }

    @Test
    fun `a declared About item lands where it was declared and carries the flag`() {
        val scope = AzNavRailScopeImpl()
        scope.reset()
        scope.azRailItem(id = "first", text = "First")
        scope.azAboutRailItem(id = "about", text = "?", color = Color.White)
        scope.azRailItem(id = "last", text = "Last")

        assertEquals(listOf("first", "about", "last"), scope.navItems.map { it.id })
        val about = scope.navItems.single { it.isAboutItem }
        assertEquals("about", about.id)
        assertEquals(Color.White, about.color)
        assertTrue(about.isRailItem)
        // The reader is a place you go, not a menu action — closing the drawer would hide the rail
        // the reader is drawn beside.
        assertFalse(about.collapseOnClick)
    }

    @Test
    fun `no About item is declared unless one is asked for`() {
        val scope = AzNavRailScopeImpl()
        scope.reset()
        scope.azRailItem(id = "only", text = "Only")
        assertNull(scope.navItems.firstOrNull { it.isAboutItem })
    }
}
