1. **Analyze Web vs. Android:**
   - Android has `AzButton.kt` and `AzNavRailButton.kt`. Both use `AzButtonShape` (CIRCLE, SQUARE, RECTANGLE, NONE).
   - In Android: `val baseFillColor = fillColor ?: computedFillColor` where `computedFillColor` is White with alpha 0.25 (if finalColor is Black) or Black with alpha 0.25 (otherwise).
   - In Android `AzNavRailButton`, the sizes are: CIRCLE/SQUARE -> `aspectRatio(1f)` with `size` (default `72.dp`), RECTANGLE/NONE -> `width(size)` and `height(40.dp)`. Wait, it says `ButtonWidth = 72.dp`.
   - Web has `AzButton.jsx` and `AzNavRailButton.jsx`, and their CSS. Web has sizes `48px` default or `64px` for nav rail button. Wait, `AzNavRailDefaults` in Android changed it to `72.dp`! Wait, no, Android `AzButton` uses `AzNavRailDefaults.ButtonWidth` (72.dp). But `AzNavRailButton` uses `64px` in Web?
   - In Web `AzNavRailButton.jsx`, width/height is hardcoded to `64px`. We need to align sizes to Android's defaults? No, the user says "make sure colors and fonts work the same way, as in where they come from, in spite of hyperlinking colors, and I need you to make sure button shape thickness and the fill (no background!) is the same."
   - Wait, "in spite of hyperlinking colors"? Hyperlinks use `currentColor` or something. In Web `AzButton.jsx`: `color = 'currentColor'`. Android uses `MaterialTheme.colorScheme.primary` by default for `color` and `activeColor`.
   - And the "fill (no background!)". Android sets `containerColor` to a computed color with 0.25 alpha if not selected, and 0.12 alpha if selected. The shape thickness in Android is `BorderStroke(3.dp, finalColor)`.
   - Web: `AzButton.css` has `border-width: 2px`. `AzNavRailButton.css` has what? Let's check.
   - React Native: `AzButton.tsx` has `borderWidth: isNone ? 0 : 2`. Color default is `#6200ee`. Size `48`.

2. **Button Shapes and Border Thickness**:
   - Android border stroke: `3.dp`.
   - Web border thickness: `3px` in CSS.
   - React Native border thickness: `3` points.

3. **Colors**:
   - Web: default `color = 'blue'` in `AzNavRailButton.jsx`. `color = 'currentColor'` in `AzButton.jsx`.
   - We should use similar defaults to Android, maybe? Wait, "where they come from, in spite of hyperlinking colors".
   - The fill logic: "no background!" The user says "the fill (no background!) is the same".
   - Let's read Web and RN code to adjust.
