### API Reference

#### `AzHostActivityLayout`

The mandatory top-level container for `AzNavRail` that manages the rail, safe zones, background content, and layout flipping.

```kotlin
@Composable
fun AzHostActivityLayout(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    currentDestination: String? = null,
    isLandscape: Boolean? = null,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavHostScope.() -> Unit
)
```

-   **`modifier`**: The modifier to be applied to the host container.
-   **`navController`**: The `NavHostController` used for navigation. Defaults to `rememberNavController()`.
-   **`currentDestination`**: Optional override for the current route. If null, it is derived from `navController`.
-   **`isLandscape`**: Optional override for the orientation. If null, it is derived from the screen configuration.
-   **`initiallyExpanded`**: Whether the rail should be initially expanded.
-   **`disableSwipeToOpen`**: Disables the swipe gesture to open the drawer.
-   **`content`**: The DSL content, scoped to `AzNavHostScope`.

#### `AzNavHost`

A wrapper around `androidx.navigation.compose.NavHost` designed to be used within the `onscreen` block of `AzHostActivityLayout`.

```kotlin
@Composable
fun AzNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    route: String? = null,
    enterTransition: ... = { fadeIn(...) },
    exitTransition: ... = { fadeOut(...) },
    popEnterTransition: ... = enterTransition,
    popExitTransition: ... = exitTransition,
    builder: NavGraphBuilder.() -> Unit
)
```

-   **`navController`**: The navigation controller.
-   **`startDestination`**: The route of the start destination.
-   **`builder`**: The builder for the navigation graph.
-   *(Standard NavHost parameters are supported)*.

#### `AzNavRail`

The main composable for the navigation rail. (Usually instantiated implicitly by `AzHostActivityLayout`).

```kotlin
@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    currentDestination: String? = null,
    isLandscape: Boolean = false,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavRailScope.() -> Unit
)
```

-   **`modifier`**: The modifier to be applied to the navigation rail.
-   **`navController`**: An optional `NavController` to enable integration with Jetpack Navigation.
-   **`currentDestination`**: The route of the current destination, used to highlight the active item.
-   **`isLandscape`**: A boolean to indicate if the device is in landscape mode.
-   **`initiallyExpanded`**: Whether the navigation rail is expanded by default. Useful for Bubble activities.
-   **`disableSwipeToOpen`**: Whether to disable the swipe-to-open gesture.
-   **`content`**: The DSL content for the navigation rail.

#### `AzTextBox`

A text input field with autocomplete.

```kotlin
@Composable
fun AzTextBox(
    modifier: Modifier = Modifier,
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    hint: String = "",
    outlined: Boolean = true,
    multiline: Boolean = false,
    secret: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    historyContext: String? = null,
    submitButtonContent: (@Composable () -> Unit)? = null,
    onSubmit: (String) -> Unit
)
```

-   **`modifier`**: The modifier to be applied to the text box.
-   **`value`**: The input text to be shown in the text field. Use `null` for an uncontrolled component.
-   **`onValueChange`**: The callback that is triggered when the input value updates. Use `null` for an uncontrolled component.
-   **`hint`**: The hint text to display when the input is empty.
-   **`outlined`**: Whether the text box has an outline. The submit button's outline will be the inverse of this value.
-   **`multiline`**: Enables multiline input, which expands the text box vertically.
-   **`secret`**: Masks the input for password fields and replaces the clear button with a reveal icon.
-   **`isError`**: Indicates if the input is in an error state, changing the outline and icon color to the error color.
-   **`keyboardOptions`**: Configuration for the keyboard, such as `ImeAction`.
-   **`keyboardActions`**: Actions to take when keyboard events occur, such as when the 'Enter' key is pressed.
-   **`leadingIcon`**: An optional composable to display at the start of the text box.
-   **`trailingIcon`**: An optional composable to display at the end of the text box (before the clear/submit buttons).
-   **`outlineColor`**: Sets the color for the outline, input text, and all icons.
-   **`historyContext`**: An optional string to provide a unique context for the autocomplete history. If provided, suggestions will be drawn only from entries saved with the same context.
-   **`submitButtonContent`**: A composable lambda for the content of the submit button.
-   **`onSubmit`**: A callback that is invoked when the submit button is clicked, providing the current text.

#### `AzForm`

A composable for creating a form with multiple `AzTextBox` fields.

```kotlin
@Composable
fun AzForm(
    formName: String,
    modifier: Modifier = Modifier,
    outlined: Boolean = true,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onSubmit: (Map<String, String>) -> Unit,
    submitButtonContent: @Composable () -> Unit = { Text("Submit") },
    content: AzFormScope.() -> Unit
)
```

-   **`formName`**: A unique name for the form. This name is used as the `historyContext` for all text fields within the form, ensuring that autocomplete suggestions are namespaced and relevant to this form only.
-   **`modifier`**: The modifier to be applied to the form.
-   **`outlined`**: Whether the text fields in the form have an outline. The submit button's outline will be the inverse of this value.
-   **`outlineColor`**: Sets the color for the outline, input text, and all icons for all fields in the form.
-   **`trailingIcon`**: An optional composable to display at the end of every text box in the form.
-   **`keyboardOptions`**: Default keyboard options for the form.
-   **`keyboardActions`**: Default keyboard actions for the form. By default, 'Enter' moves to the next field, and sends the form on the last field.
-   **`onSubmit`**: A callback that is invoked when the form's submit button is clicked, providing a map of the form data.
-   **`submitButtonContent`**: A composable lambda for the content of the submit button.
-   **`content`**: The DSL content for the form, where you define the `entry` fields.

#### `AzFormScope`

-   `entry(entryName: String, hint: String, multiline: Boolean, secret: Boolean, leadingIcon: @Composable (() -> Unit)?, isError: Boolean, keyboardOptions: KeyboardOptions, keyboardActions: KeyboardActions)`: Adds a text field to the form. `leadingIcon` allows adding an icon to the start of the field. `isError` sets the error state. `keyboardOptions` and `keyboardActions` allow overriding the form defaults.

#### `AzTextBoxDefaults`

An object for configuring global `AzTextBox` and `AzForm` settings.

-   `setSuggestionLimit(limit: Int)`: Sets the maximum number of autocomplete suggestions to display (0-5) and the corresponding history storage limit in kilobytes.
-   `setBackgroundColor(color: Color)`: Sets the global background color for all text boxes and forms.
-   `setBackgroundOpacity(opacity: Float)`: Sets the global background opacity for all text boxes and forms.

#### `AzButton`

A circular, text-only button with auto-sizing text.

```kotlin
@Composable
fun AzButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE
)
```

-   **`onClick`**: A lambda to be executed when the button is clicked.
-   **`text`**: The text to display on the button.
-   **`modifier`**: The modifier to be applied to the button.
-   **`color`**: The color of the button's border and text.
-   **`colors`**: The colors of the button, overriding `color` if provided.
-   **`shape`**: The shape of the button.

#### `AzToggle`

A toggle button that displays different text for its on and off states.

```kotlin
@Composable
fun AzToggle(
    isChecked: Boolean,
    onToggle: () -> Unit,
    toggleOnText: String,
    toggleOffText: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE
)
```

-   **`isChecked`**: Whether the toggle is in the "on" state.
-   **`onToggle`**: The callback to be invoked when the button is toggled.
-   **`toggleOnText`**: The text to display when the toggle is on.
-   **`toggleOffText`**: The text to display when the toggle is off.
-   **`modifier`**: The modifier to be applied to the button.
-   **`color`**: The color of the button's border and text.
-   **`colors`**: The colors of the button, overriding `color` if provided.
-   **`shape`**: The shape of the button.

#### `AzCycler`

A button that cycles through a list of options when clicked, with a delayed action.

```kotlin
@Composable
fun AzCycler(
    options: List<String>,
    selectedOption: String,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE
)
```

-   **`options`**: The list of options to cycle through.
-   **`selectedOption`**: The currently selected option from the view model.
-   **`onCycle`**: The callback to be invoked for the final selected option after a 1-second delay.
-   **`modifier`**: The modifier to be applied to the button.
-   **`color`**: The color of the button's border and text.
-   **`colors`**: The colors of the button, overriding `color` if provided.
-   **`shape`**: The shape of the button.

#### `AzNavRailScope`

For the full DSL reference, see [DSL.md](DSL.md).

## AzNavRail for Web (React)

`aznavrail-web` is a React component that provides a Material Design-style navigation rail.

### Features

All item functions are overloaded to support `route`-based navigation.

## AzNavRail for React Native

`aznavrail-react-native` provides the navigation rail for React Native applications.

### Installation

```bash
npm install aznavrail-react-native
# or
yarn add aznavrail-react-native
```

### Usage

```tsx
import { AzNavRail } from 'aznavrail-react-native';
import { AzRailItem, AzMenuItem } from 'aznavrail-react-native';

<AzNavRail>
  <AzRailItem id="home" text="Home" onClick={() => {}} />
  <AzMenuItem id="settings" text="Settings" onClick={() => {}} />
</AzNavRail>
```
