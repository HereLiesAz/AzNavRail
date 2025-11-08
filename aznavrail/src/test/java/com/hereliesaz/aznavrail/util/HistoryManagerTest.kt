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
        HistoryManager.addEntry("first")
        HistoryManager.addEntry("second")
        val suggestions = HistoryManager.getSuggestions("")
        assertEquals("second", suggestions.first())
    }

    @Test
    fun addEntry_movesExistingEntryToTop() {
        HistoryManager.addEntry("first")
        HistoryManager.addEntry("second")
        HistoryManager.addEntry("first")
        val suggestions = HistoryManager.getSuggestions("")
        assertEquals("first", suggestions.first())
        assertEquals(2, suggestions.size)
    }

    @Test
    fun getSuggestions_returnsMatchingEntries() {
        HistoryManager.addEntry("apple")
        HistoryManager.addEntry("apricot")
        HistoryManager.addEntry("banana")
        val suggestions = HistoryManager.getSuggestions("ap")
        assertEquals(2, suggestions.size)
        assertTrue(suggestions.contains("apple"))
        assertTrue(suggestions.contains("apricot"))
    }

    @Test
    fun getSuggestions_respectsMaxSuggestionSetting() {
        HistoryManager.updateSettings(2)
        (1..5).forEach { HistoryManager.addEntry("entry$it") }
        val suggestions = HistoryManager.getSuggestions("entry")
        assertEquals(2, suggestions.size)
    }

    @Test
    fun storageLimit_isRespected() {
        HistoryManager.updateSettings(1) // 1KB limit
        val longString = "a".repeat(1000)

        // Add a small entry first
        HistoryManager.addEntry("small entry")
        // Now add the large entry, which will be at the top of the list
        HistoryManager.addEntry(longString)

        // `saveHistory` writes from the top of the list.
        // It should write `longString`, then find there's no space for "small entry".

        // Reload from file to see what was actually saved
        HistoryManager.init(context, 1) // Re-init to force a read from disk
        val suggestions = HistoryManager.getSuggestions("")

        assertEquals("The history should only contain the single large entry", 1, suggestions.size)
        assertEquals(longString, suggestions.first())
    }

    @Test
    fun zeroLimit_disablesHistory() {
        HistoryManager.updateSettings(0) // 0KB limit, 0 suggestions
        HistoryManager.addEntry("should not be saved")
        val suggestions = HistoryManager.getSuggestions("should")
        assertTrue(suggestions.isEmpty())

        // Verify file is empty or non-existent
        assertTrue(!historyFile.exists() || historyFile.length() == 0L)
    }
}
