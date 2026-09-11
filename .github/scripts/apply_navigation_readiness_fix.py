from pathlib import Path

roots = [
    Path("aznavrail/src/main/java/com/hereliesaz/aznavrail"),
    Path("aznavrail-cmp/src/commonMain/kotlin/com/hereliesaz/aznavrail"),
]

helper = '''package com.hereliesaz.aznavrail

import androidx.navigation.NavController

/**
 * Navigates immediately once this controller owns a graph, or defers the request until the graph
 * produces its first destination.
 *
 * A [NavController] can be attached to [AzHostActivityLayout] one composition before [AzNavHost]
 * installs its graph. Calling [NavController.navigate] during that window throws
 * `IllegalArgumentException: Navigation graph has not been set`. AzNavRail's own route dispatchers
 * all use this function so a cold-start timing race cannot crash the host application.
 *
 * Requests made before readiness are kept in call order and are dispatched exactly once when the
 * first destination arrives. Invalid routes still fail normally after the graph is ready; this does
 * not swallow navigation errors.
 */
fun NavController.azNavigateWhenReady(route: String) {
    if (currentBackStackEntry != null) {
        navigate(route)
        return
    }

    AzPendingNavigation.enqueue(this, route)
}

private object AzPendingNavigation {
    private val routes = mutableMapOf<NavController, MutableList<String>>()
    private val listeners = mutableMapOf<NavController, NavController.OnDestinationChangedListener>()

    fun enqueue(controller: NavController, route: String) {
        routes.getOrPut(controller) { mutableListOf() }.add(route)
        if (listeners.containsKey(controller)) return

        lateinit var listener: NavController.OnDestinationChangedListener
        listener = NavController.OnDestinationChangedListener { readyController, _, _ ->
            readyController.removeOnDestinationChangedListener(listener)
            listeners.remove(readyController)
            val pending = routes.remove(readyController).orEmpty()
            pending.forEach { readyController.navigate(it) }
        }
        listeners[controller] = listener
        controller.addOnDestinationChangedListener(listener)
    }
}
'''

for root in roots:
    (root / "NavigationReadiness.kt").write_text(helper, encoding="utf-8")

changed = []
replacement_count = 0
for root in roots:
    for path in root.rglob("*.kt"):
        if path.name == "NavigationReadiness.kt":
            continue
        text = path.read_text(encoding="utf-8")
        count = text.count("navController?.navigate(it)")
        if count == 0:
            continue

        text = text.replace(
            "navController?.navigate(it)",
            "navController?.azNavigateWhenReady(it)",
        )
        replacement_count += count

        if "/internal/" in path.as_posix():
            nav_import = "import com.hereliesaz.aznavrail.azNavigateWhenReady\n"
            if nav_import not in text:
                package_line = "package com.hereliesaz.aznavrail.internal\n"
                if package_line not in text:
                    raise SystemExit(f"Unexpected internal package declaration in {path}")
                text = text.replace(package_line, package_line + "\n" + nav_import, 1)

        path.write_text(text, encoding="utf-8")
        changed.append(path)

if replacement_count < 10:
    raise SystemExit(
        f"Expected at least 10 AzNavRail route dispatches, found {replacement_count}"
    )

remaining = []
for root in roots:
    for path in root.rglob("*.kt"):
        if "navController?.navigate(it)" in path.read_text(encoding="utf-8"):
            remaining.append(str(path))

if remaining:
    raise SystemExit("Unhandled raw route navigation remains: " + ", ".join(remaining))

print(f"Hardened {replacement_count} route dispatches across {len(changed)} files")
for path in changed:
    print(f" - {path}")
