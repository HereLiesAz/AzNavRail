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
  secLoc?: string;
  usePhysicalDocking?: boolean;
  activeClassifiers?: string[];
}

export interface HiddenMenuItem {
  id: string;
  text: string;
  route?: string;
  isInput?: boolean;
  hint?: string;
  onClick?: () => void;
  onValueChange?: (value: string) => void;
}

export interface AzNavItem {
  id: string;
  text: string;
  route?: string;
  screenTitle?: string;
  isRailItem: boolean;
  color?: string;
  isToggle: boolean;
  isChecked?: boolean;
  toggleOnText: string;
  toggleOffText: string;
  isCycler: boolean;
  options?: string[];
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
  onRelocate?: (fromIndex: number, toIndex: number, newOrder: string[]) => void;
  // Info/Help
  info?: string;
  // New properties for parity
  classifiers?: string[];
  content?: any;
  isNestedRail?: boolean;
  nestedRailAlignment?: AzNestedRailAlignment;
  nestedRailItems?: AzNavItem[];
}

export interface AzNavItemProps {
  id: string;
  text?: string;
  route?: string;
  screenTitle?: string;
  disabled?: boolean;
  onClick?: () => void;
  onFocus?: () => void;
  color?: string;
  shape?: AzButtonShape;
  info?: string;
  content?: any;
  classifiers?: string[];
}

export interface AzToggleProps extends AzNavItemProps {
  isChecked: boolean;
  toggleOnText: string;
  toggleOffText: string;
}

export interface AzCyclerProps extends AzNavItemProps {
  options: string[];
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
    nestedRailAlignment?: AzNestedRailAlignment;
    nestedContent?: React.ReactNode;
}

export interface AzNestedRailProps extends AzNavItemProps {
    alignment?: AzNestedRailAlignment;
    children: React.ReactNode;
}

export interface HiddenMenuScope {
    listItem: (text: string, action: string | (() => void)) => void;
    inputItem: (hint: string, onValueChange: (value: string) => void) => void;
}
