/** Shape variant applied to an AzButton or nav-rail item icon. */
export enum AzButtonShape {
  /** Circular border, equal width and height. */
  CIRCLE = 'CIRCLE',
  /** Square border with no border-radius. */
  SQUARE = 'SQUARE',
  /** Full-width rectangular pill, height fixed to button height. */
  RECTANGLE = 'RECTANGLE',
  /** No visible border or background — renders an invisible hit area. */
  NONE = 'NONE',
}

/** Which edge of the screen the navigation rail is anchored to. */
export enum AzDockingSide {
  /** Rail appears on the left edge. */
  LEFT = 'LEFT',
  /** Rail appears on the right edge. */
  RIGHT = 'RIGHT',
}

/** Shape applied to the app-icon in the rail header. */
export enum AzHeaderIconShape {
  /** Fully circular icon container. */
  CIRCLE = 'CIRCLE',
  /** Square icon container with sharp corners. */
  SQUARE = 'SQUARE',
  /** Square icon container with softly rounded corners. */
  ROUNDED = 'ROUNDED',
}

/** Layout direction of a nested-rail popup relative to its host item. */
export enum AzNestedRailAlignment {
  /** Items stack in a column next to the host. */
  VERTICAL = 'VERTICAL',
  /** Items flow in a row next to the host. */
  HORIZONTAL = 'HORIZONTAL',
}

/** General orientation flag used by layout helpers. */
export enum AzOrientation {
  /** Components arranged in a column. */
  VERTICAL = 'VERTICAL',
  /** Components arranged in a row. */
  HORIZONTAL = 'HORIZONTAL',
}

/** The visible edge of the screen where the rail physically appears after rotation adjustments. */
export enum AzVisualSide {
  /** Rail is visible on the left edge. */
  LEFT = 'LEFT',
  /** Rail is visible on the right edge. */
  RIGHT = 'RIGHT',
  /** Rail is visible on the top edge (landscape rotation). */
  TOP = 'TOP',
  /** Rail is visible on the bottom edge (landscape rotation). */
  BOTTOM = 'BOTTOM',
}

/** Configuration bag passed to `<AzNavRail>` to control appearance and behaviour. */
export interface AzNavRailSettings {
  /** Whether the app name is shown next to the header icon when the rail is expanded. */
  displayAppNameInHeader?: boolean;
  /** When true, rail buttons are packed tightly with reduced spacing. */
  packRailButtons?: boolean;
  /** Width of the rail in its expanded (menu) state, in dp. */
  expandedRailWidth?: number;
  /** Width of the rail in its collapsed (icon-only) state, in dp. */
  collapsedRailWidth?: number;
  /** Whether the footer with About/Feedback links is shown in the expanded menu. */
  showFooter?: boolean;
  /** When true, a full-screen loading spinner overlay is displayed over the content area. */
  isLoading?: boolean;
  /** Default button shape applied to items that do not specify their own shape. */
  defaultShape?: AzButtonShape;
  /** Allows the user to drag the rail away from the edge, turning it into a floating widget. */
  enableRailDragging?: boolean;
  /** Which screen edge the rail docks to. */
  dockingSide?: AzDockingSide;
  /** When true, the rail never expands into a menu — icon-only mode is permanent. */
  noMenu?: boolean;
  /** When true, the help/info overlay is displayed over the screen. */
  infoScreen?: boolean;
  /** Called when the user dismisses the info screen overlay. */
  onDismissInfoScreen?: () => void;
  /** Accent color applied to the currently active/selected item. */
  activeColor?: string;
  /** When true, haptic feedback is triggered on drag start and long-press. */
  vibrate?: boolean;
  /** Shape of the header icon container. */
  headerIconShape?: AzHeaderIconShape;
  /** CSS / RN color string used as the background tint for the floating-rail widget. */
  translucentBackground?: string;
  /** Secret-location server hostname (developer feature). */
  secLoc?: string;
  /** Port for the secret-location server (developer feature). */
  secLocPort?: number;
  /** URL of the app's source repository, shown in the footer. */
  appRepositoryUrl?: string;
  /** Whether the help overlay feature is enabled. */
  helpEnabled?: boolean;
  /** When true, docking side follows the physical device edge rather than logical left/right. */
  usePhysicalDocking?: boolean;
  /** Set of classifier strings; items whose classifiers match are highlighted or filtered. */
  activeClassifiers?: Set<string>;
  /** Called with an item id and its on-screen bounds after each layout pass. */
  onItemGloballyPositioned?: (id: string, bounds: any) => void;
  /** Map of item id → help text shown in the info overlay (alternative to per-item `info` prop). */
  helpList?: Record<string, string>;
  /** Map of tutorial id → `AzTutorial` definition; makes tutorials available to the help overlay. */
  tutorials?: Record<string, AzTutorial>;
}

/**
 * Describes what the tutorial overlay highlights during a card step.
 * Discriminated by the `type` field.
 */
export type AzHighlight =
  /** Highlights an arbitrary rectangular region of the screen. */
  | { type: 'Area'; bounds: { x: number; y: number; width: number; height: number } }
  /** Highlights the rail item with the given id, using its recorded layout bounds. */
  | { type: 'Item'; id: string }
  /** Darkens the entire screen with no spotlight cutout. */
  | { type: 'FullScreen' }
  /** No highlight — card floats with no overlay darkening. */
  | { type: 'None' };

/**
 * Specifies what the user must do to advance past a tutorial card.
 * Discriminated by the `type` field.
 */
export type AzAdvanceCondition =
  /** A "Next" button is shown on the card; the user taps it to advance. */
  | { type: 'Button' }
  /** The user must tap the highlighted rail item to advance. */
  | { type: 'TapTarget' }
  /** Any tap anywhere on the screen advances the card. */
  | { type: 'TapAnywhere' }
  /** Advances when `fireEvent(name)` is called on the tutorial controller. */
  | { type: 'Event'; name: string };

/** A single instructional card shown within a tutorial scene. */
export interface AzCard {
  /** Heading text rendered at the top of the card. */
  title: string;
  /** Body text explaining the step. */
  text: string;
  /** What the tutorial overlay highlights while this card is displayed. */
  highlight?: AzHighlight;
  /** What must happen for the tutorial to move past this card. Defaults to Button. */
  advanceCondition?: AzAdvanceCondition;
  /** Label for an optional secondary action button on the card. */
  actionText?: string;
  /** Callback invoked when the secondary action button is tapped. */
  onAction?: () => void;
  /**
   * Branch map for `TapTarget` advance: maps tapped item id → scene id to jump to.
   * Used when the card's `advanceCondition` is `TapTarget` and multiple items are valid targets.
   */
  branches?: Record<string, string>;
  /** Renders optional media (image, animation, etc.) above the card text. */
  mediaContent?: () => React.ReactNode;
  /** Checklist items rendered as a bulleted list within the card. */
  checklistItems?: string[];
}

/** A named step within an `AzTutorial`, consisting of UI content and one or more instructional cards. */
export interface AzScene {
  /** Unique identifier for this scene; used as a branch target. */
  id: string;
  /** Renders the demo UI shown behind the tutorial card overlay. */
  content: () => React.ReactNode;
  /** Sequence of instructional cards displayed during this scene. */
  cards: AzCard[];
  /** Called when all cards in this scene have been completed. */
  onComplete?: () => void;
  /** Name of the controller variable whose value is checked against `branches` to pick the next scene. */
  branchVar?: string;
  /** Map of variable-value → scene id; evaluated after this scene completes to determine the next scene. */
  branches?: Record<string, string>;
}

/** A complete interactive tutorial composed of one or more scenes. */
export interface AzTutorial {
  /** Ordered list of scenes; playback starts from `scenes[0]` unless branched. */
  scenes: AzScene[];
  /** Called when the user finishes all scenes without skipping. */
  onComplete?: () => void;
  /** Called when the user skips the tutorial before it completes. */
  onSkip?: () => void;
}

/** Runtime controller for the tutorial system, obtained via `useAzTutorialController()`. */
export interface AzTutorialController {
  /** Id of the tutorial currently playing, or `null` when no tutorial is active. */
  activeTutorialId: string | null;
  /** List of tutorial ids that have been marked as read (persisted via AsyncStorage). */
  readTutorials: string[];
  /** Arbitrary variables injected at `startTutorial` and used for scene branching. */
  currentVariables: Record<string, any>;
  /** Name of the most recently fired event waiting to be consumed, or `null`. */
  pendingEvent: string | null;
  /** Starts the tutorial with the given id, optionally seeding branch variables. */
  startTutorial: (id: string, variables?: Record<string, any>) => void;
  /** Immediately ends the active tutorial and clears all state. */
  endTutorial: () => void;
  /** Persists the tutorial id in AsyncStorage so `isTutorialRead` returns true. */
  markTutorialRead: (id: string) => void;
  /** Returns true if the tutorial with the given id has been previously read. */
  isTutorialRead: (id: string) => boolean;
  /** Sets `pendingEvent` so that an `Event`-type advance condition can resolve. */
  fireEvent: (name: string) => void;
  /** Clears `pendingEvent` after the overlay has consumed it. */
  consumeEvent: () => void;
}

/** A single entry in the long-press hidden menu of a draggable reloc item. */
export interface HiddenMenuItem {
  /** Unique identifier for this menu entry. */
  id: string;
  /** Display label for the menu item. */
  text: string;
  /** Alternative label shown when the item is rendered inside the expanded rail menu. */
  menuText?: string;
  /** If provided, tapping opens this URL instead of calling `onClick`. */
  route?: string;
  /** When true, renders an `AzTextBox` input instead of a label. */
  isInput?: boolean;
  /** Placeholder hint shown inside the text input when `isInput` is true. */
  hint?: string;
  /** Pre-filled value for the text input when `isInput` is true. */
  initialValue?: string;
  /** Called when a non-input menu item is tapped. */
  onClick?: () => void;
  /** Called on each keystroke when `isInput` is true. */
  onValueChange?: (value: string) => void;
}

/**
 * Fully-resolved nav item state used internally by `AzNavRail` to render each item.
 * Consumers declare items via the DSL helpers (`AzRailItem`, `AzMenuToggle`, etc.)
 * which construct this shape automatically.
 */
export interface AzNavItem {
  /** Unique identifier for this item. */
  id: string;
  /** Primary display label shown on the rail button. */
  text: string;
  /** Alternative label used in the expanded menu; falls back to `text`. */
  menuText?: string;
  /** Navigation route string associated with this item. */
  route?: string;
  /** Screen title displayed in the host layout header when this item is active. */
  screenTitle?: string;
  /** Whether this item appears in the collapsed (icon-only) rail. */
  isRailItem: boolean;
  /** Border and text color for the item button. */
  color?: string;
  /** Overrides the text color independently of the border color. */
  textColor?: string;
  /** Background fill color inside the button shape. */
  fillColor?: string;
  /** True when this item is a two-state toggle button. */
  isToggle: boolean;
  /** Current checked state for toggle items. */
  isChecked?: boolean;
  /** Label shown on the rail button when the toggle is in the ON (checked) state. */
  toggleOnText: string;
  /** Label shown on the rail button when the toggle is in the OFF (unchecked) state. */
  toggleOffText: string;
  /** Menu label override for the ON state; falls back to `toggleOnText`. */
  menuToggleOnText?: string;
  /** Menu label override for the OFF state; falls back to `toggleOffText`. */
  menuToggleOffText?: string;
  /** True when this item cycles through a list of options on each tap. */
  isCycler: boolean;
  /** Ordered list of option labels for cycler items. */
  options?: string[];
  /** Per-option labels used in the expanded menu; must match `options` length. */
  menuOptions?: string[];
  /** Currently selected cycler option. */
  selectedOption?: string;
  /** True when this item renders as a visual separator line. */
  isDivider: boolean;
  /** When true, the rail collapses to icon-only mode after this item is tapped. */
  collapseOnClick: boolean;
  /** Button shape used to render this item's icon. */
  shape: AzButtonShape;
  /** Whether the item is non-interactive. */
  disabled: boolean;
  /** Cycler options that are skipped when cycling (still displayed but not selectable). */
  disabledOptions?: string[];
  /** True when this item is a collapsible group header in the rail. */
  isHost: boolean;
  /** True when this item is a child nested under a host item. */
  isSubItem: boolean;
  /** Id of the parent host item when `isSubItem` is true. */
  hostId?: string;
  /** Whether this host item's sub-items are currently visible. */
  isExpanded: boolean;
  /** Called when the item is tapped. */
  onClick?: () => void;
  /** Called when the item gains focus. */
  onFocus?: () => void;
  // Reloc Item properties
  /** True when this item participates in drag-to-reorder within its host cluster. */
  isRelocItem?: boolean;
  /** Resolved hidden-menu entries shown on long-press for reloc items. */
  hiddenMenu?: HiddenMenuItem[];
  /** When true, the hidden menu is shown immediately without requiring a long-press. */
  forceHiddenMenuOpen?: boolean;
  /** Called when the hidden menu is closed. */
  onHiddenMenuDismiss?: () => void;
  /** Called after a successful drag-reorder with the old index, new index, and new id order. */
  onRelocate?: (fromIndex: number, toIndex: number, newOrder: string[]) => void;
  // Info/Help
  /** Help text displayed on this item's card in the info overlay. */
  info?: string;
  // New properties for parity
  /** Classifier tags used to filter or highlight this item when `activeClassifiers` is set. */
  classifiers?: Set<string>;
  /** Custom React content rendered inside the button instead of text. */
  content?: any;
  /** True when this item opens a nested-rail popup when tapped. */
  isNestedRail?: boolean;
  /** When true, the nested-rail popup remains open until explicitly dismissed. */
  keepNestedRailOpen?: boolean;
  /** True when this item was declared via `AzHelpRailItem` or `AzHelpSubItem`. */
  isHelpItem?: boolean;
  /** Layout direction of this item's nested-rail popup. */
  nestedRailAlignment?: AzNestedRailAlignment;
  /** Pre-resolved sub-items for the nested-rail popup. */
  nestedRailItems?: AzNavItem[];
  /** Settings overrides applied only to this item's nested-rail popup. */
  nestedRailSettings?: any;
}

/** Base props shared by all DSL item-declaration components (e.g. `AzRailItem`, `AzMenuItem`). */
export interface AzNavItemProps {
  /** Unique identifier; used as navigation key and help-overlay anchor. */
  id: string;
  /** Primary label shown on the rail button and in the menu. */
  text: string;
  /** Alternative label shown only in the expanded menu. */
  menuText?: string;
  /** Navigation route string for this item. */
  route?: string;
  /** Screen title displayed in the host header when this item is active. */
  screenTitle?: string;
  /** Disables tapping when true. */
  disabled?: boolean;
  /** Called when the item is tapped. */
  onClick?: () => void;
  /** Called when the item receives focus. */
  onFocus?: () => void;
  /** Border and icon tint color. */
  color?: string;
  /** Independent text color override. */
  textColor?: string;
  /** Background fill color inside the button shape. */
  fillColor?: string;
  /** Button shape; overrides the rail-level `defaultShape` if set. */
  shape?: AzButtonShape;
  /** Help text shown for this item in the info overlay. */
  info?: string;
  /** Custom React content rendered inside the button instead of the text label. */
  content?: any;
  /** Classifier tags used for filtering or highlighting with `activeClassifiers`. */
  classifiers?: Set<string>;
}

/** Props for toggle-type DSL items (`AzRailToggle`, `AzMenuToggle`). */
export interface AzToggleProps extends AzNavItemProps {
  /** Current checked/on state. */
  isChecked: boolean;
  /** Rail button label when the toggle is ON. */
  toggleOnText: string;
  /** Rail button label when the toggle is OFF. */
  toggleOffText: string;
  /** Menu label override for the ON state. */
  menuToggleOnText?: string;
  /** Menu label override for the OFF state. */
  menuToggleOffText?: string;
}

/** Props for cycler-type DSL items (`AzRailCycler`, `AzMenuCycler`). */
export interface AzCyclerProps extends AzNavItemProps {
  /** Ordered list of option labels to cycle through. */
  options: string[];
  /** Per-option labels for the expanded menu; must match `options` in length. */
  menuOptions?: string[];
  /** The currently selected option label. */
  selectedOption: string;
  /** Options that are skipped during cycling. */
  disabledOptions?: string[];
}

/** Props for host-type DSL items (`AzRailHostItem`, `AzMenuHostItem`) — a collapsible group header. */
export interface AzHostItemProps extends AzNavItemProps {}

/** Props for sub-item DSL components — an item nested under a host. */
export interface AzSubItemProps extends AzNavItemProps {
  /** Id of the parent host item this sub-item belongs to. */
  hostId: string;
}

/** Props for a toggle item nested under a host (`AzRailSubToggle`, `AzMenuSubToggle`). */
export interface AzSubToggleProps extends AzToggleProps {
  /** Id of the parent host item. */
  hostId: string;
}

/** Props for a cycler item nested under a host (`AzRailSubCycler`, `AzMenuSubCycler`). */
export interface AzSubCyclerProps extends AzCyclerProps {
  /** Id of the parent host item. */
  hostId: string;
}

/** Props for a drag-reorderable sub-item (`AzRailRelocItem`). */
export interface AzRailRelocItemProps extends AzSubItemProps {
  /** Called after the user drops the item to a new position with the source index, target index, and new ordering. */
  onRelocate?: (fromIndex: number, toIndex: number, newOrder: string[]) => void;
  /** Hidden-menu definition: either an array of simple label/action pairs, or a builder function using `HiddenMenuScope`. */
  hiddenMenu?: { text: string; onClick: () => void }[] | ((scope: HiddenMenuScope) => void);
  /** When true, the hidden menu is shown immediately on render without requiring a long-press. */
  forceHiddenMenuOpen?: boolean;
  /** Called when the hidden menu is dismissed by the user. */
  onHiddenMenuDismiss?: () => void;
  /** Layout direction of the nested-rail popup for this item. */
  nestedRailAlignment?: AzNestedRailAlignment;
  /** React content rendered in an isolated nested-rail context inside this item. */
  nestedContent?: React.ReactNode;
}

/** Props for `AzNestedRail` — a rail item that opens a secondary popup rail containing child items. */
export interface AzNestedRailProps extends AzNavItemProps {
  /** Layout direction of the popup rail relative to the host item. */
  alignment?: AzNestedRailAlignment;
  /** Child DSL item declarations that populate the nested-rail popup. */
  children: React.ReactNode;
}

/** Builder scope passed to the `hiddenMenu` function prop of `AzRailRelocItemProps` for defining menu entries declaratively. */
export interface HiddenMenuScope {
  /** Adds a tappable label item; `action` is either a route URL string or an onClick callback. */
  listItem: (text: string, action: string | (() => void)) => void;
  /**
   * Adds a text input item to the hidden menu.
   */
  inputItem(hint: string, onValueChange: (value: string) => void): void;
  /**
   * Adds a text input item to the hidden menu with an initial value.
   */
  inputItem(hint: string, initialValue: string, onValueChange: (value: string) => void): void;
}

/** Partial configuration used by the DSL builder helpers to construct an `AzNavItem`. */
export interface AzItemConfig {
  /** Navigation route string. */
  route?: string;
  /** Title shown in the host layout header. */
  screenTitle?: string;
  /** Help text for the info overlay. */
  info?: string;
  /** Whether the item appears in the collapsed rail. */
  isRailItem: boolean;
  /** Disables the item when true. */
  disabled?: boolean;
  /** Marks the item as a collapsible group header. */
  isHost?: boolean;
  /** Marks the item as a child of a host item. */
  isSubItem?: boolean;
  /** Id of the parent host item when `isSubItem` is true. */
  hostId?: string | null;
  /** Classifier tags for filtering. */
  classifiers?: Set<string>;
  /** Called when the item gains focus. */
  onFocus?: () => void;
  /** Custom React content rendered inside the button. */
  content?: any;
  /** Border and icon tint color. */
  color?: string;
  /** Independent text color override. */
  textColor?: string;
  /** Background fill color inside the button shape. */
  fillColor?: string;
  /** Button shape. */
  shape?: AzButtonShape;
}
