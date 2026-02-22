# Az High-Inference KSP System (v7.25)

The Az High-Inference system is a ruthless compile-time code generator that eliminates the boilerplate traditionally associated with setting up `AzNavRail`. By using the `@Az` annotation and a smart KSP processor, you declare your intentions, and the machine constructs the architecture.

**v7.25 Update: Live Dictatorship.** The system now supports dynamic reactive binding. Annotations can link to Activity properties by name, allowing the rail to update in real-time as your state changes.

## Key Features

* **Live Dictatorship**: Bind visibility, labels, and badges to `mutableStateOf` properties in your Activity.
* **Zero-Talk Inference**: Automatically infers IDs and titles from your function and property names.
    * `fun WifiSettings()` -> ID: `wifi_settings`, Text: "Wifi Settings"
* **Action & State Binding**: The processor observes the shape of your code. It binds non-composable functions as transient click actions, and mutable properties as UI state.
* **Base Class Automation**: `AzActivity` handles all the `onCreate`, `setContent`, and `AzHostActivityLayout` wiring for you.

## The Vocabulary

The `@Az` annotation accepts the following contextual blocks. 
*Note: In v7.25, Enum-like parameters (e.g., `dock`, `alignment`) are passed as **String literals** to prevent cyclic dependency issues in the annotation module.*

* `app = App(...)`: Global configuration, geometry, and docking rules. Use `dock = "LEFT"` or `"RIGHT"`.
* `advanced = Advanced(...)`: Loading states, window overlays, and drag callbacks.
* `rail = RailItem(...)`: Standard UI screens or transient actions. Supports `visibleProperty`, `textProperty`, etc.
* `menu = MenuItem(...)`: Items relegated to the footer drawer.
* `host = RailHost(...)`: Parent items that expand inline.
* `nested = NestedRail(...)`: Popup menu structures. Use `alignment = "VERTICAL"` or `"HORIZONTAL"`.
* `toggle = Toggle(...)`: State-driven ON/OFF buttons. Requires `isCheckedProperty`.
* `cycler = Cycler(...)`: State-driven multi-option rotators. Supports `optionsProperty`.
* `reloc = RelocItem(...)`: Draggable items with hidden menus.
* `background = Background(...)`: Base layers beneath the safe zones.
* `divider = Divider(...)`: Visual breaks in the list.

## Binding Protocols: The Shape of the Function

### 1. Screen Binding (The Navigable Destination)
If your goal is to open a new screen, the function **MUST** be annotated with `@Composable`. 

~~~kotlin
@Az // ID: "home", Text: "Home"
@Composable
fun Home() {
    Text("Home Screen")
}
~~~

### 2. Action Binding (The Transient Execution)
If you omit the `@Composable` annotation, the compiler deduces it is a **transient action**. The function is wired directly to the `onClick` lambda.

~~~kotlin
@Az(rail = RailItem(icon = R.drawable.ic_power, text = "Self Destruct"))
fun InitiatePurge() {
    System.exit(0)
}
~~~

### 3. State Binding (The Puppet / Live Dictatorship)
Annotate a `var` property with a `Toggle` or `Cycler`. In v7.25, you can also bind standard `RailItem` fields to Activity state.

~~~kotlin
class MainActivity : AzActivity() {
    override val graph = AzGraph
    
    // Live State
    var isAlertActive by mutableStateOf(false)
    var userLabel by mutableStateOf("Guest")
}

@Az(rail = RailItem(textProperty = "userLabel"))
@Composable
fun Profile() { ... }

@Az(toggle = Toggle(isCheckedProperty = "isAlertActive", toggleOnText = "LOUD", toggleOffText = "QUIET"))
var isAlertActive by mutableStateOf(false)
~~~

## Inference Rules

| Code Symbol | Inferred ID | Inferred Text |
| :--- | :--- | :--- |
| `fun UserProfile()` | `user_profile` | "User Profile" |
| `fun FAQ()` | `f_a_q` | "F A Q" |
| `var SystemVolume` | `system_volume` | "System Volume" |
