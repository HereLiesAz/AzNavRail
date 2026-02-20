# Az High-Inference KSP System

The Az High-Inference system is a ruthless compile-time code generator that eliminates the boilerplate traditionally associated with setting up `AzNavRail`. By using the `@Az` annotation and a smart KSP processor, you declare your intentions, and the machine constructs the architecture.

## Key Features

* **Zero-Talk Inference**: Automatically infers IDs and titles from your function and property names.
    * `fun WifiSettings()` -> ID: `wifi_settings`, Text: "Wifi Settings"
* **Action & State Binding**: The processor observes the shape of your code. It binds non-composable functions as transient click actions, and mutable properties as UI state.
* **Unified Annotation**: A single `@Az` annotation handles Apps, Overlays, Items, Hosts, Toggles, Cyclers, and Nested Rails.
* **Base Class Automation**: `AzActivity` handles all the `onCreate`, `setContent`, and `AzHostActivityLayout` wiring for you.

## The Vocabulary

The `@Az` annotation accepts the following contextual blocks:

* `app = App(...)`: Global configuration, geometry, and docking rules.
* `advanced = Advanced(...)`: Loading states, window overlays, and drag callbacks.
* `rail = RailItem(...)`: Standard UI screens or transient actions.
* `menu = MenuItem(...)`: Items relegated to the footer drawer.
* `host = RailHost(...)`: Parent items that expand inline.
* `nested = NestedRail(...)`: Popup menu structures.
* `toggle = Toggle(...)`: State-driven ON/OFF buttons.
* `cycler = Cycler(...)`: State-driven multi-option rotators.
* `reloc = RelocItem(...)`: Draggable items with hidden menus.
* `background = Background(...)`: Base layers beneath the safe zones.
* `divider = Divider(...)`: Visual breaks in the list.

## Binding Protocols: The Shape of the Function

The machine judges your functions by their shape. It determines whether a button opens a new screen or simply executes a block of code based entirely on the presence or absence of the `@Composable` annotation.

### 1. Screen Binding (The Navigable Destination)
If your goal is to open a new screen, the function **MUST** be annotated with `@Composable`. The compiler sees the visual mass and routes it into the `AzNavHost` graph, assigning it a destination ID.

~~~kotlin
@Az // ID: "home", Text: "Home"
@Composable
fun Home() {
    Text("Home Screen")
}
~~~

### 2. Action Binding (The Transient Execution)
**What if you don't want to open a screen?** What if you just want a button that triggers a process (e.g., logging out, clearing a cache, initiating a sync)?

You simply omit the `@Composable` annotation. By starving the function of visual mass, the compiler deduces it is a **transient action**. Instead of throwing it into the navigation graph, the processor wires the function directly into the button's `onClick` lambda. 

When the user clicks the item, your function executes, the rail collapses instantly, and absolutely no navigation occurs.

~~~kotlin
@Az(rail = RailItem(icon = R.drawable.ic_power, text = "Self Destruct"))
fun InitiatePurge() {
    System.exit(0)
}
~~~

### 3. State Binding (The Puppet)
Annotate a `var` property with a `Toggle` or `Cycler`. The machine reads its state to update the UI and mutates it when clicked. You must use `by mutableStateOf()`.

~~~kotlin
@Az(toggle = Toggle(toggleOnText = "LOUD", toggleOffText = "QUIET"))
var isAlertActive by mutableStateOf(false)
~~~

## Inference Rules

| Code Symbol | Inferred ID | Inferred Text |
| :--- | :--- | :--- |
| `fun UserProfile()` | `user_profile` | "User Profile" |
| `fun FAQ()` | `f_a_q` (Standard Snake) | "F A Q" |
| `var SystemVolume` | `system_volume` | "System Volume" |

## Validation

The processor enforces "The Tax":
* Any function annotated as a screen (`RailItem`, `MenuItem`, `NestedRail`) that you *intend* to be navigable **MUST** have `@Composable`. If you forget it, the system will treat your screen as a silent, invisible action.
* Sub-items must declare a valid, existing `parent` ID string.
