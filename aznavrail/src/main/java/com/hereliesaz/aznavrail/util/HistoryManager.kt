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

/**
 * Manages per-context autocomplete history for [com.hereliesaz.aznavrail.AzTextBox].
 *
 * History is persisted to private files named `az_text_box_history_<context>.txt`. Each context
 * is an independent namespace so different text boxes do not share suggestions. The history is
 * bounded by [maxSizeBytes] (derived from the suggestion limit) to prevent unbounded disk use.
 *
 * Must be initialised via [init] before [addEntry] or [getSuggestions] are called. Calling [init]
 * again only updates the suggestion limit rather than re-loading history.
 */
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

    /**
     * Initialises the manager with the application context and suggestion limit.
     *
     * Safe to call multiple times — after the first call only [updateSettings] is invoked.
     *
     * @param context Application context (the application context is extracted internally).
     * @param suggestionLimit Maximum suggestions to show, clamped to 0–5.
     */
    fun init(context: Context, suggestionLimit: Int = 5) {
        if (isInitialized) {
            updateSettings(suggestionLimit) // Allow updating settings on re-init
            return
        }
        this.context = context.applicationContext
        updateSettings(suggestionLimit)
        isInitialized = true
    }

    /**
     * Updates the suggestion limit and rewrites persisted history files to respect the new size cap.
     *
     * @param suggestionLimit Maximum number of suggestions, clamped to 0–5.
     */
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
                val newHistory = historyFile.useLines(Charsets.UTF_8) { it.toMutableList() }
                synchronized(histories) {
                    histories[historyContext] = newHistory
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

    /**
     * Records a submitted value at the front of the history for [historyContext].
     *
     * No-ops if the manager is not initialised, [text] is blank, or the suggestion limit is 0.
     * Duplicate entries are moved to the front rather than duplicated.
     *
     * @param text The value to record.
     * @param historyContext Namespace key; falls back to "default" when null.
     */
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

    /**
     * Returns a ranked list of autocomplete suggestions from history that match [query].
     *
     * Prefix matches are returned before substring matches. Results are capped to the configured
     * suggestion limit. History is lazy-loaded from disk on first access per context.
     *
     * @param query The current input text to match against.
     * @param historyContext Namespace key; falls back to "default" when null.
     * @return Ordered list of matching suggestions (empty if not initialised or limit is 0).
     */
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
