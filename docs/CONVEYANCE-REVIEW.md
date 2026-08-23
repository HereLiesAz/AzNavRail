# AzNavRail, measured against the Conveyance framework

A review of this library against the framework specified in
[Conveyance/docs/CONVEYANCE-FRAMEWORK.md](https://github.com/HereLiesAz/Conveyance/blob/main/docs/CONVEYANCE-FRAMEWORK.md).

The framework was designed first, from the manifesto alone, without reading a line of this
repository. This document is what happened when the two were put side by side.

---

## 0. The short version

AzNavRail did not fail because its instincts are wrong. Several of its most considered decisions
match, almost exactly, mechanisms the framework arrived at independently — `AzLoad`'s refusal to
settle is the framework's Yield; a toggle whose label *is* its state is the framework's One Element;
the alert morph and the travelling dissolve are Continuity; the two haptic voices are consequence
made tactile. Someone was thinking about the right things.

It failed for one structural reason:

> **It is a component, and conveyance is a property of a grammar.**
>
> A navigation rail can convey about itself. It cannot make the app around it convey, because it
> does not own the app's actions, transitions, or consequences. So the conveyance work went into the
> rail's own craft, one component at a time, by hand — and everywhere the rail's authority ends, the
> construction zone resumes untouched.

Everything else follows from that. Because conveyance had no generative core to come from, it had to
be added as care — and care doesn't scale across 38 DSL entry points, 50,000 lines and four
platforms. Where care ran out, the defaults are conventional: `disabled` at 38% alpha, cross-fading
route transitions, per-item colour overrides, configurable easing curves, and a very good coach-mark
engine.

The second failure is the one the manifesto would notice fastest, and it is worth stating plainly:

> **The library's own API is a construction zone.**
>
> 38 DSL functions and 16 public composables, documented by ~5,000 lines of prose including a file
> the README calls "The Bible." A framework about not needing instructions ships a bible. That is the
> Two-Sided Rule failing — the API is a user interface, and this one requires a manual.

---

## 1. What the framework says, and where the rail lands

| | Law | Verdict |
|---|---|---|
| 1 | **One Element** — invitation, progress, result and failure are the same pixels | **Partial.** Best-in-class in two places, absent elsewhere |
| 2 | **Continuity** — nothing appears from nowhere | **Partial.** Dissolve and alert-morph are real; route transitions are a cross-fade |
| 3 | **Grammar** — one motion signature per verb, everywhere, for nothing else | **Absent.** Motion is a style (Turnstile/Fade/SlideUp/tilt), configurable per app, carrying no verb |
| 4 | **Employment** — two jobs minimum | **Strong.** The rail is genuinely multi-employed and the README argues the case |
| 5 | **Benefit of the doubt** — reverse, demonstrate, escort | **Inverted.** `disabled` at 0.38 alpha, no inverse anywhere, and instruction is a first-class subsystem |

### 1.1 Where it is already right, precisely

These are not consolation prizes. Each one is a mechanism the framework specifies, already built:

- **`AzLoad` is the Yield.** "A shape that refuses to settle already reads as working; it needs no
  caption, and it localises for free." That is the framework's §5.5 argument, reached independently
  and stated better. Killing `showLabel` and the word "loading…" was exactly right.
- **Toggles and cyclers are One Element.** `azRailToggle(toggleOnText, toggleOffText)` — label is
  state is control. Three conventional elements (control, label, state indicator) collapsed into one.
  This is the library's single most conveyant construct and it is barely mentioned.
- **The alert morph is Continuity.** An item morphing into a warning triangle and back, rather than
  cutting, is Law 2 applied at component scale.
- **The dissolve is a real verb.** A label travelling outward from the rail across the window is
  `Enter` in embryo — and the note about hosting it at the window root so it isn't clipped is the
  kind of detail that only matters if you actually understand why the motion is load-bearing.
- **Haptics have two voices, `commit()` and `modeChange()`.** Distinguishing "you did a thing" from
  "the world changed mode" is a grammar decision, in the one channel where the library made one.
- **The UX analyzer is a Conscience.** `compose_destinations_ux.py`, and specifically
  *"KSP can prove a route exists. It cannot prove the route is not a furnished oubliette"* — that is
  the framework's Part VII thesis, already running in CI, already emitting SARIF. The
  `orphaned-error-state` rule is a Dead End audit under another name.
- **`AzDropdownMenu` "accepts no styling the rest of the library doesn't."** The instinct behind
  Channel Economy, applied once, to one component.

Six of the framework's mechanisms already exist here in some form. The problem is that they exist as
six good decisions rather than as one system, so nothing generates the seventh.

---

## 2. The four structural findings

### Finding 1 — Conveyance was applied as craft, not as a model

The library has no representation of *what a user can do*. It has representations of **items**:
things with an id, a label, a shape, a colour, a route and a click handler. An item is an appearance
with a callback attached — which is exactly the modelling gap the framework identifies as the source
of every instruction ever written (§0).

Because there is no `Act`, there is nothing that knows:

- what will change when this is pressed → so there is no continuity to derive, and transitions must
  be configured by hand;
- what magnitude that change has → so there is no weight, and every press feels identical whether it
  opens a panel or wipes a database;
- what would make it available → so unavailability is a boolean and renders as 38% alpha;
- how to undo it → so nothing is reversible, and safety has to be borrowed from the platform's
  dialogs.

This is the root finding. The other three are symptoms.

### Finding 2 — Instruction is the largest and newest subsystem

`azStatus`, `azEdge`, `azGoal`, `azGuidanceTarget`, `azGuideRenderer`, `azSuppressGuide`,
`AzInstructionStep`, `HelpOverlay`, `helpList`, `azHelpRailItem`, `azHelpSubItem`, and an `info:
String` parameter repeated across 21 DSL signatures. A dim scrim, a spotlight punch-out, a text
callout, paged sub-steps, live re-routing, persisted completion.

It is genuinely well built. It is also a coach-mark engine, and the manifesto's whole argument is
that a coach mark is a sign at a construction site. `AGENTS_Prefs.md` already half-knows this — *"a
rail that has to caption its own buttons has already failed to convey them"* — and the fix applied
was to turn auto-generated captions **off by default**. That is the right call and it is not
sufficient: the mechanism is still the product's answer to "how does the user learn this," it is
still what a developer reaches for, and turning it off leaves nothing in its place.

**But this finding contains the single most valuable asset in the repository, and it is worth being
precise about why.**

`azStatus` / `azEdge` / `azGoal` is a **directed graph of world-states and the transitions between
them, with a live reactive engine that knows which transition the user needs next.** That is not a
tutorial feature. That is the semantic model the framework spends Part II specifying — statuses are
Gates, edges are Acts, goals are intents — and it already exists here, tested
(`AzStatusEngineTest`, `AzGuidanceRoutingTest`), cross-platform, and wired to live predicates.

**The model is right. It is piped into the wrong renderer.**

Today the engine computes "the user needs to reach status X, via the control at these bounds" and
then draws a caption next to it. The framework's Escort (§5.2) consumes *exactly the same
computation* and instead moves the user to the control and articulates it. `azGuideRenderer` already
exists as the seam where the renderer can be swapped.

That is the highest-leverage change available in this codebase, and it is a renderer, not a rewrite.

### Finding 3 — Channels are handed out, not assigned

The framework's Part IV requires every visual channel to carry exactly one global meaning. This
library does the opposite: it treats channels as configuration surface, and hands them to the app
developer with defaults.

A single rail item can be coloured through `color`, `textColor`, `fillColor`,
`translucentBackgroundColor`, and then `azHighlight(active, focus, secondary, tertiary)` — eight
entry points, against a theme that itself sets five more. There are eight button shapes. `azKinetics`
exposes `entranceDurationMs`, `entranceEasing`, `entranceStaggerMs`, `entranceStartAngle` and
`maxTiltDegrees`.

Three consequences, all of which have already happened:

1. **Meaning cannot be relied upon.** If colour is configurable per item, colour cannot mean rank —
   so rank has to be re-established some other way, which is what the four separate highlights are.
   Four highlights is not a feature; it is the symptom of a channel that was spent and had to be
   bought back.
2. **The library cannot be internally consistent across apps**, so nothing a user learns in one
   AzNavRail app transfers to another — which was the whole return on grammar (§9.2).
3. **Duration and easing are in the API**, which the framework's Audit 5 fails outright. Not because
   configurable timing is inelegant, but because an app whose motion timing is a per-call decision
   cannot have a motion grammar, and without a grammar motion is decoration.

The library is not styleable *enough* to satisfy a designer who wants their brand, and it is far too
styleable to guarantee anything to a user. That is the worst position on the axis. The framework's
answer — assign every channel one meaning, ship no overrides — is more restrictive than "dictatorially
restrictive" currently is, and is the direction the README's own joke is pointing.

### Finding 4 — The API violates the Two-Sided Rule

Measured on the surface itself:

| | |
|---|---|
| DSL functions on `AzNavRailScope` | 38 |
| Public composables | 16 |
| Lines of prose documentation | ~4,950 (README 1,308 + `docs/` 3,649) |
| Largest single DSL signature | `azRailRelocItem`, 27 parameters |
| `isLoading: Boolean` repeated across signatures | 25 |
| `textColor` / `fillColor` / `translucentBackgroundColor` / `badge` | 23 each |
| `info: String?` | 21 |
| `disabled: Boolean` | 20 |

The repetition tells the story. The DSL was grown as a cartesian product — {rail, menu} × {item,
toggle, cycler, slider, host, sub-item, sub-host, reloc, nested, help, about} — and each new cell
copies the same twenty-parameter tail. Every future property costs 23 edits and 23 doc updates.

The framework's answer is composition rather than enumeration: one `Act`, whose *placement* is a
scope and whose *behaviour* is its consequence class. Rail-versus-menu is a layout question, not a
type question, and it should never have entered the constructor's name.

And then the documentation. ~5,000 lines of prose for a navigation rail, one file titled "The Bible,"
a README section explaining a Gradle variant-resolution error, and a `docs/DSL.md` paragraph
describing a hybrid kerning-and-font-scale solver. Some of that is genuinely good writing. All of it
is the tell. **By the library's own philosophy, every page of that manual is a traffic cone.**

---

## 3. What the framework got from reading this

The review runs both ways. Three things here are better than what the spec had, and the spec should
absorb them:

1. **The CI-time UX audit is more mature than the spec's Part VII.** SARIF output, GitHub
   annotations, error-versus-warning tiers, and a stated rationale for why warnings stay
   non-blocking — *"conservative source analysis cannot see every dynamic route, but silence would be
   the more expensive lie."* That reasoning belongs verbatim in the Conscience's design.
2. **Haptic voices should be in Channel Economy explicitly.** The spec assigns haptics to
   "consequence magnitude"; `commit()` versus `modeChange()` is the sharper formulation — magnitude
   *and* kind, two voices, no more.
3. **"Per-item, never screen-blanking" is a general rule, not a loading rule.** `azItemState`'s
   per-item spinner beating a global `isLoading` overlay is Law 1 stated as an operational
   preference, and the spec should say it that way.

One correction back the other way: `isLoading` on a button currently renders by setting the button's
content to `alpha 0f` and drawing `AzLoad` in its place. That is a **swap**, not a Yield. The element
briefly stops being itself, which severs precisely the identity link the mechanism exists to preserve.
The Yield asks the button to deform — compress and fill — while remaining recognisably the button
that was pressed.

---

## 4. Conversion path

Not a rewrite. Ordered by leverage per unit of work.

### Keep
`AzLoad` · toggle/cycler as label-is-state-is-control · the alert morph · the dissolve · haptic
voices · `azStatus`/`azEdge`/`azGoal` (the graph, not the callout) · the UX analyzer · per-item over
global state.

### Do first — repoint the guidance engine
Implement the Escort as a built-in `azGuideRenderer`: when the engine resolves "next transition is
control C," scroll/travel to C and articulate it instead of drawing a caption over it. Same engine,
same graph, same tests. This converts the library's largest instruction subsystem into its largest
conveyance subsystem, and it is a renderer swap.

### Do second — abolish `disabled`
Replace `disabled: Boolean` with `requires: Gate`, where a gate names the element that satisfies it.
Pressing a gated item escorts to that element. Delete the 0.38 alpha. This removes the library's most
patronising construct and reuses the Escort built in step one. Twenty signatures change; the
migration is mechanical and `disabled` can deprecate rather than break.

### Do third — collapse the cartesian product
One item declaration, placement by scope, consequence by class. This is the change that makes every
later change cheap, and it is the one that shrinks the bible. Target: the DSL fits on one page.

### Do fourth — assign the channels
Write the Part IV table for AzNavRail. Fold four highlights back into one rank channel with a fixed
meaning. Remove `textColor`/`fillColor`/`translucentBackgroundColor` per item. Remove duration,
easing, stagger and tilt-degree parameters from `azKinetics` and replace the entrance styles with the
verb grammar. Expect this to be unpopular with anyone using the library today and correct anyway —
it is the only step that makes the library mean the same thing twice.

### Do fifth — route transitions
`slideInHorizontally + fadeIn` becomes the `Enter` morph: the pressed item expands to become the
destination. The library already knows which item was pressed and already tracks item bounds
(`onItemGloballyPositioned` exists for the tutorial overlay — the same measurement, used for
pedagogy instead of instruction).

### Retire
`HelpOverlay` · `helpList` · `info:` · `azHelpRailItem` / `azHelpSubItem` · text callouts ·
`AzInstructionStep`. Not because they are badly made — they are the best-made things here — but
because their existence is the reason the alternatives never had to be built.

---

## 5. The honest summary

AzNavRail is a good component library that was asked to be a philosophy. It could not become one,
because the philosophy's leverage point is the app's model of what a person can do, and a navigation
rail does not own that model — so conveyance could only ever be retrofitted, component by component,
by hand, faster than one person can maintain across 50,000 lines and four platforms.

What it proves is that the instincts were right the whole time. Every mechanism the framework
specifies that this library also built, it built well and for the stated reason. What was missing was
never taste. It was a core small enough that the taste didn't have to be reapplied 38 times.

The framework's test is unchanged and applies to this document too: hand it to someone who has never
seen it, say nothing, and watch.
