# Migrating from the Android (Jetpack Compose) DSL

The Android library declares a rail as a Compose DSL block of builder calls
(`azRailItem`, `azRailToggle`, `azAdvanced`, …) inside `AzNavRail { … }`. The React port has
no DSL block — it's a plain `<AzNavRail>` component driven by an `items: AzNavItem[]` array
and a `settings` object. This file maps one to the other. For behavior that genuinely
differs between platforms (not just spelling), see [KNOWN_GAPS.md](./KNOWN_GAPS.md) instead —
this file only covers where the two APIs already agree.

## Core shape

| Android (Compose DSL) | React |
| :--- | :--- |
| `AzNavRail(navController) { … }` | `<AzNavRail items={items} settings={settings} … />` |
| `azConfig(...)` (docking side, colors, widths) | `settings: AzNavRailSettings` prop |
| `azAdvanced(...)` (loading, drag, guidance edges) | fields on `settings` (`enableRailDragging`, etc.) |
| `azTheme(...)` | `settings.activeColor`, `headerIconShape`, `headerIconSize` |
| a builder call per item, in declaration order | one object per item, in array order, in `items` |

## Item builders → `AzNavItem` fields

Every Android builder call becomes one object in the `items` array. The builder name maps to
a boolean discriminant field on that object:

| Android builder | React `AzNavItem` shape |
| :--- | :--- |
| `azRailItem(id, text, route) { onClick }` | `{ id, text, route, isRailItem: true, onClick }` |
| `azMenuItem(id, text, route) { onClick }` | `{ id, text, route, isRailItem: false, onClick }` |
| `azRailHostItem(id, text, expandWhen)` | `{ id, text, isRailItem: true, isHost: true, isExpanded, onClick }` |
| `azRailSubItem(id, hostId, text)` | `{ id, hostId, text, isRailItem: true, isSubItem: true }` |
| `azRailToggle(id, isChecked, toggleOnText, toggleOffText)` | `{ id, isRailItem: true, isToggle: true, isChecked, toggleOnText, toggleOffText, onClick }` |
| `azRailCycler(id, options, selectedOption)` | `{ id, isRailItem: true, isCycler: true, options, selectedOption, onClick }` |
| `azRailRelocItem(id, hostId) { onRelocate; hiddenMenu { } }` | `AzRailRelocItemProps` — see the "Reorderable items" example in the root README |
| `azNestedRail(id) { … }` | nested-rail popup — see `AzNestedRailPopup` / the root README's "Nested rail" section |
| `azDivider()` | `{ id, isDivider: true }`, or the standalone `<AzDivider />` inside `AzDropdownMenu` |

`expandWhen` (reactive auto-expand) has no builder-call equivalent in React — drive
`isExpanded` yourself from whatever condition you'd have passed to `expandWhen`.

## Standalone components

These are already 1:1 by name and mirror the Compose composable's props closely enough that
the root README's per-feature "React Implementation" code blocks are the authoritative
reference, not this table:

`AzButton`, `AzToggle`, `AzCycler`, `AzTextBox`, `AzRoller`, `AzSlider`, `AzLoad`,
`AzDropdownMenu` (+ `AzDropdownItem`, `AzDivider`), `AzBottomSheet` /
`AzBottomSheetInsetAware`, `AzPopup`, `AzWindow`.

## `AzDropdownMenu`

Android declares items imperatively inside the `AzDropdownMenu { azItem(...) }` block; React
declares them as JSX children (`<AzDropdownItem />`, `<AzDivider />`) instead of array
entries — this is the one place the React API is declarative-children rather than
array-of-objects. `azConfig(design, dockingSide, trigger, ...)` becomes props directly on
`<AzDropdownMenu design={...} dockingSide={...} ... >`.

## Guidance DSL (`azStatus` / `azEdge` / `azGoal`)

The guidance framework has reached behavioral parity across platforms and keeps the same
concept names and persistence keys on both sides — see `AzGuidanceProvider`,
`AzInstructionOverlay`, and the guidance exports in `src/guidance/` for the React
equivalents of `azStatus`/`azEdge`/`azGoal`/`azGuidanceTarget`. The one visual difference
(bounding-box outline vs. true-geometry outline, no arrowhead) is documented in
KNOWN_GAPS.md, not here.

## Android-only surface with no React equivalent

Covered in full in KNOWN_GAPS.md, briefly:

- System overlay (`SYSTEM_ALERT_WINDOW`, `AzNavRailOverlayService` and friends) — React's
  stand-in is `<AzFloatingRail>`, which floats above the app's own tree but not other apps.
- System navigation-bar inset detection (`drawBehindNavBar`, gesture-nav margin) — has no
  meaningful web analog; `AzBottomSheetInsetAware` only gets you `env(safe-area-inset-bottom)`.
- `azUnattachedHostItem` — Android/CMP only.

## What isn't written yet

This file covers the DSL-to-props mapping. It does not attempt a symbol-for-symbol API
reference; for that, see [`docs/API.md`](../docs/API.md) (Android) and the TypeScript types
exported from `src/index.tsx` (React) directly.
