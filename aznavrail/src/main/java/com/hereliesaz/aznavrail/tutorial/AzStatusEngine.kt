package com.hereliesaz.aznavrail.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** Poll interval for status predicates backed by non-Compose sources (StateFlow.value / a var). */
private const val STATUS_POLL_MS = 300L

/**
 * Observes the app's full status set reactively and returns the ids currently **true**. Statuses come
 * from three unified sources:
 *  - developer predicates (`azStatus(id) { … }`), observed via `snapshotFlow` (instant for Compose
 *    snapshot state) merged with a low-rate poll (covers `StateFlow.value` / a plain `var`) — the same
 *    engine that drives `expandWhen`;
 *  - active classifiers ([activeClassifiers]) — a classifier name is a status while it is active;
 *  - built-in `az.*` ids computed by [builtins] from AzNavHost's live rail/host/route/onscreen state.
 *
 * Keyed on the stable predicate-id set so collectors aren't torn down on unrelated recompositions.
 */
@Composable
internal fun rememberActiveStatuses(
    statusPredicates: Map<String, () -> Boolean>,
    activeClassifiers: Set<String>,
    builtins: () -> Set<String>,
): State<Set<String>> {
    val customActive = remember { mutableStateMapOf<String, Boolean>() }
    // Keyed on the stable id set so collectors aren't torn down on unrelated recompositions. The
    // predicate lambdas are resolved fresh per evaluation through `currentPredicates`, so a developer
    // re-passing a new lambda for the same id is honored without restarting the running collectors.
    val currentPredicates by rememberUpdatedState(statusPredicates)
    val keysSignature = statusPredicates.keys.sorted().joinToString(",")
    LaunchedEffect(keysSignature) {
        customActive.clear()
        currentPredicates.keys.forEach { id ->
            launch {
                val evaluate = { currentPredicates[id]?.invoke() ?: false }
                merge(
                    snapshotFlow { evaluate() },
                    flow { while (true) { emit(evaluate()); delay(STATUS_POLL_MS) } },
                ).distinctUntilChanged().collect { isOn -> customActive[id] = isOn }
            }
        }
    }

    val classifiers = rememberUpdatedState(activeClassifiers)
    val builtinsFn = rememberUpdatedState(builtins)
    return remember {
        derivedStateOf {
            val out = HashSet<String>()
            out.addAll(builtinsFn.value())   // reactive: reads AzNavHost snapshot state inside
            out.addAll(classifiers.value)
            customActive.forEach { (id, on) -> if (on) out.add(id) }
            out
        }
    }
}

/**
 * Maps AzNavHost's live rail/host/route/onscreen state to the set of true built-in `az.*` statuses.
 * Pure; call it from inside the `builtins` lambda handed to [rememberActiveStatuses] so the reads stay
 * reactive. The id scheme is the status vocabulary an app inherits for free.
 */
internal fun computeBuiltinStatuses(
    railExpanded: Boolean,
    railFloating: Boolean,
    hostStates: Map<String, Boolean>,
    currentRoute: String?,
    activeItemId: String?,
    nestedRailOpenId: String?,
    helpOpen: Boolean,
    onscreenVisibleIds: Set<String> = emptySet(),
): Set<String> {
    val out = HashSet<String>()
    out.add("az.app.ready") // always-true root, so navigation auto-edges have a reachable `from`.
    if (railExpanded) out.add("az.rail.expanded") else out.add("az.rail.collapsed")
    if (railFloating) out.add("az.rail.floating")
    hostStates.forEach { (id, expanded) -> if (expanded) out.add("az.host.$id.expanded") }
    currentRoute?.let { out.add("az.screen.$it") }
    activeItemId?.let { out.add("az.item.$it.active") }
    nestedRailOpenId?.let { out.add("az.nestedRail.$it.open") }
    if (helpOpen) out.add("az.help.open")
    onscreenVisibleIds.forEach { out.add("az.onscreen.$it.visible") }
    return out
}
