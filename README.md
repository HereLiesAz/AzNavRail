# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

AzNavRail is a highly customizable and opinionated navigation rail component for Jetpack Compose, designed with a streamlined, DSL-style API. It provides a powerful yet easy-to-use solution for app navigation, ensuring a consistent and polished user experience. A web version is also available for React.

This "navigrenuail" provides a vertical navigation rail that expands to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Core Principles

- **Convention over Configuration**: AzNavRail is intentionally opinionated to ensure a consistent and predictable user experience. While it offers a high degree of customization, it provides sensible defaults that work out-of-the-box.
- **Developer Experience**: The DSL-style API is designed to be intuitive and easy to use, allowing you to build complex navigation structures with minimal code.
- **"Batteries-Included"**: AzNavRail provides a comprehensive set of features out-of-the-box, including a loading state, disabled states, and a variety of item types.

## Features

- **Responsive Layout**: Automatically adjusts to orientation changes.
- **Scrollable Content**: Both the rail and the expanded menu are scrollable.
- **Text-Only DSL API**: A simple, declarative API for building your navigation.
- **Multi-line Menu Items**: Supports multi-line text with automatic indentation.
- **Stateless and Observable**: Hoist and manage state in your own composables.
-   **Customizable Shapes**: Choose from `CIRCLE`, `SQUARE`, `RECTANGLE`, or `NONE` for rail buttons.
-   **Uniform Rectangle Width**: All rectangular buttons automatically share the same width, determined by the widest rectangle.
- **Smart Collapse Behavior**: Menu items intelligently collapse the rail after interactions.
- **Delayed Cycler Action**: Cycler items have a built-in delay to prevent accidental triggers.
- **Customizable Colors**: Apply custom colors to individual rail buttons.
- **Dividers**: Easily add dividers to your menu.
- **Automatic Header**: Displays your app's icon or name by default.
- **Configurable Layout**: Pack buttons together or preserve spacing.
- **Disabled State**: Disable any item, including individual cycler options.
- **Loading State**: Show a loading animation over the rail.
- **Standalone Components**: Use `AzButton`, `AzToggle`, `AzCycler`, and `AzDivider` on their own.

## AzNavRail for Android (Jetpack Compose)

### Setup

To use this library, add JitPack to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

And add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {

    implementation("com.github.HereLiesAz:AzNavRail:3.14") // Or the latest version
}
```

### Usage

Here is a comprehensive example demonstrating the various features of `AzNavRail`. You can find this code in the `SampleApp`.

```kotlin
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.model.AzButtonShape

@Composable
fun SampleScreen() {
    var isOnline by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val cycleOptions = remember { listOf("A", "B", "C", "D") }
    var selectedOption by remember { mutableStateOf(cycleOptions.first()) }
    val context = LocalContext.current

    Row {
        AzNavRail {
            azSettings(
                displayAppNameInHeader = true,
                packRailButtons = false,
                isLoading = isLoading,
                defaultShape = AzButtonShape.RECTANGLE // Set a default shape for all rail items
            )

            // A standard menu item
            azMenuItem(id = "home", text = "Home", onClick = { /* ... */ })

            // A rail item with the default shape (RECTANGLE)
            azRailItem(id = "favorites", text = "Favorites", onClick = { /* ... */ })

            // A disabled rail item that overrides the default shape
            azRailItem(
                id = "profile",
                text = "Profile",
                shape = AzButtonShape.CIRCLE,
                disabled = true,
                onClick = { /* This will not be triggered */ }
            )

            azDivider()

            // A toggle item with the SQUARE shape
            azRailToggle(
                id = "online",
                isChecked = isOnline,
                toggleOnText = "Online",
                toggleOffText = "Offline",
                shape = AzButtonShape.SQUARE,
                onClick = { isOnline = !isOnline }
            )

            // A cycler with a disabled option
            azRailCycler(
                id = "cycler",
                options = cycleOptions,
                selectedOption = selectedOption,
                disabledOptions = listOf("C"),
                onClick = {
                    val currentIndex = cycleOptions.indexOf(selectedOption)
                    selectedOption = cycleOptions[(currentIndex + 1) % cycleOptions.size]
                }
            )

            // A button to demonstrate the loading state
            azRailItem(id = "loading", text = "Load", onClick = { isLoading = !isLoading })
        }

        // Your app's main content goes here
        Column(modifier = Modifier.padding(16.dp)) {
            Text("isOnline: $isOnline")
            Text("selectedOption: $selectedOption")
            Text("isLoading: $isLoading")
        }
    }
}
```

### API Reference

#### `AzNavRail`

The main composable for the navigation rail.

```kotlin
@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavRailScope.() -> Unit
)
```

-   **`modifier`**: The modifier to be applied to the navigation rail.
-   **`initiallyExpanded`**: Whether the navigation rail is expanded by default.
-   **`disableSwipeToOpen`**: Whether to disable the swipe-to-open gesture.
-   **`content`**: The DSL content for the navigation rail.

#### `AzNavRailScope`

The DSL for configuring the `AzNavRail`.

**Note:** Functions prefixed with `azMenu` will only appear in the expanded menu view. Functions prefixed with `azRail` will appear on the collapsed rail, and their text will be used as the label in the expanded menu.

-   `azSettings(displayAppNameInHeader: Boolean, packRailButtons: Boolean, expandedRailWidth: Dp, collapsedRailWidth: Dp, showFooter: Boolean, isLoading: Boolean, defaultShape: AzButtonShape)`: Configures the settings for the `AzNavRail`.
-   `azMenuItem(id: String, text: String, disabled: Boolean, onClick: () -> Unit)`: Adds a menu item that only appears in the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text with the `\n` character.
-   `azRailItem(id: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, onClick: () -> Unit)`: Adds a rail item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text in the expanded menu.
-   `azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean, onClick: () -> Unit)`: Adds a toggle item that only appears in the expanded menu. Tapping it executes the action and collapses the rail.
-   `azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean, onClick: () -> Unit)`: Adds a toggle item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail.
-   `azMenuCycler(id: String, options: List<String>, selectedOption: String, disabled: Boolean, disabledOptions: List<String>?, onClick: () -> Unit)`: Adds a cycler item that only appears in the expanded menu. Tapping it cycles through options and executes the `onClick` action for the final selection after a 1-second delay, then collapses the rail.
-   `azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, onClick: () -> Unit)`: Adds a cycler item that appears in both the collapsed rail and the expanded menu. The behavior is the same as `azMenuCycler`.
-   `azDivider()`: Adds a horizontal divider to the expanded menu.
-   `azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a toggle item that only appears in the expanded menu. Tapping it executes the action and collapses the rail.
-   `azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a toggle item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail.
-   `azMenuCycler(id: String, options: List<String>, selectedOption: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit)`: Adds a cycler item that only appears in the expanded menu. Tapping it cycles through options and executes the `onClick` action for the final selection after a 1-second delay, then collapses the rail.
-   `azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit)`: Adds a cycler item that appears in both the collapsed rail and the expanded menu. The behavior is the same as `azMenuCycler`.
-   `azDivider()`: Adds a horizontal divider to the expanded menu.
-   **`screenTitle`**: An optional parameter for all `azMenuItem`, `azRailItem`, `azMenuToggle`, `azRailToggle`, `azMenuCycler`, and `azRailCycler` functions that displays a title on the screen when the item is selected. The title will be **force-aligned to the right** of the screen. If no `screenTitle` is provided, the item's `text` will be used instead. To prevent a title from being displayed, use `screenTitle = AzNavRail.noTitle`.

## AzNavRail for Web (React)

`aznavrail-web` is a React component that provides a Material Design-style navigation rail, inspired by its counterpart in the Android ecosystem. It is designed to be a "batteries-included" solution, offering a highly configurable and easy-to-use navigation component for web applications.

### Features

- **Expandable and Collapsible**: A compact rail that expands into a full menu drawer.
- **Configurable Items**: Supports standard, toggle, and cycler navigation items.
- **DSL-like Configuration**: Use a simple JavaScript array of objects to define the navigation content, similar to a DSL.
- **Dynamic Text Resizing**: Text inside the rail buttons automatically resizes to fit, preventing overflow.
- **Customizable**: Easily customize dimensions, colors, and behavior through a settings prop.

### Getting Started

#### Prerequisites

- [Node.js](https://nodejs.org/) (v16 or higher)
- [React](https://reactjs.org/) (v18 or higher)

#### Installation

1.  Copy the `aznavrail-web/src/components` and `aznavrail-web/src/hooks` directories into your project's source folder.
2.  Ensure you have the necessary dependencies by installing them if you haven't already:
    ```bash
    npm install react react-dom
    ```

### Usage

To use the `AzNavRail` component, import it into your application and provide it with `content` and `settings` props.

```jsx
import React, { useState } from 'react';
import AzNavRail from './components/AzNavRail';
import './App.css'; // Ensure you have styles for your layout

function App() {
  const [isPowerOn, setIsPowerOn] = useState(true);
  const [theme, setTheme] = useState('Light');

  const navItems = [
    {
      id: 'home',
      text: 'Home',
      isRailItem: true,
      onClick: () => console.log('Home clicked'),
    },
    {
      id: 'power',
      isRailItem: true,
      isToggle: true,
      isChecked: isPowerOn,
      toggleOnText: 'Power On',
      toggleOffText: 'Power Off',
      onClick: () => setIsPowerOn(!isPowerOn),
      color: 'green',
    },
    {
      id: 'theme',
      isRailItem: true,
      isCycler: true,
      options: ['Light', 'Dark', 'System'],
      selectedOption: theme,
      onClick: (selectedTheme) => setTheme(selectedTheme),
      color: 'purple',
    },
    {
      id: 'settings',
      text: 'Settings',
      onClick: () => console.log('Settings clicked'),
    },
  ];

  const railSettings = {
    appName: 'My Awesome App',
    displayAppNameInHeader: true,
  };

  return (
    <div className="App">
      <AzNavRail content={navItems} settings={railSettings} />
      <main className="main-content">
        <h1>Main Content Area</h1>
        <p>Power is currently: {isPowerOn ? 'On' : 'Off'}</p>
        <p>Selected theme is: {theme}</p>
      </main>
    </div>
  );
}

export default App;
```

### Configuration

#### `AzNavRail` Props

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `content` | `Array` | `[]` | An array of navigation item objects. |
| `settings` | `Object` | `{}` | An object for customizing the rail's appearance and behavior. |
| `initiallyExpanded` | `boolean` | `false` | Sets the initial expanded state of the rail. |
| `disableSwipeToOpen`| `boolean` | `false` | (Not yet implemented) Disables the swipe-to-open gesture. |

#### `settings` Object

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `appName` | `string` | `'App'` | The name of the app, displayed in the header. |
| `displayAppNameInHeader` | `boolean` | `false` | If `true`, shows `appName` in the header instead of an icon. |
| `expandedRailWidth` | `string` | `'260px'` | The width of the rail when expanded. |
| `collapsedRailWidth` | `string` | `'80px'` | The width of the rail when collapsed. |
| `showFooter` | `boolean` | `true` | Whether to display the footer section in the expanded menu. |
| `isLoading` | `boolean` | `false` | If `true`, shows a loading indicator instead of the navigation content. |
| `packRailButtons` | `boolean` | `false` | If `true`, packs the rail buttons at the top instead of spacing them out.|

#### Navigation Item Object (`content` array)

Each object in the `content` array defines a navigation item.

| Key | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | `string` | Yes | A unique identifier for the item. |
| `onClick` | `function`| Yes | The callback function executed on click. |
| `isRailItem` | `boolean` | No | If `true`, the item appears in the collapsed rail as well as the expanded menu.|
| `text` | `string` | Yes | The text for a standard menu item. Supports `\n` for multi-line text. |
| `isToggle` | `boolean` | No | Set to `true` for a toggle item. |
| `isChecked` | `boolean` | No | The current state of a toggle item. Required if `isToggle` is `true`. |
| `toggleOnText` | `string` | No | Text for the "on" state. Required if `isToggle` is `true`. |
| `toggleOffText` | `string` | No | Text for the "off" state. Required if `isToggle` is `true`. |
| `isCycler` | `boolean` | No | Set to `true` for a cycler item. |
| `options` | `Array` | No | An array of strings for the cycler options. Required if `isCycler` is `true`. |
| `selectedOption`| `string` | No | The currently selected option. Required if `isCycler` is `true`. |
| `color` | `string` | No | The border color for a rail button (e.g., `'red'`, `'#FF5733'`). |
| `screenTitle` | `string` | No | The title to be displayed on the screen when this item is selected. This title will be **force-aligned to the right**. If no `screenTitle` is provided, the item's `text` will be used instead. To prevent a title from being displayed, pass `"noTitle"`. |

## Project Structure

This project is a monorepo containing the following packages:

-   `aznavrail`: The core Android library.
-   `SampleApp`: A sample Android application that demonstrates how to use the `aznavrail` library.
-   `aznavrail-web`: The web version of the `aznavrail` library.

## Contributing

Contributions are welcome! Please feel free to submit a pull request or open an issue.

## License

```
Copyright 2024 The AzNavRail Authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
