Android library changes we need to mimic in web and react native:

Android:
```kotlin
    val buttonModifier = when (shape) {
        AzButtonShape.CIRCLE, AzButtonShape.SQUARE -> modifier
            .size(size)
            .aspectRatio(1f)
        AzButtonShape.RECTANGLE, AzButtonShape.NONE -> modifier
            .width(size) // Fixed identical width
            .height(40.dp) // Decreased fixed height variant
    }
```
Default `size` in `AzNavRailDefaults` is `72.dp`.
So sizes are:
CIRCLE/SQUARE: 72x72
RECTANGLE/NONE: 72x40
Border stroke: `3.dp`.
Colors:
`color` and `activeColor` default to Primary.
Fill colors:
```kotlin
    val computedFillColor = if (finalColor == Color.Black) Color.White else Color.Black
    val computedActiveFillColor = if (activeColor == Color.Black) Color.White else Color.Black

    val baseFillColor = fillColor ?: computedFillColor
    val activeFillColor = fillColor ?: computedActiveFillColor

    val containerColor = if (isSelected && !isPressed) {
        activeFillColor.copy(alpha = 0.12f)
    } else {
        baseFillColor.copy(alpha = 0.25f)
    }
```

Web library:
`AzNavRailButton.jsx`:
```jsx
                width: '64px',
                minWidth: '64px',
                maxWidth: '64px',
                height: '64px',
                minHeight: '64px',
                maxHeight: '64px',
```
This needs to be updated to handle sizes based on shape:
if CIRCLE or SQUARE: width 72px, height 72px.
if RECTANGLE or NONE: width 72px, height 40px.
border: `3px solid` or none.

Also `AzButton.css`:
```css
.az-button-shape-circle {
  border-radius: 50%;
  width: 72px; /* Default size */
  height: 72px;
}

.az-button-shape-square {
  border-radius: 0;
  width: 72px;
  height: 72px;
}

.az-button-shape-rectangle {
  border-radius: 0;
  width: 72px;
  height: 40px; /* Fixed height for rectangle */
  padding: 0 12px;
}

.az-button-shape-none {
  border: none;
  width: 72px;
  height: 40px;
  padding: 0 12px;
}
```

Border width should be `3px` in `AzButton.css` and `AzNavRailButton.css`.

In `AzButton.jsx` and `AzNavRailButton.jsx`, the default color should be what? "make sure colors and fonts work the same way, as in where they come from, in spite of hyperlinking colors".
Currently web `AzButton.jsx` uses `color = 'currentColor'`. `AzNavRailButton.jsx` uses `color || 'blue'`. Android uses `MaterialTheme.colorScheme.primary` by default. We should probably accept the color passed in or default appropriately, but wait, the prompt says "as in where they come from, in spite of hyperlinking colors". I think it means the font colors should match the button border color (as in Android: `val finalTextColor = textColor ?: finalColor`), and the fill color should be based on `color` or `fillColor`, NOT using css background from a generic theme unless explicitly passed. In Web `AzButton.jsx`, `color` is applied to border and text: `borderColor: color, color: color`.

React Native `AzButton.tsx`:
`const size = 72;`
If CIRCLE/SQUARE: width 72, height 72.
If RECTANGLE: width 72, height 40.
If NONE: width 72, height 40.
borderWidth: 3 (instead of 2).

Let's check `RailMenuItem.tsx` in React Native to see if it uses the same sizes.
