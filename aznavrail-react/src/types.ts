export enum AzButtonShape {
  CIRCLE = 'CIRCLE',
  SQUARE = 'SQUARE',
  RECTANGLE = 'RECTANGLE',
  NONE = 'NONE',
}

export enum AzDockingSide {
  LEFT = 'LEFT',
  RIGHT = 'RIGHT',
}

export enum AzHeaderIconShape {
  CIRCLE = 'CIRCLE',
  SQUARE = 'SQUARE',
  ROUNDED = 'ROUNDED',
}

export enum AzNestedRailAlignment {
  VERTICAL = 'VERTICAL',
  HORIZONTAL = 'HORIZONTAL',
}

export enum AzOrientation {
  VERTICAL = 'VERTICAL',
  HORIZONTAL = 'HORIZONTAL',
}

export enum AzVisualSide {
  LEFT = 'LEFT',
  RIGHT = 'RIGHT',
  TOP = 'TOP',
  BOTTOM = 'BOTTOM',
}

export interface AzNavRailSettings {
  displayAppNameInHeader?: boolean;
  packRailButtons?: boolean;
  expandedRailWidth?: number;
  collapsedRailWidth?: number;
  showFooter?: boolean;
  isLoading?: boolean;
  defaultShape?: AzButtonShape;
  enableRailDragging?: boolean;
  dockingSide?: AzDockingSide;
  noMenu?: boolean;
  infoScreen?: boolean;
  onDismissInfoScreen?: () => void;
  activeColor?: string;
  vibrate?: boolean;
  headerIconShape?: AzHeaderIconShape;
  translucentBackground?: string;
  secLoc?: string;
  secLocPort?: number;
  appRepositoryUrl?: string;
  helpEnabled?: boolean;
  usePhysicalDocking?: boolean;
  activeClassifiers?: Set<string>;
  onItemGloballyPositioned?: (id: string, bounds: any) => void;
  helpList?: Record<string, string>;
  tutorials?: Record<string, AzTutorial>;
}

export type AzHighlight =
  | { type: 'Area'; bounds: { x: number; y: number; width: number; height: number } }
  | { type: 'Item'; id: string }
  | { type: 'FullScreen' }
  | { type: 'None' };

export type AzAdvanceCondition =
  | { type: 'Button' }
  | { type: 'TapTarget' }
  | { type: 'TapAnywhere' }
  | { type: 'Event'; name: string };

export interface AzCard {
  title: string;
  text: string;
  highlight?: AzHighlight;
  advanceCondition?: AzAdvanceCondition;
  actionText?: string;
  onAction?: () => void;
  branches?: Record<string, string>;
  mediaContent?: () => React.ReactNode;
  checklistItems?: string[];
}

export interface AzScene {
  id: string;
  content: () => React.ReactNode;
  cards: AzCard[];
  onComplete?: () => void;
  branchVar?: string;
  branches?: Record<string, string>;
}

export interface AzTutorial {
  scenes: AzScene[];
  onComplete?: () => void;
  onSkip?: () => void;
}

export interface AzTutorialController {
  activeTutorialId: string | null;
  readTutorials: string[];
  currentVariables: Record<string, any>;
  pendingEvent: string | null;
  startTutorial: (id: string, variables?: Record<string, any>) => void;
  endTutorial: () => void;
  markTutorialRead: (id: string) => void;
  isTutorialRead: (id: string) => boolean;
  fireEvent: (name: string) => void;
  consumeEvent: () => void;
}

export interface HiddenMenuItem {
  id: string;
  text: string;
  menuText?: string;
  route?: string;
  isInput?: boolean;
  hint?: string;
  initialValue?: string;
  onClick?: () => void;
  onValueChange?: (value: string) => void;
}

export interface AzNavItem {
  id: string;
  text: string;
  menuText?: string;
  route?: string;
  screenTitle?: string;
  isRailItem: boolean;
  color?: string;
  textColor?: string;
  fillColor?: string;
  isToggle: boolean;
  isChecked?: boolean;
  toggleOnText: string;
  toggleOffText: string;
  menuToggleOnText?: string;
  menuToggleOffText?: string;
  isCycler: boolean;
  options?: string[];
  menuOptions?: string[];
  selectedOption?: string;
  isDivider: boolean;
  collapseOnClick: boolean;
  shape: AzButtonShape;
  disabled: boolean;
  disabledOptions?: string[];
  isHost: boolean;
  isSubItem: boolean;
  hostId?: string;
  isExpanded: boolean;
  onClick?: () => void;
  onFocus?: () => void;
  // Reloc Item properties
  isRelocItem?: boolean;
  hiddenMenu?: HiddenMenuItem[];
  forceHiddenMenuOpen?: boolean;
  onHiddenMenuDismiss?: () => void;
  onRelocate?: (fromIndex: number, toIndex: number, newOrder: string[]) => void;
  // Info/Help
  info?: string;
  // New properties for parity
  classifiers?: Set<string>;
  content?: any;
  isNestedRail?: boolean;
  keepNestedRailOpen?: boolean;
  isHelpItem?: boolean;
  nestedRailAlignment?: AzNestedRailAlignment;
  nestedRailItems?: AzNavItem[];
  nestedRailSettings?: any;
}

export interface AzNavItemProps {
  id: string;
  text: string;
  menuText?: string;
  route?: string;
  screenTitle?: string;
  disabled?: boolean;
  onClick?: () => void;
  onFocus?: () => void;
  color?: string;
  textColor?: string;
  fillColor?: string;
  shape?: AzButtonShape;
  info?: string;
  content?: any;
  classifiers?: Set<string>;
}

export interface AzToggleProps extends AzNavItemProps {
  isChecked: boolean;
  toggleOnText: string;
  toggleOffText: string;
  menuToggleOnText?: string;
  menuToggleOffText?: string;
}

export interface AzCyclerProps extends AzNavItemProps {
  options: string[];
  menuOptions?: string[];
  selectedOption: string;
  disabledOptions?: string[];
}

export interface AzHostItemProps extends AzNavItemProps {}

export interface AzSubItemProps extends AzNavItemProps {
  hostId: string;
}

export interface AzSubToggleProps extends AzToggleProps {
  hostId: string;
}

export interface AzSubCyclerProps extends AzCyclerProps {
  hostId: string;
}

export interface AzRailRelocItemProps extends AzSubItemProps {
    onRelocate?: (fromIndex: number, toIndex: number, newOrder: string[]) => void;
    hiddenMenu?: { text: string; onClick: () => void }[] | ((scope: HiddenMenuScope) => void);
    forceHiddenMenuOpen?: boolean;
    onHiddenMenuDismiss?: () => void;
    nestedRailAlignment?: AzNestedRailAlignment;
    nestedContent?: React.ReactNode;
}

export interface AzNestedRailProps extends AzNavItemProps {
    alignment?: AzNestedRailAlignment;
    children: React.ReactNode;
}

export interface HiddenMenuScope {
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

export interface AzItemConfig {
  route?: string;
  screenTitle?: string;
  info?: string;
  isRailItem: boolean;
  disabled?: boolean;
  isHost?: boolean;
  isSubItem?: boolean;
  hostId?: string | null;
  classifiers?: Set<string>;
  onFocus?: () => void;
  content?: any;
  color?: string;
  textColor?: string;
  fillColor?: string;
  shape?: AzButtonShape;
}
