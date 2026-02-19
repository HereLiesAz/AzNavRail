# Migration Guide: Surviving the Automation Purge

If you are reading this, you are attempting to upgrade AzNavRail from its chaotic, manual-DSL era to the totalitarian KSP architecture. The old ways of hand-crafting your `AzHostActivityLayout` and meticulously typing `azSettings` are dead. 

Follow this sequence to avoid compilation failure and structural collapse.

## Phase 1: Eradicate the Manual Graph

You previously built your UI inside `setContent { ... }` using a sprawling DSL block. 

**Action:** Delete it. Delete all of it. The `AzHostActivityLayout`, the `AzNavHost`, the `composable` routes. KSP generates this now.

## Phase 2: Surrender the Activity

Your `MainActivity` must now bow to the generated structural authority.

**Old Way:**
~~~kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AzHostActivityLayout(...) { ... }
        }
    }
}
~~~

**The New Mandate:**
Extend `AzActivity` and declare the generated `AzGraph`. The `onCreate` logic is handled for you.

~~~kotlin
@Az(app = AzApp(dock = AzDockingSide.LEFT))
class MainActivity : AzActivity() {
    override val graph = AzGraph
}
~~~

## Phase 3: The Dissolution of `azSettings`

The bloated, omnipotent `azSettings` function has been destroyed for crimes against modularity. You must split your runtime configurations into three distinct sectors within the `configureRail()` escape hatch.

**Action:** Move your parameters to their designated functions.

1.  **`azTheme { ... }`**: Visual aesthetics (colors, shapes, widths, footer visibility).
2.  **`azConfig { ... }`**: Behavioral mechanics (docking side, haptics, packing, missing menus).
3.  **`azAdvanced { ... }`**: System-level manipulation (loading screens, help overlays, floating windows).

~~~kotlin
class MainActivity : AzActivity() {
    override val graph = AzGraph

    // The sole survivor of the DSL era.
    override fun AzNavRailScope.configureRail() {
        azTheme(
            expandedWidth = 260.dp,
            collapsedWidth = 80.dp,
            activeColor = Color.Red
        )
        
        azConfig(
            dockingSide = AzDockingSide.LEFT,
            vibrate = true
        )
        
        azAdvanced(
            infoScreen = isFirstLaunch,
            enableRailDragging = true
        )
    }
}
~~~

## Phase 4: Submit to the Synthetic Tongue

The `AzNavRailScope` has been stripped of its human conveniences. There are no longer polymorphic overloads for every conceivable combination of missing arguments.

If you are dynamically creating items that the annotations cannot reach, you must use the explicit, monolithic function signatures. Look at `DSL.md` for the exact parameters. Do not attempt to guess; the compiler will not forgive you.
