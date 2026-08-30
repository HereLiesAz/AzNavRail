package com.hereliesaz.aznavrail.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory autocomplete history for [com.hereliesaz.aznavrail.AzTextBox] (CMP port).
 *
 * The Android sibling ([com.hereliesaz.aznavrail.util.HistoryManager]) persists history to per-
 * context files under `Context.filesDir`. Because commonMain has no `Context` and no cross-platform
 * file-system API in scope for this phase, the CMP port persists via `multiplatform-settings`
 * backed by `azCacheSettings`, maintaining the same call surface.
 */
import com.hereliesaz.aznavrail.service.azCacheSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
internal object HistoryStore {

    private const val DEFAULT_HISTORY_CONTEXT = "default"

    private var maxSuggestions = 5
    private val histories = mutableMapOf<String, MutableList<String>>()
    private val loadedContexts = mutableSetOf<String>()
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
     *
     * Suspends rather than launching its own detached job: a caller (necessarily inside some
     * `CoroutineScope`, e.g. one bound to the composition) controls where and when this actually
     * runs, so it lands on the same dispatcher queue as a subsequent [getSuggestions] call — the
     * write is guaranteed visible to a suggestions query issued right after, matching the Android
     * sibling's synchronous `HistoryManager.addEntry`. A detached `GlobalScope` launch here would
     * race an immediately-following `getSuggestions` call on an unrelated dispatcher.
     */
    suspend fun addEntry(text: String, historyContext: String?) {
        val ctx = historyContext ?: DEFAULT_HISTORY_CONTEXT
        if (text.isBlank() || maxSuggestions == 0) return
        mutex.withLock {
            ensureLoaded(ctx)
            val list = histories.getOrPut(ctx) { mutableListOf() }
            list.remove(text)
            list.add(0, text)
            azCacheSettings.putString("az_history_$ctx", Json.encodeToString(list))
        }
    }

    private fun ensureLoaded(ctx: String) {
        if (loadedContexts.add(ctx)) {
            val saved = azCacheSettings.getString("az_history_$ctx", "")
            if (saved.isNotBlank()) {
                val list = histories.getOrPut(ctx) { mutableListOf() }
                val parsed = try { Json.decodeFromString<List<String>>(saved) } catch (e: Exception) { emptyList() }
                list.clear()
                list.addAll(parsed)
            }
        }
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
            ensureLoaded(ctx)
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
