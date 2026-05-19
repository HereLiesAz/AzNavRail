package com.hereliesaz.aznavrail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises [AzNavRailScopeImpl.applyRelocReorders] — the mechanism that survives the DSL re-run
 * triggered by every recomposition. The DSL rebuilds `navItems` in declaration order; without
 * `applyRelocReorders()` the user's drag-and-drop ordering would silently revert on the very next
 * frame. These tests pin the persistence contract.
 */
class AzNavRailScopeReorderTest {

    private fun registerThreeRelocItems(scope: AzNavRailScopeImpl, hostId: String) {
        scope.azRailRelocItem(id = "reloc-1", hostId = hostId, text = "1") {}
        scope.azRailRelocItem(id = "reloc-2", hostId = hostId, text = "2") {}
        scope.azRailRelocItem(id = "reloc-3", hostId = hostId, text = "3") {}
    }

    @Test
    fun applyRelocReorders_appliesSavedOrderInPlace() {
        val scope = AzNavRailScopeImpl()
        val hostId = "host-A"
        registerThreeRelocItems(scope, hostId)

        // Pre-condition: navItems are in declaration order.
        val before = scope.navItems.filter { it.isRelocItem && it.hostId == hostId }.map { it.id }
        assertEquals(
            "Pre-condition failed: reloc items must appear in declaration order before " +
                "applyRelocReorders runs — got $before. Verify azRailRelocItem appends in order.",
            listOf("reloc-1", "reloc-2", "reloc-3"),
            before,
        )

        // Simulate a drag-end persisting a new order.
        scope.savedRelocOrders[hostId] = listOf("reloc-2", "reloc-1", "reloc-3")
        scope.applyRelocReorders()

        val after = scope.navItems.filter { it.isRelocItem && it.hostId == hostId }.map { it.id }
        assertEquals(
            "applyRelocReorders() must rewrite the reloc cluster to match savedRelocOrders[$hostId] = " +
                "[reloc-2, reloc-1, reloc-3] — got $after. Verify the in-place position rewrite in " +
                "AzNavRailScopeImpl.applyRelocReorders().",
            listOf("reloc-2", "reloc-1", "reloc-3"),
            after,
        )

        // savedRelocOrders must NOT be cleared on apply — the next recomposition must re-apply it.
        assertTrue(
            "savedRelocOrders[$hostId] must persist across applyRelocReorders() calls. " +
                "If it is cleared, the next recomposition will revert the user's drag-drop order. " +
                "Current state: ${scope.savedRelocOrders}",
            scope.savedRelocOrders.containsKey(hostId),
        )
    }

    @Test
    fun applyRelocReorders_keepsNonRelocSurroundingsUntouched() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "before", text = "Before")
        registerThreeRelocItems(scope, "host-B")
        scope.azRailItem(id = "after", text = "After")

        scope.savedRelocOrders["host-B"] = listOf("reloc-3", "reloc-2", "reloc-1")
        scope.applyRelocReorders()

        val allIds = scope.navItems.map { it.id }
        assertEquals(
            "applyRelocReorders() must only swap items inside the reloc cluster; non-reloc " +
                "neighbours must stay anchored. Got navItems = $allIds. The position rewrite " +
                "should target only the indices listed by `indexed`.",
            listOf("before", "reloc-3", "reloc-2", "reloc-1", "after"),
            allIds,
        )
    }

    @Test
    fun applyRelocReorders_prunesEntryWhenSavedIdsReferenceNonexistentItem() {
        val scope = AzNavRailScopeImpl()
        val hostId = "host-C"
        registerThreeRelocItems(scope, hostId)

        // The saved order names an id that doesn't exist in the current cluster ("phantom"),
        // which means the DSL was edited between sessions. The library must refuse to apply
        // a stale order rather than risk reordering unrelated items.
        scope.savedRelocOrders[hostId] = listOf("reloc-1", "reloc-2", "phantom")

        val before = scope.navItems.filter { it.isRelocItem && it.hostId == hostId }.map { it.id }
        scope.applyRelocReorders()
        val after = scope.navItems.filter { it.isRelocItem && it.hostId == hostId }.map { it.id }

        assertEquals(
            "applyRelocReorders() must NOT mutate the cluster when savedOrder references an id " +
                "that is missing from the current navItems — the saved order is stale. " +
                "Before = $before; After = $after; saved = ${scope.savedRelocOrders[hostId]}. " +
                "Verify the `currentIds != savedOrder.toSet()` guard.",
            before,
            after,
        )
        assertFalse(
            "Stale savedRelocOrders entry must be pruned so the next reorder rebuilds it cleanly. " +
                "Still present: ${scope.savedRelocOrders}",
            scope.savedRelocOrders.containsKey(hostId),
        )
    }

    @Test
    fun applyRelocReorders_prunesEntryWhenSavedCountDiffersFromCluster() {
        val scope = AzNavRailScopeImpl()
        val hostId = "host-D"
        registerThreeRelocItems(scope, hostId)

        // 2 saved ids vs 3 cluster items: the cluster shape no longer matches the saved order.
        scope.savedRelocOrders[hostId] = listOf("reloc-1", "reloc-2")

        val before = scope.navItems.filter { it.isRelocItem && it.hostId == hostId }.map { it.id }
        scope.applyRelocReorders()
        val after = scope.navItems.filter { it.isRelocItem && it.hostId == hostId }.map { it.id }

        assertEquals(
            "applyRelocReorders() must skip clusters whose size has diverged from the saved order " +
                "(saved 2 ids, cluster has ${before.size}). Before = $before; After = $after. " +
                "Verify the size-vs-set guard in applyRelocReorders().",
            before,
            after,
        )
        assertFalse(
            "Stale savedRelocOrders entry must be pruned when set sizes differ — still present: " +
                "${scope.savedRelocOrders}",
            scope.savedRelocOrders.containsKey(hostId),
        )
    }

    @Test
    fun applyRelocReorders_prunesEntryWhenClusterNoLongerExists() {
        val scope = AzNavRailScopeImpl()
        // No reloc items registered for "ghost-host" at all.
        scope.azRailItem(id = "x", text = "X")
        scope.savedRelocOrders["ghost-host"] = listOf("reloc-1", "reloc-2")

        scope.applyRelocReorders()

        assertFalse(
            "savedRelocOrders for hosts whose reloc cluster no longer exists must be pruned to " +
                "avoid leaking dead state across recompositions. Still present: ${scope.savedRelocOrders}",
            scope.savedRelocOrders.containsKey("ghost-host"),
        )
    }

    @Test
    fun applyRelocReorders_isNoOpWhenSavedOrdersIsEmpty() {
        val scope = AzNavRailScopeImpl()
        registerThreeRelocItems(scope, "host-E")
        val before = scope.navItems.map { it.id }
        scope.applyRelocReorders()
        val after = scope.navItems.map { it.id }
        assertEquals(
            "applyRelocReorders() must be a true no-op when savedRelocOrders is empty — " +
                "Before = $before, After = $after. The first line should early-return.",
            before,
            after,
        )
    }
}
