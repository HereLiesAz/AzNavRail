# AzNavRail for Web

`aznavrail-web` is a React component that provides a Material Design-style navigation rail, inspired by its counterpart in the Android ecosystem. It is designed to be a "batteries-included" solution, offering a highly configurable and easy-to-use navigation component for web applications.

## Features

- **Expandable and Collapsible:** A compact rail that expands into a full menu drawer.
- **Configurable Items:** Supports standard, toggle, and cycler navigation items.
- **DSL-like Configuration:** Use a simple JavaScript array of objects to define the navigation content, similar to a DSL.
- **Dynamic Text Resizing:** Text inside the rail buttons automatically resizes to fit, preventing overflow.
- **Customizable:** Easily customize dimensions, colors, and behavior through a settings prop.

## Getting Started

### Prerequisites

- [Node.js](https://nodejs.org/) (v16 or higher)
- [React](https://reactjs.org/) (v18 or higher)

### Installation

1.  Copy the `aznavrail-web/src/components` and `aznavrail-web/src/hooks` directories into your project's source folder.
2.  Ensure you have the necessary dependencies by installing them if you haven't already:
    ```bash
    npm install react react-dom
    ```

## Usage

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

## Configuration

### `AzNavRail` Props

| Prop                | Type      | Default           | Description                                                                 |
| ------------------- | --------- | ----------------- | --------------------------------------------------------------------------- |
| `content`           | `Array`   | `[]`              | An array of navigation item objects.                                        |
| `settings`          | `Object`  | `{}`              | An object for customizing the rail's appearance and behavior.             |
| `initiallyExpanded` | `boolean` | `false`           | Sets the initial expanded state of the rail.                                |
| `disableSwipeToOpen`| `boolean` | `false`           | (Not yet implemented) Disables the swipe-to-open gesture.                   |

### `settings` Object

| Key                      | Type      | Default    | Description                                                              |
| ------------------------ | --------- | ---------- | ------------------------------------------------------------------------ |
| `appName`                | `string`  | `'App'`    | The name of the app, displayed in the header.                            |
| `displayAppNameInHeader` | `boolean` | `false`    | If `true`, shows `appName` in the header instead of an icon.             |
| `expandedRailWidth`      | `string`  | `'260px'`  | The width of the rail when expanded.                                     |
| `collapsedRailWidth`     | `string`  | `'80px'`   | The width of the rail when collapsed.                                    |
| `showFooter`             | `boolean` | `true`     | Whether to display the footer section in the expanded menu.              |
| `isLoading`              | `boolean` | `false`    | If `true`, shows a loading indicator instead of the navigation content.  |
| `packRailButtons`        | `boolean` | `false`    | If `true`, packs the rail buttons at the top instead of spacing them out.|

### Navigation Item Object (`content` array)

Each object in the `content` array defines a navigation item.

| Key             | Type      | Required | Description                                                                    |
| --------------- | --------- | -------- | ------------------------------------------------------------------------------ |
| `id`            | `string`  | Yes      | A unique identifier for the item.                                              |
| `onClick`       | `function`| Yes      | The callback function executed on click.                                       |
| `isRailItem`    | `boolean` | No       | If `true`, the item appears in the collapsed rail as well as the expanded menu.|
| `text`          | `string`  | Yes      | The text for a standard menu item. Supports `\n` for multi-line text.          |
| `isToggle`      | `boolean` | No       | Set to `true` for a toggle item.                                               |
| `isChecked`     | `boolean` | No       | The current state of a toggle item. Required if `isToggle` is `true`.          |
| `toggleOnText`  | `string`  | No       | Text for the "on" state. Required if `isToggle` is `true`.                     |
| `toggleOffText` | `string`  | No       | Text for the "off" state. Required if `isToggle` is `true`.                    |
| `isCycler`      | `boolean` | No       | Set to `true` for a cycler item.                                               |
| `options`       | `Array`   | No       | An array of strings for the cycler options. Required if `isCycler` is `true`.  |
| `selectedOption`| `string`  | No       | The currently selected option. Required if `isCycler` is `true`.               |
| `color`         | `string`  | No       | The border color for a rail button (e.g., `'red'`, `'#FF5733'`).                |