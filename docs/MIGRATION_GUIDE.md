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
Extend `AzActivity` and declare the generated `AzGraph`. The `onCreate` logic is handled for you by the synthetic backend. Note that your custom functions and state properties can now live safely inside the Activity; the processor will find them.

~~~kotlin
@Az(app = App(dock = AzDockingSide.LEFT))
class MainActivity : AzActivity() {
    override val graph = AzGraph
}
~~~

## Phase 3: The Rebirth of `azTheme` as `@Theme`

The bloated, omnipotent `azSettings` function has been destroyed for crimes against modularity. We originally burned `azTheme` to the ground entirely, but it has been resurrected from the ashes as a declarative annotation.

Parameters that previously lived in `azTheme` (like `expandedWidth` and `collapsedWidth`) have been absorbed into `azConfig` and the `@App` annotation.

**Action:** Move your structural and aesthetic parameters to the top-level annotations.

~~~kotlin
@Az(
    app = App(
        dock = AzDockingSide.LEFT,
        expandedWidth = 260,
        collapsedWidth = 80
    ),
    theme = Theme(
        activeColorHex = "#FF00FF",
        defaultShape = AzButtonShape.ROUNDED_RECTANGLE
    ),
    advanced = Advanced(
        enableRailDragging = true,
        onRailDrag = "MyDragCallback" // The processor will wire this to instance.MyDragCallback()
    )
)
class MainActivity : AzActivity() {
    override val graph = AzGraph
    
    fun MyDragCallback(x: Float, y: Float) { }
}
~~~

## Phase 4: Submit to the Synthetic Tongue

The `AzNavRailScope` has been stripped of its human conveniences. There are no longer polymorphic overloads for every conceivable combination of arguments.

If you are dynamically creating items that the annotations cannot reach, you must use the explicit, monolithic function signatures. Look at `DSL.md` for the exact parameters. Do not attempt to guess; the compiler will not forgive you.
