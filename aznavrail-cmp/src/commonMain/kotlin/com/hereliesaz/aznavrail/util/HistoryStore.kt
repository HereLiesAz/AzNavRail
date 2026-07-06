package com.hereliesaz.aznavrail.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory autocomplete history for [com.hereliesaz.aznavrail.AzTextBox] (CMP port).
 *
 * The Android sibling ([com.hereliesaz.aznavrail.util.HistoryManager]) persists history to per-
 * context files under `Context.filesDir`. Because commonMain has no `Context` and no cross-platform
 * file-system API in scope for this phase, the CMP port keeps the same public shape
 * (`updateSettings` / `addEntry` / `getSuggestions`) but drops persistence — entries live only for
 * the current process. A follow-up phase can add real persistence via `multiplatform-settings` or
 * Okio without changing this call surface.
 */
internal object HistoryStore {

    private const val DEFAULT_HISTORY_CONTEXT = "default"

    private var maxSuggestions = 5
    private val histories = mutableMapOf<String, MutableList<String>>()
    private val mutex = Mutex()

    /**
     * Sets the maximum suggestion count. Clamped to 0..5 to match the Android sibling.
     */
    fun updateSettings(suggestionLimit: Int) {
        maxSuggestions = suggestionLimit.coerceIn(0, 5)
    }

    /**
     * Records a submitted value at the front of the history for [historyContext].
     * No-ops when [text] is blank or the suggestion limit is 0.
     */
    fun addEntry(text: String, historyContext: String?) {
        val ctx = historyContext ?: DEFAULT_HISTORY_CONTEXT
        if (text.isBlank() || maxSuggestions == 0) return
        val list = histories.getOrPut(ctx) { mutableListOf() }
        list.remove(text)
        list.add(0, text)
    }

    /**
     * Returns a ranked list of autocomplete suggestions from history that match [query].
     * Prefix matches are returned before substring matches. Results are capped to the configured
     * suggestion limit.
     */
    suspend fun getSuggestions(query: String, historyContext: String?): List<String> {
        val ctx = historyContext ?: DEFAULT_HISTORY_CONTEXT
        if (maxSuggestions == 0) return emptyList()
        return mutex.withLock {
            val history = histories[ctx] ?: return@withLock emptyList()
            if (query.isBlank()) {
                history.take(maxSuggestions)
            } else {
                val startsWith = ArrayList<String>(maxSuggestions)
                val contains = ArrayList<String>(maxSuggestions)
                for (item in history) {
                    if (item.equals(query, ignoreCase = true)) continue
                    if (item.startsWith(query, ignoreCase = true)) {
                        if (startsWith.size < maxSuggestions) startsWith.add(item)
                    } else if (contains.size < maxSuggestions &&
                        item.contains(query, ignoreCase = true)) {
                        contains.add(item)
                    }
                    if (startsWith.size >= maxSuggestions) break
                }
                val result = ArrayList<String>(maxSuggestions)
                result.addAll(startsWith)
                val needed = maxSuggestions - result.size
                if (needed > 0) result.addAll(contains.take(needed))
                result
            }
        }
    }
}
