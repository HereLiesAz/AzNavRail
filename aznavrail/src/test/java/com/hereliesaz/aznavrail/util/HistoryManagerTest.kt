package com.hereliesaz.aznavrail.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HistoryManagerTest {

    private lateinit var context: Context
    private lateinit var historyFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        historyFile = File(context.filesDir, "az_text_box_history.txt")
        HistoryManager.init(context, 5) // Default to 5
    }

    @After
    fun teardown() {
        if (historyFile.exists()) {
            historyFile.delete()
        }
        HistoryManager.resetForTesting()
    }

    @Test
    fun addEntry_addsNewEntryToTopOfHistory() {
        HistoryManager.addEntry("first", null)
        HistoryManager.addEntry("second", null)
        val suggestions = HistoryManager.getSuggestions("", null)
        assertEquals("second", suggestions.first())
    }

    @Test
    fun addEntry_movesExistingEntryToTop() {
        HistoryManager.addEntry("first", null)
        HistoryManager.addEntry("second", null)
        HistoryManager.addEntry("first", null)
        val suggestions = HistoryManager.getSuggestions("", null)
        assertEquals("first", suggestions.first())
        assertEquals(2, suggestions.size)
    }

    @Test
    fun getSuggestions_returnsMatchingEntries() {
        HistoryManager.addEntry("apple", null)
        HistoryManager.addEntry("apricot", null)
        HistoryManager.addEntry("banana", null)
        val suggestions = HistoryManager.getSuggestions("ap", null)
        assertEquals(2, suggestions.size)
        assertTrue(suggestions.contains("apple"))
        assertTrue(suggestions.contains("apricot"))
    }

    @Test
    fun getSuggestions_respectsMaxSuggestionSetting() {
        HistoryManager.updateSettings(2)
        (1..5).forEach { HistoryManager.addEntry("entry$it", null) }
        val suggestions = HistoryManager.getSuggestions("entry", null)
        assertEquals(2, suggestions.size)
    }

    @Test
    fun storageLimit_isRespected() {
        HistoryManager.updateSettings(1) // 1KB limit
        val longString = "a".repeat(1000)

        // Add a small entry first
        HistoryManager.addEntry("small entry", null)
        // Now add the large entry, which will be at the top of the list
        HistoryManager.addEntry(longString, null)

        // `saveHistory` writes from the top of the list.
        // It should write `longString`, then find there's no space for "small entry".

        // Reload from file to see what was actually saved
        HistoryManager.init(context, 1) // Re-init to force a read from disk
        val suggestions = HistoryManager.getSuggestions("", null)

        assertEquals("The history should only contain the single large entry", 1, suggestions.size)
        assertEquals(longString, suggestions.first())
    }

    @Test
    fun zeroLimit_disablesHistory() {
        HistoryManager.updateSettings(0) // 0KB limit, 0 suggestions
        HistoryManager.addEntry("should not be saved", null)
        val suggestions = HistoryManager.getSuggestions("should", null)
        assertTrue(suggestions.isEmpty())

        // Verify file is empty or non-existent
        assertTrue(!historyFile.exists() || historyFile.length() == 0L)
    }

    @Test
    fun namespacedHistory_isolatesSuggestions() {
        // Add entries to two different contexts
        HistoryManager.addEntry("user_search_1", "users")
        HistoryManager.addEntry("user_search_2", "users")
        HistoryManager.addEntry("product_search_1", "products")

        // Get suggestions for 'users' context
        val userSuggestions = HistoryManager.getSuggestions("user", "users")
        assertEquals(2, userSuggestions.size)
        assertTrue(userSuggestions.contains("user_search_1"))
        assertTrue(userSuggestions.contains("user_search_2"))

        // Get suggestions for 'products' context
        val productSuggestions = HistoryManager.getSuggestions("product", "products")
        assertEquals(1, productSuggestions.size)
        assertTrue(productSuggestions.contains("product_search_1"))

        // Get suggestions for default (null) context
        val defaultSuggestions = HistoryManager.getSuggestions("", null)
        assertTrue(defaultSuggestions.isEmpty())
    }

    @Test
    fun namespacedHistory_movesExistingEntryToTop() {
        HistoryManager.addEntry("entry1", "contextA")
        HistoryManager.addEntry("entry2", "contextA")
        HistoryManager.addEntry("entry1", "contextB") // different context
        HistoryManager.addEntry("entry1", "contextA") // same context, should move to top

        val suggestionsA = HistoryManager.getSuggestions("", "contextA")
        assertEquals(2, suggestionsA.size)
        assertEquals("entry1", suggestionsA.first())

        val suggestionsB = HistoryManager.getSuggestions("", "contextB")
        assertEquals(1, suggestionsB.size)
        assertEquals("entry1", suggestionsB.first())
    }
}
