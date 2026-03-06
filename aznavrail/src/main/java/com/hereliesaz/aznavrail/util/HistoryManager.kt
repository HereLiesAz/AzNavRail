package com.hereliesaz.aznavrail.util

import android.content.Context
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object HistoryManager {

    private const val HISTORY_FILE_PREFIX = "az_text_box_history_"
    private const val DEFAULT_HISTORY_CONTEXT = "default"
    private var maxSizeBytes = 5 * 1024
    private var maxSuggestions = 5

    private var isInitialized = false
    private var context: Context? = null
    private val histories = mutableMapOf<String, MutableList<String>>()
    internal var coroutineScope = CoroutineScope(Dispatchers.IO)
    private val fileMutex = Mutex()

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
            histories.keys.forEach { context ->
                coroutineScope.launch {
                    saveHistory(context)
                }
            }
        }
    }

    private fun getHistoryFile(historyContext: String): File? {
        return context?.let { File(it.filesDir, "$HISTORY_FILE_PREFIX$historyContext.txt") }
    }

    private suspend fun loadHistory(historyContext: String) = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val historyFile = getHistoryFile(historyContext) ?: return@withLock
            if (!historyFile.exists()) return@withLock

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
    }

    private suspend fun saveHistory(historyContext: String) = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val historyFile = getHistoryFile(historyContext) ?: return@withLock
            try {
                historyFile.writer(Charsets.UTF_8).use { writer ->
                    var currentSize = 0
                    val entriesToWrite: List<String>
                    synchronized(histories) {
                        entriesToWrite = histories[historyContext]?.toList() ?: emptyList()
                    }

                    val lineSeparator = System.lineSeparator()
                    val lineSeparatorSize = lineSeparator.toByteArray(Charsets.UTF_8).size
                    for (entry in entriesToWrite) {
                        if (maxSizeBytes == 0) break // Do not save if limit is 0KB
                        val entrySize = entry.toByteArray(Charsets.UTF_8).size + lineSeparatorSize
                        if (currentSize + entrySize <= maxSizeBytes) {
                            writer.write(entry)
                            writer.write(lineSeparator)
                            currentSize += entrySize
                        } else {
                            break
                        }
                    }
                }
            } catch (e: IOException) {
                // Silently ignore, history not saved.
            }
        }
    }

    fun addEntry(text: String, historyContext: String?) {
        val safeContext = historyContext ?: DEFAULT_HISTORY_CONTEXT
        if (!isInitialized || text.isBlank() || maxSizeBytes == 0) return

        synchronized(histories) {
            val history = histories.getOrPut(safeContext) { mutableListOf() }
            history.remove(text)
            history.add(0, text)
        }
        coroutineScope.launch {
            saveHistory(safeContext)
        }
    }

    suspend fun getSuggestions(query: String, historyContext: String?): List<String> {
        val safeContext = historyContext ?: DEFAULT_HISTORY_CONTEXT
        if (!isInitialized || maxSuggestions == 0) {
            return emptyList()
        }

        // Lazy load history if not already in memory for this context
        val needsLoad = synchronized(histories) { !histories.containsKey(safeContext) }
        if (needsLoad) {
            loadHistory(safeContext)
        }

        return synchronized(histories) {
            val history = histories[safeContext] ?: return@synchronized emptyList()

            if (query.isBlank()) {
                history.take(maxSuggestions)
            } else {
                val startsWith = ArrayList<String>(maxSuggestions)
                val contains = ArrayList<String>(maxSuggestions)

                for (item in history) {
                    if (item.equals(query, ignoreCase = true)) continue

                    if (item.startsWith(query, ignoreCase = true)) {
                        if (startsWith.size < maxSuggestions) {
                            startsWith.add(item)
                        }
                    } else if (contains.size < maxSuggestions && item.contains(query, ignoreCase = true)) {
                        contains.add(item)
                    }

                    if (startsWith.size >= maxSuggestions) {
                        break
                    }
                }

                val result = ArrayList<String>(maxSuggestions)
                result.addAll(startsWith)
                val needed = maxSuggestions - result.size
                if (needed > 0) {
                    result.addAll(contains.take(needed))
                }
                result
            }
        }
    }

    internal fun resetForTesting() {
        synchronized(histories) {
            histories.clear()
        }
        isInitialized = false
        context = null
    }
}
