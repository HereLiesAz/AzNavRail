So for sizes:
Android uses: `size` = 72.dp
React Native `AzButton.tsx`: size = 72
Web `AzNavRailButton.jsx`: width: 72px, height: 72px / height: 40px

Wait, what about text colors and fill colors?
The user says: "make sure colors and fonts work the same way, as in where they come from, in spite of hyperlinking colors, and I need you to make sure button shape thickness and the fill (no background!) is the same."
Android's `AzNavRailButton.kt`:
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

    val finalTextColor = textColor ?: finalColor
```

In Web `AzButton.jsx` and `AzNavRailButton.jsx`:
Text color is set to `color` property (which is border color).
Fill color is set to `fillColor || computedFillColor` with 0.25 opacity.

But `AzNavRailButton.jsx` text color:
Currently `.button-text { color: #000; }`.
This is NOT right. It should use `color` or `textColor` passed in, "where they come from, in spite of hyperlinking colors" probably means if a button is inside an `a` tag or something, the `button-text` class might be getting inherited link colors or something, or we should explicitly set the text color to `textColor || color` on the text element itself.

Also, RN `AzButton.tsx`:
```typescript
  const lowercaseColor = color.toLowerCase();
  const defaultFillColor = (lowercaseColor === 'black' || lowercaseColor === '#000000' || lowercaseColor === '#000') ? 'rgba(255, 255, 255, 0.25)' : 'rgba(0, 0, 0, 0.25)';

  const actualFillColor = fillColor || defaultFillColor;
  ...
  const textStyle: TextStyle = {
    color: color, // Here it should probably use textColor if provided, but AzButtonProps doesn't have textColor in RN right now. Let's add it.
  ...
```

So changes required:
1. Update Web `AzNavRailButton.jsx`, `AzButton.jsx` sizes (72x72 for circle/square, 72x40 for rectangle/none).
2. Update Web CSS to use 3px border.
3. Update Web `AzNavRailButton.jsx` to correctly apply `textColor || color` to the text span to override any external hyperlink colors. The prompt says "where they come from, in spite of hyperlinking colors", meaning `color: inherit` might be happening. Wait, in Android `AzButton.kt` there is `textColor`. We need to use `textColor` if it's passed.
4. Update RN `AzButton.tsx` and `RailMenuItem.tsx` sizes to 72/40. Border width to 3. Add `textColor` prop to `AzButton.tsx` and apply it to `Text` component.
