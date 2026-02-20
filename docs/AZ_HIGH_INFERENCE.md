# Az High-Inference KSP System

The Az High-Inference system is a ruthless compile-time code generator that eliminates the boilerplate traditionally associated with setting up `AzNavRail`. By using the `@Az` annotation and a smart KSP processor, you declare your intentions, and the machine constructs the architecture.

## Key Features

* **Zero-Talk Inference**: Automatically infers IDs and titles from your function and property names.
* **Scope Amnesia Cured**: The processor identifies if your state or action lives inside an Activity class. It automatically casts the instance and routes the execution, meaning you can annotate your private properties and functions.
* **Action & State Binding**: The processor observes the shape of your code. It binds non-composable functions as transient click actions, and mutable properties as UI state.

## The Vocabulary

The `@Az` annotation accepts the following contextual blocks:

* `app`: Global configuration, geometry, and docking rules.
* `theme`: Structural shapes and active colors.
* `advanced`: Loading states, window overlays, and drag callbacks.
* `rail` / `menu`: Standard UI screens or transient actions.
* `host`: Parent items that expand inline.
* `nested`: Popup menu structures (Vertical or Horizontal).
* `toggle` / `cycler`: State-driven interactive buttons.
* `reloc`: Draggable items with hidden action/input menus.
* `background`: Layers exiled behind the safe zones.
* `divider`: Visual breaks in the list.

## Binding Protocols: The Shape of the Function

The machine judges your functions by their shape. It determines whether a button opens a new screen or simply executes a block of code based entirely on the presence or absence of the `@Composable` annotation.

### 1. Screen Binding (The Navigable Destination)
If your goal is to open a new screen, the function **MUST** be annotated with `@Composable`. The compiler sees the visual mass and routes it into the `AzNavHost` graph.

~~~kotlin
@Az // ID: "home", Text: "Home"
@Composable
fun Home() {
    Text("Home Screen")
}
~~~

### 2. Action Binding (The Transient Execution)
**What if you don't want to open a screen?** You simply omit the `@Composable` annotation. By starving the function of visual mass, the compiler deduces it is a **transient action**. Instead of throwing it into the navigation graph, the processor wires the function directly into the button's `onClick` lambda. 

~~~kotlin
@Az(rail = RailItem(icon = R.drawable.ic_power, text = "Self Destruct"))
fun InitiatePurge() {
    System.exit(0)
}
~~~

### 3. State Binding (The Puppet)
Annotate a `var` property with a `Toggle` or `Cycler` inside your Activity. The machine reads its state to update the UI and mutates it when clicked. You must use `by mutableStateOf()`.

~~~kotlin
@Az(toggle = Toggle(toggleOnText = "LOUD", toggleOffText = "QUIET"))
var isAlertActive by mutableStateOf(false)
~~~

## Callbacks & Reflection
When passing strings for callbacks in `@Advanced` (`onRailDrag`, `onUndock`) or `@RelocItem` (`hiddenMenuActions`), the processor will automatically synthesize the lambda execution to target the matching function name in your Activity. 

~~~kotlin
// Generates: hiddenMenuActions = { instance?.NukeDatabase() }
@Az(reloc = RelocItem(hiddenMenuActions = ["NukeDatabase"]))
fun FileManager() {}

fun NukeDatabase() { db.clearAllTables() }
~~~
