package com.hereliesaz.aznavrail

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Asserts that the duplicate-id check fires with an *actionable* message — the goal is to make the
 * fix obvious to a developer who didn't read the source. If you weaken the message, this test
 * should fail loudly.
 */
class AzNavRailScopeDuplicateIdTest {

    @Test
    fun duplicateId_acrossSamePool_throws_withActionableMessage() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "duplicate", text = "First")

        val e = try {
            scope.azRailItem(id = "duplicate", text = "Second")
            null
        } catch (iae: IllegalArgumentException) {
            iae
        }
        assertNotNull(
            "Declaring a second item with id = \"duplicate\" must throw IllegalArgumentException — " +
                "the rail uses id as the key in onClickMap/onFocusMap/onRelocateMap, so silent overwrite " +
                "would lose handlers. Got null (no throw).",
            e,
        )

        val message = e!!.message.orEmpty()
        // Educational expectations: the error must (1) name the duplicate id, (2) tell the user it
        // has to be unique, and (3) propose a concrete fix.
        assertTrue(
            "Duplicate-id error must echo the offending id 'duplicate' so the developer can find it. " +
                "Got: $message",
            message.contains("duplicate"),
        )
        assertTrue(
            "Duplicate-id error must contain 'unique' guidance to point at the contract being " +
                "violated (every item must have a unique id). Got: $message",
            message.contains("unique", ignoreCase = true) && message.contains("id", ignoreCase = true),
        )
        assertTrue(
            "Duplicate-id error must suggest a fix (eg. rename to '<id>-2' or '<id>_nested') so the " +
                "developer doesn't have to read the source to know what to do. Got: $message",
            message.contains("rename", ignoreCase = true) || message.contains("Fix:", ignoreCase = true),
        )
    }

    @Test
    fun duplicateId_betweenRailAndMenu_throws() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "shared", text = "Rail")
        try {
            scope.azMenuItem(id = "shared", text = "Menu")
            fail(
                "Declaring an azMenuItem with the same id as an existing azRailItem must throw — " +
                    "the global id namespace is shared across rail and menu items. " +
                    "No exception was raised."
            )
        } catch (iae: IllegalArgumentException) {
            // Expected.
            assertTrue(
                "Cross-pool duplicate error must still name the duplicate id 'shared'. Got: ${iae.message}",
                iae.message.orEmpty().contains("shared"),
            )
        }
    }

    @Test
    fun duplicateId_betweenNestedAndParent_throws() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "shared", text = "Parent")
        try {
            scope.azNestedRail(id = "wrapper", text = "W") {
                azRailItem("shared", "Nested duplicate")
            }
            fail(
                "Declaring a nested child with the same id as an existing parent item must throw — " +
                    "the globalIdSet is shared across nested scopes by design. No exception was raised."
            )
        } catch (iae: IllegalArgumentException) {
            // Expected: checkId() in the nested scope sees the id already in globalIdSet.
            assertTrue(
                "Nested duplicate error must name the id 'shared'. Got: ${iae.message}",
                iae.message.orEmpty().contains("shared"),
            )
        } catch (ise: IllegalStateException) {
            // Acceptable: the alternative codepath in azNestedRail throws ISE on map collision.
            assertTrue(
                "Nested duplicate error must name the id 'shared'. Got: ${ise.message}",
                ise.message.orEmpty().contains("shared"),
            )
        }
    }
}
