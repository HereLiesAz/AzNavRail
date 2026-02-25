// FILE: ./aznavrail/src/test/java/com/hereliesaz/aznavrail/AzComponentErrorTest.kt
package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.model.AzButtonShape
import org.junit.Test
import java.util.Collections.emptySet

class AzComponentErrorTest {

    private val scope = AzNavRailScopeImpl()

    @Test(expected = IllegalArgumentException::class)
    fun `azRailItem throws exception when text is empty`() {
        scope.azRailItem(
            id = "test_item",
            text = "",
            onClick = {}
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `azRailItem throws exception when text is blank`() {
        scope.azRailItem(
            id = "test_item",
            text = "   ",
            onClick = {}
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `azMenuCycler throws exception when selectedOption is not in options`() {
        scope.azMenuCycler(
            id = "test_cycler",
            options = listOf("A", "B"),
            selectedOption = "C",
            onClick = {}
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `azMenuToggle throws exception when toggle texts are empty`() {
        scope.azMenuToggle(
            id = "test_toggle",
            isChecked = false,
            toggleOnText = "",
            toggleOffText = "Off",
            onClick = {}
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `azMenuToggle throws exception when toggle off text is empty`() {
        scope.azMenuToggle(
            id = "test_toggle",
            isChecked = false,
            toggleOnText = "On",
            toggleOffText = "",
            onClick = {}
        )
    }
}
