from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one match, found {count}: {old[:100]!r}")
    write(path, text.replace(old, new, 1))


def append_section_once(path: Path, heading: str, body: str) -> None:
    text = read(path)
    if heading in text:
        return
    write(path, text.rstrip() + "\n\n" + body.strip() + "\n")


# README: remove the obsolete warning that React lags and point at the parity contract.
readme = ROOT / "README.md"
text = read(readme)
text = re.sub(
    r"> \*\*Platform parity note\.\*\*.*?\n\n## 📚 Documentation",
    "> **Platform parity note.** Android (`aznavrail`) is the behavioral reference, while Compose Multiplatform (`aznavrail-cmp`) and React (`aznavrail-react`) expose the same generally applicable AzNavRail behavior. Platform-only features are explicitly marked as such. See [Platform Parity](docs/PLATFORM_PARITY.md) for the contract and migration notes.\n\n## 📚 Documentation",
    text,
    count=1,
    flags=re.S,
)
if "**[Platform Parity](/docs/PLATFORM_PARITY.md)**" not in text:
    needle = "-   **[Capabilities & Limitations](/docs/CAPABILITIES_AND_LIMITATIONS.md)**"
    idx = text.find(needle)
    if idx == -1:
        raise RuntimeError("README documentation-list marker not found")
    end = text.find("\n", idx)
    text = text[: end + 1] + "-   **[Platform Parity](/docs/PLATFORM_PARITY.md)**: Cross-platform contract, visibility/overflow API, samples, and migration notes.\n" + text[end + 1 :]
write(readme, text)


# Capabilities: replace the old two-platform and stale feature-parity tables.
cap = ROOT / "docs/CAPABILITIES_AND_LIMITATIONS.md"
text = read(cap)
platform_block = """## Platform parity

Android is the behavioral reference implementation. Compose Multiplatform and React are expected to match every generally applicable public behavior. See [Platform Parity](PLATFORM_PARITY.md) for the full contract and migration notes.

| Area | Android | CMP | React |
| :--- | :---: | :---: | :---: |
| Kinetic entrance/exit/title | ✅ | ✅ | ✅ |
| Tilt-on-press | ✅ | ✅ where pointer semantics apply | ✅ web; native RN no-op |
| `expandWhen` / host `initiallyExpanded` | ✅ | ✅ | ✅ |
| Unattached hosts | ✅ | ✅ | ✅ |
| Unattached subtree scrolling | ✅ | ✅ | ✅ |
| Per-host `maxHeight` | ✅ | ✅ | ✅ |
| Global controlled visibility | ✅ | ✅ | ✅ |
| Per-unattached-host visibility | ✅ | ✅ | ✅ |
| `NONE` / `DISSOLVE` / `NEAREST_EDGE` / `SWIPE_LEFT` | ✅ | ✅ | ✅ |
| Visibility duration control | ✅ | ✅ | ✅ |
| Popups and floating windows | ✅ | ✅ | ✅ |
| Shared rail palette | ✅ | ✅ | ✅ |
| Three highlights | ✅ | ✅ | ✅ |
| About de-duplication / warm-up | ✅ | ✅ | ✅ |
| Status-driven guidance | ✅ | ✅ | ✅ |
| System overlay | ✅ | Android target only | n/a |
| `@Az` + KSP-generated Android activity graph | ✅ | n/a | n/a |

---

## Feature parity (as of this release)

| Feature | Android (`aznavrail`) | CMP (`aznavrail-cmp`) | React (`aznavrail-react`) |
| :--- | :---: | :---: | :---: |
| Drop-down menus | ✅ | ✅ | ✅ |
| Unattached hosts | ✅ | ✅ | ✅ |
| Bounded/scrollable unattached subtrees | ✅ | ✅ | ✅ |
| Reversible Az chrome visibility | ✅ | ✅ | ✅ |
| Floating-host screen-edge / rail docking | ✅ | ✅ | ✅ where supported by React host renderer |
| Per-item state / overrides | ✅ | ✅ | ✅ |
| Popups + warning treatment | ✅ | ✅ | ✅ |
| Pinned \"More from Az\" rail item | ✅ | ✅ | ✅ |
| Active / focus / secondary highlights | ✅ | ✅ | ✅ |
| Per-item highlight colours | ✅ | ✅ | ✅ |
| About de-duplication across surfaces | ✅ | ✅ | ✅ |
| About content warm-up | ✅ | ✅ | ✅ |
| Auto-sizing footer labels | ✅ | ✅ | ✅ |
| Floating windows (`AzWindow`) | ✅ | ✅ | ✅ |
| Hidden menu in a window | ✅ | ✅ | ✅ |
| Dissolve overlay on item tap | ✅ | ✅ | ✅ |
| Unit tests | ✅ | ✅ | ✅ |

"""
text, count = re.subn(
    r"## Platform parity\n.*?## Known gaps\n",
    platform_block + "## Known gaps\n",
    text,
    count=1,
    flags=re.S,
)
if count != 1:
    raise RuntimeError("Capabilities parity block not found")
text = re.sub(
    r"- \*\*The React port lags the Kotlin modules\*\*.*?do\n  not move together\.\n",
    "- **Platform parity is now part of the release contract.** Android remains the behavioral reference; generally applicable Android changes must land in CMP, React, their samples, and the documentation in the same feature cycle.\n",
    text,
    count=1,
    flags=re.S,
)
write(cap, text)


api_section = """## Cross-platform visibility and unattached overflow

See [Platform Parity](PLATFORM_PARITY.md) for the normative behavior.

The current public surface adds two related controls across Android, CMP, and React:

- **Controlled Az chrome visibility** — `visible`, `visibilityAnimation`, and `visibilityDurationMillis`.
- **Bounded unattached hosts** — `maxHeight`, with scrolling for the complete host subtree when content exceeds the effective viewport.

Visibility is reversible and state-preserving. Hiding Az chrome does not mean conditionally removing the navigation component. The supported transitions are `NONE`, `DISSOLVE`, `NEAREST_EDGE`, and `SWIPE_LEFT`.

An unattached host may also control its own visibility. Global hidden state always wins over local visibility.
"""
append_section_once(ROOT / "docs/API.md", "## Cross-platform visibility and unattached overflow", api_section)


dsl_section = """## Visibility and bounded unattached hosts

These controls are intentionally parallel across the Kotlin DSL and React component API. See [Platform Parity](PLATFORM_PARITY.md).

### Kotlin

```kotlin
azConfig(
    visible = chromeVisible,
    visibilityAnimation = AzVisibilityAnimation.DISSOLVE,
    visibilityDurationMillis = 250,
)

azUnattachedHostItem(
    id = "tools",
    text = "Tools",
    maxHeight = 420.dp,
    visible = toolsVisible,
    visibilityAnimation = AzVisibilityAnimation.NEAREST_EDGE,
    visibilityDurationMillis = 250,
)
```

`maxHeight` bounds the viewport rather than clipping the subtree; overflow remains reachable by scrolling.

### React

```tsx
<AzNavRail
  visible={chromeVisible}
  visibilityAnimation={AzVisibilityAnimation.DISSOLVE}
  visibilityDurationMillis={250}
>
  <AzUnattachedHostItem
    id="tools"
    text="Tools"
    maxHeight={420}
    visible={toolsVisible}
    visibilityAnimation={AzVisibilityAnimation.NEAREST_EDGE}
    visibilityDurationMillis={250}
  />
</AzNavRail>
```
"""
append_section_once(ROOT / "docs/DSL.md", "## Visibility and bounded unattached hosts", dsl_section)


guide_section = """## Cross-platform visibility and overflow contract

Android is the reference implementation, but controlled visibility and unattached-host overflow are one API contract across Android, Compose Multiplatform, and React. The detailed parity matrix and migration rules live in [Platform Parity](PLATFORM_PARITY.md).

The important behavioral guarantees are:

1. Az chrome may be hidden without destroying app screen content or AzNavRail state.
2. Showing reverses the configured hide transition.
3. `NONE`, `DISSOLVE`, `NEAREST_EDGE`, and `SWIPE_LEFT` are the complete visibility transition set for this API generation.
4. A large unattached host scrolls its whole subtree inside a bounded viewport instead of extending off-screen.
5. `maxHeight` is capped by a safe viewport height on every platform.
6. CMP derives viewport metrics from common Compose window information; common code must not depend on Android `LocalConfiguration`.
7. A global hide overrides locally visible Az surfaces.

The maintained Android, CMP, and PWA samples are part of this contract: a platform API change is unfinished until its sample demonstrates the same behavior.
"""
append_section_once(ROOT / "docs/AZNAVRAIL_COMPLETE_GUIDE.md", "## Cross-platform visibility and overflow contract", guide_section)


module_section = """## Platform parity

This module follows the shared AzNavRail behavioral contract documented in [`docs/PLATFORM_PARITY.md`](../docs/PLATFORM_PARITY.md). Android is the reference implementation; generally applicable behavior is expected to match here rather than merely resemble it.

The current parity-sensitive additions are controlled/reversible Az chrome visibility and bounded, scrollable unattached-host subtrees.
"""
for rel in ["aznavrail-cmp/README.md", "aznavrail-react/README.md"]:
    path = ROOT / rel
    if path.exists():
        append_section_once(path, "## Platform parity", module_section)

print("Platform parity documentation synchronized.")
