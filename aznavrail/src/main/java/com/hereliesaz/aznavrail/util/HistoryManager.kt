package com.hereliesaz.aznavrail.util

import android.content.Context
import java.io.File
import java.io.IOException

object HistoryManager {

    private const val HISTORY_FILE_PREFIX = "az_text_box_history_"
    private const val DEFAULT_HISTORY_CONTEXT = "default"
    private var maxSizeBytes = 5 * 1024
    private var maxSuggestions = 5

    private var isInitialized = false
    private lateinit var context: Context
    private val histories = mutableMapOf<String, MutableList<String>>()

    fun init(context: Context, suggestionLimit: Int = 5) {
        if (isInitialized) {
            updateSettings(suggestionLimit) // Allow updating settings on re-init
            return
        }
        this.context = context.applicationContext
        updateSettings(suggestionLimit)
        isInitialized = true
    }

    fun updateSettings(suggestionLimit: Int) {
        val newLimit = suggestionLimit.coerceIn(0, 5)
        maxSuggestions = newLimit
        maxSizeBytes = newLimit * 1024
        // If storage is reduced, we might need to trim the history file
        if (isInitialized) {
            histories.keys.forEach { saveHistory(it) }
        }
    }

    private fun getHistoryFile(historyContext: String): File {
        return File(context.filesDir, "$HISTORY_FILE_PREFIX$historyContext.txt")
    }

    private fun loadHistory(historyContext: String) {
        val historyFile = getHistoryFile(historyContext)
        if (!historyFile.exists()) return

        try {
            val lines = historyFile.readLines(Charsets.UTF_8)
            synchronized(histories) {
                histories.getOrPut(historyContext) { mutableListOf() }.apply {
                    clear()
                    addAll(lines)
                }
            }
        } catch (e: IOException) {
            // Silently ignore, no history will be loaded.
        }
    }

    private fun saveHistory(historyContext: String) {
        val historyFile = getHistoryFile(historyContext)
        try {
            historyFile.writer(Charsets.UTF_8).use { writer ->
                var currentSize = 0
                val entriesToWrite: List<String>
                synchronized(histories) {
                    entriesToWrite = histories[historyContext]?.toList() ?: emptyList()
                }

                for (entry in entriesToWrite) {
                    val entryWithNewline = entry + System.lineSeparator()
                    val entrySize = entryWithNewline.toByteArray(Charsets.UTF_8).size
                    if (maxSizeBytes == 0) break // Do not save if limit is 0KB
                    if (currentSize + entrySize <= maxSizeBytes) {
                        writer.write(entryWithNewline)
                        currentSize += entrySize
                    } else {
                        break
                    }
                }
            }
            // After saving, reload to ensure consistency and trim the in-memory list
            loadHistory(historyContext)
        } catch (e: IOException) {
            // Silently ignore, history not saved.
        }
    }

    fun addEntry(text: String, historyContext: String?) {
        val context = historyContext ?: DEFAULT_HISTORY_CONTEXT
        if (!isInitialized || text.isBlank() || maxSizeBytes == 0) return

        synchronized(histories) {
            val history = histories.getOrPut(context) { mutableListOf() }
            history.remove(text)
            history.add(0, text)
        }
        saveHistory(context)
    }

    fun getSuggestions(query: String, historyContext: String?): List<String> {
        val context = historyContext ?: DEFAULT_HISTORY_CONTEXT
        if (!isInitialized || maxSuggestions == 0) {
            return emptyList()
        }
        synchronized(histories) {
            loadHistory(context)
            val history = histories[context] ?: return emptyList()

            return if (query.isBlank()) {
                history.take(maxSuggestions)
            } else {
                val (startsWith, contains) = history
                    .filter { it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }
                    .partition { it.startsWith(query, ignoreCase = true) }

                (startsWith + contains.sorted()).take(maxSuggestions)
            }
        }
    }

    internal fun resetForTesting() {
        synchronized(histories) {
            histories.clear()
        }
        isInitialized = false
    }
}
