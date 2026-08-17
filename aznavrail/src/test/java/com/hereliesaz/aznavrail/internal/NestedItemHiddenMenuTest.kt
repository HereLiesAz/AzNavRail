package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import com.hereliesaz.aznavrail.model.HiddenMenuItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression coverage for the bug where an item nested inside another item's `nestedContent`
 * had no way to open its own hidden menu: [NestedRail] rendered nested items via
 * [NestedItemWrapper], which wired `onClick` but never a long-press-to-open-hidden-menu gesture,
 * so `hiddenMenuOpenId`/`onMenuOpen` were never even threaded down to it.
 */
@RunWith(RobolectricTestRunner::class)
class NestedItemHiddenMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val childWithMenu = AzNavItem(
        id = "child_with_menu",
        text = "ChildA",
        isRailItem = true,
        hiddenMenuItems = listOf(HiddenMenuItem(id = "child_with_menu_hidden_0", text = "Rename")),
    )

    private val childWithoutMenu = AzNavItem(
        id = "child_no_menu",
        text = "ChildB",
        isRailItem = true,
    )

    @Test
    fun `long-press on a nested item with a hidden menu opens it`() {
        var hiddenMenuOpenId by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            NestedRail(
                parentItem = AzNavItem(id = "parent", text = "Parent", isRailItem = true),
                items = listOf(childWithMenu, childWithoutMenu),
                currentDestination = null,
                activeColor = Color.Red,
                activeClassifiers = emptySet(),
                onItemSelected = {},
                alignment = AzNestedRailAlignment.VERTICAL,
                isRightDocked = false,
                hiddenMenuOpenId = hiddenMenuOpenId,
                onMenuOpen = { hiddenMenuOpenId = it },
                onHiddenMenuDismiss = { hiddenMenuOpenId = null },
            )
        }

        // The hidden menu is not shown until the long-press fires.
        composeTestRule.onNodeWithText("Rename").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("ChildA").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Rename").assertExists()
    }

    @Test
    fun `long-press on a nested item without a hidden menu does nothing`() {
        var hiddenMenuOpenId by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            NestedRail(
                parentItem = AzNavItem(id = "parent", text = "Parent", isRailItem = true),
                items = listOf(childWithMenu, childWithoutMenu),
                currentDestination = null,
                activeColor = Color.Red,
                activeClassifiers = emptySet(),
                onItemSelected = {},
                alignment = AzNestedRailAlignment.VERTICAL,
                isRightDocked = false,
                hiddenMenuOpenId = hiddenMenuOpenId,
                onMenuOpen = { hiddenMenuOpenId = it },
                onHiddenMenuDismiss = { hiddenMenuOpenId = null },
            )
        }

        composeTestRule.onNodeWithContentDescription("ChildB").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        // Neither item's hidden menu should have opened: ChildB declared none, and the long-press
        // on it must not spuriously open ChildA's.
        composeTestRule.onNodeWithText("Rename").assertDoesNotExist()
    }
}
