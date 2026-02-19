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

## Binding Protocols

### 1. Screen Binding (The Standard)
Annotate a `@Composable` function. The machine routes it into the `AzNavHost` graph.

```kotlin
@Az // ID: "home", Text: "Home"
@Composable
fun Home() {
    Text("Home Screen")
}
