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
};
