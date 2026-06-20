/** Shape variant applied to an AzButton or nav-rail item icon. */
export let AzButtonShape = /*#__PURE__*/function (AzButtonShape) {
  /** Circular border, equal width and height. */
  AzButtonShape["CIRCLE"] = "CIRCLE";
  /** Square border with no border-radius. */
  AzButtonShape["SQUARE"] = "SQUARE";
  /** Full-width rectangular pill, height fixed to button height. */
  AzButtonShape["RECTANGLE"] = "RECTANGLE";
  /** No visible border or background — renders an invisible hit area. */
  AzButtonShape["NONE"] = "NONE";
  return AzButtonShape;
}({});

/** Which edge of the screen the navigation rail is anchored to. */
export let AzDockingSide = /*#__PURE__*/function (AzDockingSide) {
  /** Rail appears on the left edge. */
  AzDockingSide["LEFT"] = "LEFT";
  /** Rail appears on the right edge. */
  AzDockingSide["RIGHT"] = "RIGHT";
  return AzDockingSide;
}({});

/** Shape applied to the app-icon in the rail header. */
export let AzHeaderIconShape = /*#__PURE__*/function (AzHeaderIconShape) {
  /** Fully circular icon container. */
  AzHeaderIconShape["CIRCLE"] = "CIRCLE";
  /** Square icon container with sharp corners. */
  AzHeaderIconShape["SQUARE"] = "SQUARE";
  /** Square icon container with softly rounded corners. */
  AzHeaderIconShape["ROUNDED"] = "ROUNDED";
  return AzHeaderIconShape;
}({});

/** Layout direction of a nested-rail popup relative to its host item. */
export let AzNestedRailAlignment = /*#__PURE__*/function (AzNestedRailAlignment) {
  /** Items stack in a column next to the host. */
  AzNestedRailAlignment["VERTICAL"] = "VERTICAL";
  /** Items flow in a row next to the host. */
  AzNestedRailAlignment["HORIZONTAL"] = "HORIZONTAL";
  return AzNestedRailAlignment;
}({});

/**
 * The visual design of an `AzDropdownMenu` panel — a slice of the rail or the menu. The choice
 * drives both the item rendering and the panel width (so it matches what it imitates).
 */
export let AzDropdownDesign = /*#__PURE__*/function (AzDropdownDesign) {
  /** Collapsed-rail look: compact rail buttons, constrained to the collapsed rail width (≈100). */
  AzDropdownDesign["RAIL"] = "rail";
  /** Expanded-menu look: full-width labeled rows, constrained to the expanded menu width (≈160). */
  AzDropdownDesign["MENU"] = "menu";
  return AzDropdownDesign;
}({});

/** General orientation flag used by layout helpers. */
export let AzOrientation = /*#__PURE__*/function (AzOrientation) {
  /** Components arranged in a column. */
  AzOrientation["VERTICAL"] = "VERTICAL";
  /** Components arranged in a row. */
  AzOrientation["HORIZONTAL"] = "HORIZONTAL";
  return AzOrientation;
}({});

/** The visible edge of the screen where the rail physically appears after rotation adjustments. */
export let AzVisualSide = /*#__PURE__*/function (AzVisualSide) {
  /** Rail is visible on the left edge. */
  AzVisualSide["LEFT"] = "LEFT";
  /** Rail is visible on the right edge. */
  AzVisualSide["RIGHT"] = "RIGHT";
  /** Rail is visible on the top edge (landscape rotation). */
  AzVisualSide["TOP"] = "TOP";
  /** Rail is visible on the bottom edge (landscape rotation). */
  AzVisualSide["BOTTOM"] = "BOTTOM";
  return AzVisualSide;
}({});

/** Configuration bag passed to `<AzNavRail>` to control appearance and behaviour. */

/**
 * Describes what the tutorial overlay highlights during a card step.
 * Discriminated by the `type` field.
 */

/**
 * Specifies what the user must do to advance past a tutorial card.
 * Discriminated by the `type` field.
 */

/** A single instructional card shown within a tutorial scene. */

/** A named step within an `AzTutorial`, consisting of UI content and one or more instructional cards. */

/** A complete interactive tutorial composed of one or more scenes. */

/** Runtime controller for the tutorial system, obtained via `useAzTutorialController()`. */

/** A single entry in the long-press hidden menu of a draggable reloc item. */

/**
 * Fully-resolved nav item state used internally by `AzNavRail` to render each item.
 * Consumers declare items via the DSL helpers (`AzRailItem`, `AzMenuToggle`, etc.)
 * which construct this shape automatically.
 */

/** Base props shared by all DSL item-declaration components (e.g. `AzRailItem`, `AzMenuItem`). */

/** Props for toggle-type DSL items (`AzRailToggle`, `AzMenuToggle`). */

/** Props for cycler-type DSL items (`AzRailCycler`, `AzMenuCycler`). */

/** Props for host-type DSL items (`AzRailHostItem`, `AzMenuHostItem`) — a collapsible group header. */

/** Props for sub-item DSL components — an item nested under a host. */

/** Props for a toggle item nested under a host (`AzRailSubToggle`, `AzMenuSubToggle`). */

/** Props for a cycler item nested under a host (`AzRailSubCycler`, `AzMenuSubCycler`). */

/** Props for a drag-reorderable sub-item (`AzRailRelocItem`). */

/** Props for `AzNestedRail` — a rail item that opens a secondary popup rail containing child items. */

/** Builder scope passed to the `hiddenMenu` function prop of `AzRailRelocItemProps` for defining menu entries declaratively. */

/**
 * Discrete heights an `<AzBottomSheet>` can settle at, mirroring the four-detent
 * model ported from the Android library's `LogKitty`-style sheet.
 */
export let AzSheetDetent = /*#__PURE__*/function (AzSheetDetent) {
  /** A thin, near-invisible strip that swallows a drag-up gesture but lets touches pass otherwise. */
  AzSheetDetent["HIDDEN"] = "HIDDEN";
  /** A short ticker-height strip showing a single line of content. */
  AzSheetDetent["PEEK"] = "PEEK";
  /** Roughly half the parent height (tuned via `halfFraction`). */
  AzSheetDetent["HALF"] = "HALF";
  /** Nearly full parent height (tuned via `fullFraction`). */
  AzSheetDetent["FULL"] = "FULL";
  return AzSheetDetent;
}({});

/**
 * Static configuration for an `<AzBottomSheet>`. All values have defaults matching the
 * Android `AzSheetConfig`; override individually to tune for your context.
 */

/** Partial configuration used by the DSL builder helpers to construct an `AzNavItem`. */
//# sourceMappingURL=types.js.map