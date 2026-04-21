Web Changes:
1. `AzNavRailButton.jsx`:
- Update props to destructure `textColor` from `item`.
- Apply `color: item.textColor || effectiveColor` directly to the `span` element with `className="button-text"`.
- Update dimensions in the `button` inline styles based on `shapeClass`.
  - CIRCLE / SQUARE: `width: 72px, minWidth: 72px, maxWidth: 72px, height: 72px, minHeight: 72px, maxHeight: 72px`
  - RECTANGLE / NONE: `width: 72px, minWidth: 72px, maxWidth: 72px, height: 40px, minHeight: 40px, maxHeight: 40px`
- Border: Update `AzNavRailButton.css` border-width to `3px`. And in `AzNavRailButton.jsx` set `borderWidth: shapeClass === 'none' ? 0 : '3px'` if inline style overrides. Actually, css is `.az-nav-rail-button { border: 2px solid; }`. I will change it to 3px solid.

2. `AzButton.jsx`:
- Add `textColor` prop.
- Apply `color: textColor || color` directly to the `span` element with `className="az-button-text"`.
- In `AzButton.css`, update:
  - `.az-button { border-width: 3px; }`
  - `.az-button-shape-circle` and `.az-button-shape-square`: `width: 72px; height: 72px;`
  - `.az-button-shape-rectangle` and `.az-button-shape-none`: `width: 72px; height: 40px;` (maybe `min-width: 72px` instead of `width` if they shouldn't stretch? Wait, Android says `width(size)` which sets fixed width for RECTANGLE/NONE). Let's set `width: 72px`.

React Native Changes:
1. `AzButton.tsx`:
- Add `textColor?: string;` to `AzButtonProps`.
- Update `size` to `72`.
- Update `borderWidth` to `isNone ? 0 : 3`.
- Update RECTANGLE / NONE height to `40`.
  - For rectangle/none: `containerStyle.height = 40;` `containerStyle.width = 72;`
- Apply `textColor || color` to `textStyle.color`.

2. Let's make sure the background fill has NO backgrounds.
"the fill (no background!) is the same" - what does this mean?
In RN `AzButton.tsx`, there's:
`<View style={[StyleSheet.absoluteFill, { backgroundColor: actualFillColor, zIndex: -1, borderRadius: containerStyle.borderRadius }]} pointerEvents="none" />`
This acts as a fill overlay because `backgroundColor` on the main container would conflict or something, or it's designed to not be a true background color for opacity reasons. Wait, is that what they mean? Yes, RN has NO background on the main container.
Wait, what about Web?
In Web `AzButton.jsx`, we have `backgroundColor: finalFillColor`. Should it also use a pseudo-element or separate absolute div to avoid background? "the fill (no background!) is the same". Maybe they mean the `backgroundColor` property shouldn't be used directly on the `button` in Web, but rather an absolute span with `z-index: -1`?
Wait, if I look at the Web memory: "In aznavrail-react-native, when applying custom background fills to TouchableOpacity buttons like AzButton, use an absolutely positioned, pointerEvents="none" underlying <View> with zIndex: -1 instead of changing the container's backgroundColor or switching to TouchableHighlight. This preserves the intended opacity reduction interaction model."
And "the fill (no background!) is the same" might mean the computed fill color logic. "make sure button shape thickness and the fill (no background!) is the same." So the background of the button should be `transparent`, and the fill is rendered via the absolute positioning or just matching the color math (`rgba` with 0.25 opacity)?
Wait! In Web `AzNavRailButton.jsx` and `AzButton.jsx`:
`backgroundColor: finalFillColor`
Let's see if there's any problem there. "the fill (no background!) is the same" could mean they want the exact same alpha math. I already see Web has:
`const computedFillColor = (lowerColor === 'black' || lowerColor === '#000000' || lowerColor === '#000') ? 'rgba(255, 255, 255, 0.25)' : 'rgba(0, 0, 0, 0.25)';`
Which perfectly matches Android.
Does Android's `AzNavRailButton.kt` have `backgroundColor`?
In Android `AzNavRailButton.kt`:
```kotlin
    Surface(
        shape = buttonShape,
        color = containerColor,
```
`Surface` uses `color` which is the background color. "no background" might mean "no background on the button itself, use a separate layer" or it might refer to not using standard background colors from themes.
Or maybe the "no background!" refers to shapes: NONE.
"button shape thickness and the fill (no background!) is the same."
Ah! "no background!" might literally mean: when `fillColor` is specified, it should NOT override the `transparent` background of the `button` container, but it should be a fill! But `backgroundColor: finalFillColor` works fine in web. Wait, in Android `fillColor` is a parameter, and it defaults to the calculated color. But if it's "no background!", it might mean `fillColor` on shape NONE should be `transparent`?
Let's check Android code for `AzNavRailButton.kt`:
Wait, the `containerColor` is used for `Surface(color = containerColor)`. Even if shape is NONE, it has a fill color unless disabled by alpha? No, `Surface` will clip to `RectangleShape` and apply `containerColor`.
Wait! What if "fill (no background!)" means the fill is an internal color overlay and not CSS `background`? No, if we just set it identically it's fine.

Let's review "the fill (no background!) is the same". Maybe they are talking about the `fillColor` property? Yes, `fillColor` is explicitly mentioned. They want `fillColor` parameter. I already verified RN and Web both have `fillColor` parameter logic.

I'll check `RailMenuItem.tsx` in React Native to see if it needs dimension updates, but it's a menu item, not a rail button. Menu items don't have shapes, they are just rows. Wait, does it? "button shape thickness and the fill".

Let me draft the plan.
