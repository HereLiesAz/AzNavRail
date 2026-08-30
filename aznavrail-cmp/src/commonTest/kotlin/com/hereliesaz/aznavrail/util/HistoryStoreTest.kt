package com.hereliesaz.aznavrail.util

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test for [HistoryStore.addEntry]'s ordering guarantee: it used to launch its mutation
 * on a detached `GlobalScope` job and return immediately, so a [HistoryStore.getSuggestions] call
 * issued right after (as `AzTextBox` does, from its own `LaunchedEffect(text)`) could race the write
 * on an unrelated dispatcher and miss the just-added entry — a divergence from the Android sibling's
 * synchronous `HistoryManager.addEntry`. Now that `addEntry` itself suspends until the write lands,
 * awaiting it (as this test does, and as `AzTextBox` does via its own `CoroutineScope`) guarantees
 * the entry is visible to the very next call.
 */
class HistoryStoreTest {

    @Test
    fun addEntry_isImmediatelyVisibleToGetSuggestions() = runBlocking {
        val ctx = "history-store-test-ordering"
        HistoryStore.updateSettings(5)
        HistoryStore.addEntry("first entry", ctx)
        val suggestions = HistoryStore.getSuggestions("first", ctx)
        assertEquals(listOf("first entry"), suggestions)
    }

    @Test
    fun addEntry_movesARepeatedValueToTheFront() = runBlocking {
        val ctx = "history-store-test-reorder"
        HistoryStore.updateSettings(5)
        HistoryStore.addEntry("alpha", ctx)
        HistoryStore.addEntry("beta", ctx)
        HistoryStore.addEntry("alpha", ctx)
        assertEquals(listOf("alpha", "beta"), HistoryStore.getSuggestions("", ctx))
    }
}
