# Platform Parity

AzNavRail has three public implementations:

- **Android** — `aznavrail`
- **Compose Multiplatform** — `aznavrail-cmp`
- **React / React Native / Web** — `aznavrail-react`

Android remains the reference implementation for behavior. CMP and React are expected to expose the same user-facing AzNavRail concepts unless a feature is intrinsically platform-specific (for example Android system overlays or KSP-generated Android activities).

This document is the parity contract. A feature should not be described as cross-platform unless it behaves equivalently in all applicable implementations.

---

## Current parity contract

| Behavior | Android | CMP | React |
| --- | :---: | :---: | :---: |
| Rail/menu item DSL | ✅ | ✅ | ✅ |
| Nested hosts | ✅ | ✅ | ✅ |
| Unattached hosts | ✅ | ✅ | ✅ |
| Unattached subtree scrolling | ✅ | ✅ | ✅ |
| Configurable unattached maximum height | ✅ | ✅ | ✅ |
| Global rail/chrome visibility | ✅ | ✅ | ✅ |
| Per-unattached-host visibility | ✅ | ✅ | ✅ |
| Reversible visibility animation | ✅ | ✅ | ✅ |
| Visibility duration control | ✅ | ✅ | ✅ |
| `NONE` visibility transition | ✅ | ✅ | ✅ |
| `DISSOLVE` visibility transition | ✅ | ✅ | ✅ |
| `NEAREST_EDGE` visibility transition | ✅ | ✅ | ✅ |
| `SWIPE_LEFT` visibility transition | ✅ | ✅ | ✅ |
| Floating windows (`AzWindow`) | ✅ | ✅ | ✅ |
| Popups (`azPopup` / `AzPopup`) | ✅ | ✅ | ✅ |
| Three highlight channels | ✅ | ✅ | ✅ |
| About de-duplication / warm-up | ✅ | ✅ | ✅ |
| Status-driven guidance | ✅ | ✅ | ✅ |
| Android system overlay | ✅ | Android target only | n/a |
| `@Az` + KSP-generated Android graph/activity | ✅ | n/a | n/a |

Platform-specific implementation mechanics are allowed to differ. Observable behavior should not.

---

## Visibility

Visibility is **controlled state**. Hiding Az chrome does not destroy app content or reset AzNavRail state. Restoring visibility reverses the configured transition.

The four transition modes are:

- `NONE` — immediate hide/show.
- `DISSOLVE` — opacity transition.
- `NEAREST_EDGE` — exits toward the nearest screen edge and returns from the same direction.
- `SWIPE_LEFT` — exits left and reverses on show.

The same duration is used in both directions. Negative durations are invalid.

### Android / Compose Multiplatform

Use the visibility fields exposed by the Az configuration/host APIs. Names follow the Kotlin convention:

```kotlin
azConfig(
    visible = chromeVisible,
    visibilityAnimation = AzVisibilityAnimation.DISSOLVE,
    visibilityDurationMillis = 250,
)
```

An unattached host may override visibility for its own complete subtree:

```kotlin
azUnattachedHostItem(
    id = "tools",
    text = "Tools",
    maxHeight = 420.dp,
    visible = toolsVisible,
    visibilityAnimation = AzVisibilityAnimation.NEAREST_EDGE,
    visibilityDurationMillis = 250,
)
```

A global hide always wins. A locally visible host cannot punch through globally hidden Az chrome.

### React

```tsx
<AzNavRail
  visible={chromeVisible}
  visibilityAnimation={AzVisibilityAnimation.DISSOLVE}
  visibilityDurationMillis={250}
>
  {/* app screen content remains mounted */}
</AzNavRail>
```

Per unattached host:

```tsx
<AzUnattachedHostItem
  id="tools"
  text="Tools"
  anchor={AzUnattachedAnchor.FLOATING}
  maxHeight={420}
  visible={toolsVisible}
  visibilityAnimation={AzVisibilityAnimation.NEAREST_EDGE}
  visibilityDurationMillis={250}
/>
```

React uses an inherited visibility provider internally. Global visibility therefore applies to rail chrome, unattached hosts, windows, and other Az surfaces without forcing each child to duplicate the global state.

---

## Unattached host overflow

An unattached host owns a complete subtree. When that subtree is taller than the available viewport, **the subtree scrolls inside the host instead of overflowing off-screen**.

`maxHeight` limits the visible viewport; it is not a clipping instruction. Content beyond the limit remains reachable by scrolling.

The library also applies a safe viewport ceiling so a caller cannot accidentally request a host taller than the usable window:

- Android: derived from the current Compose configuration/window.
- CMP: derived from common Compose `LocalWindowInfo` and density, so Android, Desktop, and wasm use the same common implementation.
- React: derived from the current React Native/Web window dimensions.

The effective height is the smaller of the caller's `maxHeight` and the safe viewport height.

---

## Sample applications

Every maintained sample is expected to exercise the parity-sensitive APIs rather than merely compile against them:

| Sample | Platform | What it demonstrates |
| --- | --- | --- |
| `SampleApp` | Android | Unattached host maximum height, scrolling, controlled visibility, reversible transitions |
| `aznavrail-cmp-demo` | CMP Desktop + wasm | The same unattached/visibility behavior from `commonMain` |
| `sample-pwa` | React/Web | The same visibility modes and bounded unattached-host behavior through `aznavrail-react` |

A sample that still uses an older signature is a regression even if the library itself compiles.

---

## Migration from the previous API

Existing code remains valid when it does not use the new controls; defaults preserve visible chrome and the library's normal transition behavior.

When adopting the parity API:

1. Hoist visibility state rather than conditionally removing the AzNavRail component.
2. Use `visible` to hide/show chrome while retaining navigation and host state.
3. Set `maxHeight` on large unattached hosts instead of manually clipping their children.
4. Choose one of the four `AzVisibilityAnimation` values rather than hand-animating the outer surface.
5. Keep the same duration for hide and show unless a future API explicitly introduces asymmetric timing.

Do **not** unmount an unattached host merely to hide it if you expect its expansion, scroll, drag, or nested state to survive.

---

## What parity does not mean

Parity means equivalent public behavior, not byte-for-byte implementation.

- Android may use Android-only system APIs.
- CMP common code must not import Android-only configuration APIs.
- React may use `Animated`, DOM/RN pointer semantics, and window metrics.
- A feature with no meaningful equivalent on a platform is marked **n/a**, not "missing."

If Android gains a generally applicable public behavior, CMP, React, their samples, and the documentation must be updated in the same feature cycle. Otherwise the feature is not finished.
