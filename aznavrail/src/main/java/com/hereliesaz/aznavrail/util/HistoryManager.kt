package com.hereliesaz.aznavrail.util

import android.content.Context
import java.io.File
import java.io.IOException

object HistoryManager {

    private const val HISTORY_FILE_NAME = "az_text_box_history.txt"
    private var maxSizeBytes = 5 * 1024
    private var maxSuggestions = 5

    private var isInitialized = false
    private lateinit var historyFile: File
    private val history = mutableListOf<String>()

    fun init(context: Context, suggestionLimit: Int = 5) {
        if (isInitialized) {
            updateSettings(suggestionLimit) // Allow updating settings on re-init
            return
        }
        historyFile = File(context.applicationContext.filesDir, HISTORY_FILE_NAME)
        updateSettings(suggestionLimit)
        if (historyFile.exists()) {
            loadHistory()
        }
        isInitialized = true
    }

    fun updateSettings(suggestionLimit: Int) {
        val newLimit = suggestionLimit.coerceIn(0, 5)
        maxSuggestions = newLimit
        maxSizeBytes = newLimit * 1024
        // If storage is reduced, we might need to trim the history file
        if (isInitialized) {
            saveHistory()
        }
    }

    private fun loadHistory() {
        try {
            val lines = historyFile.readLines(Charsets.UTF_8)
            synchronized(history) {
                history.clear()
                history.addAll(lines)
            }
        } catch (e: IOException) {
            // Silently ignore, no history will be loaded.
        }
    }

    private fun saveHistory() {
        try {
            historyFile.writer(Charsets.UTF_8).use { writer ->
                var currentSize = 0
                val entriesToWrite: List<String>
                synchronized(history) {
                    entriesToWrite = history.toList() // Create a snapshot
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
            loadHistory()
        } catch (e: IOException) {
            // Silently ignore, history not saved.
        }
    }


    fun addEntry(text: String) {
        if (!isInitialized || text.isBlank() || maxSizeBytes == 0) return

        synchronized(history) {
            history.remove(text)
            history.add(0, text)
        }
        saveHistory()
    }

    fun getSuggestions(query: String): List<String> {
        if (!isInitialized || maxSuggestions == 0) {
            return emptyList()
        }
        synchronized(history) {
            return if (query.isBlank()) {
                history.take(maxSuggestions)
            } else {
                history
                    .filter { it.startsWith(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }
                    .take(maxSuggestions)
            }
        }
    }

    internal fun resetForTesting() {
        synchronized(history) {
            history.clear()
        }
        isInitialized = false
    }
}
