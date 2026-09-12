package com.hereliesaz.aznavrail.util

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.BeforeTest

class HistoryStoreTest {

    private val memoryStore = mutableMapOf<String, String>()

    @BeforeTest
    fun setUp() {
        memoryStore.clear()
        HistoryStore.testSettingsOverride = { key, value -> memoryStore[key] = value }
        HistoryStore.testSettingsReader = { key, default -> memoryStore[key] ?: default }
    }

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
