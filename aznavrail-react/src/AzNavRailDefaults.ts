/** Compile-time constants that mirror the default dimension and layout values from the Kotlin library. */
export const AzNavRailDefaults = {
  /** Width and height of the app-icon in the rail header, in dp. */
  HeaderIconSize: 48,
  /** Padding around the header icon container, in dp. */
  HeaderPadding: 16,
  /** Total height of the rail header row, in dp. */
  HeaderHeightDp: 56,
  /** Horizontal padding inside the rail's icon column, in dp. */
  RailContentHorizontalPadding: 12,
  /** Vertical gap between consecutive rail buttons, in dp. */
  RailContentVerticalArrangement: 8,
  /** Minimum swipe distance (px) required to trigger undocking the rail into floating mode. */
  SWIPE_THRESHOLD_PX: 50,
  /** Distance (px) from the docked position within which a released floating rail snaps back to the edge. */
  SNAP_BACK_RADIUS_PX: 100,
  /** Default rail width when collapsed to icon-only mode, in dp. */
  CollapsedRailWidth: 100,
  /** Default rail width when expanded to show the full menu, in dp. */
  ExpandedRailWidth: 160,
  /** Standard button size used in the full-size rail, in dp. */
  ButtonWidth: 72,
  /** Smaller button size used when the rail is in compact/shrunk mode, in dp. */
  ShrunkButtonWidth: 56,
  /** Sentinel value for `screenTitle` meaning no title should be shown in the host header. */
  NO_TITLE: 'NO_TITLE',
  /** The colour a rail item falls back to when neither the item nor the host names one. */
  AccentFallback: '#6200ee',
  /**
   * Auto-generated ID for the implicit About (`?`) button appended to the end of the rail when
   * the developer declared none of their own.
   */
  AutoAboutId: 'AZ_AUTO_ABOUT_ID_INTERNAL',
};

/**
 * The library's one motion scale — the TypeScript twin of Kotlin's `AzMotion`.
 *
 * Every transition in the port reads its timing from here. Before this existed the same two
 * numbers — a 60 ms stagger and a 720 ms item duration — were written out as literal defaults in
 * seven separate places, which is how they drifted past the point of being useful: an eight-item
 * drawer took **1.2 seconds** to finish arriving, and because the turnstile entrance starts each
 * item edge-on (and therefore invisible), the panel sat there empty for the first stretch of it.
 *
 * Motion here is a guide, not a toll. It tells the user where a thing came from and then gets out
 * of the way. The durations are drawn from Material 3 Expressive's scale and deliberately sit at
 * its quick end: a cascade the user has to wait out has stopped being a cue and become a delay.
 */
export const AzMotion = {
  /** One item's own entrance or exit. M3 Expressive `medium` — spatial, but on the fast side. */
  ItemDurationMs: 280,
  /**
   * The gap between one item starting and the next. Small enough that the cascade reads as a
   * single gesture rather than a queue.
   */
  ItemStaggerMs: 22,
  /** A container arriving or leaving — a panel, a scrim, a popup. M3 Expressive `effects`. */
  PanelDurationMs: 200,
  /** A layout settling into a new size, e.g. the rail's collapsed/expanded width. */
  SettleDurationMs: 240,
  /** One step of a continuous, non-blocking indicator. */
  IndicatorStepMs: 420,
} as const;
