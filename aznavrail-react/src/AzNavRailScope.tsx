import React, { useEffect, useContext, useRef } from 'react';
import { AzNavItem, AzButtonShape, AzNavItemProps, AzToggleProps, AzCyclerProps, AzHostItemProps, AzSubItemProps, AzSubToggleProps, AzSubCyclerProps, AzRailRelocItemProps, AzNestedRailProps, AzNestedRailAlignment, HiddenMenuScope, AzHeaderIconShape, AzDockingSide } from './types';

export const AzNavRailContext = React.createContext<{
  register: (item: AzNavItem) => void;
  unregister: (id: string) => void;
  updateSettings: (settings: any) => void;
  defaultShape?: import('./types').AzButtonShape;
  getDividerId: () => string;
  checkId?: (id: string) => void;
} | null>(null);

const useAzItem = (item: AzNavItem) => {
  const context = useContext(AzNavRailContext);
  const previousItem = useRef<AzNavItem | null>(null);

  if (!item.id) {
    throw new Error('Item must have an ID');
  }
  if (item.text && item.text.trim() === '') {
      throw new Error('Item text cannot be blank');
  }
  if (item.isToggle) {
      if ((item.toggleOnText && item.toggleOnText.trim() === '') || (item.toggleOffText && item.toggleOffText.trim() === '')) {
          throw new Error('Toggle texts cannot be blank');
      }
  }
  if (item.isCycler) {
      if (item.options && item.selectedOption && !item.options.includes(item.selectedOption)) {
          throw new Error('Selected option must be in options array');
      }
      if (item.options && item.menuOptions && item.options.length !== item.menuOptions.length) {
          throw new Error('menuOptions must be same size as options');
      }
  }

  useEffect(() => {
    if (!context) return;

    const prev = previousItem.current;

    // Deep equality for ALL properties



    const isSame = prev &&
      prev.id === item.id &&
      prev.text === item.text &&
      prev.disabled === item.disabled &&
      prev.isChecked === item.isChecked &&
      prev.selectedOption === item.selectedOption &&
      prev.menuText === item.menuText &&
      prev.menuToggleOnText === item.menuToggleOnText &&
      prev.menuToggleOffText === item.menuToggleOffText &&
      prev.textColor === item.textColor &&
      prev.fillColor === item.fillColor &&
      JSON.stringify(prev.options) === JSON.stringify(item.options) &&
      JSON.stringify(prev.menuOptions) === JSON.stringify(item.menuOptions) &&
      prev.shape === item.shape &&
      prev.color === item.color &&
      prev.info === item.info &&
      prev.isRelocItem === item.isRelocItem &&
      JSON.stringify(prev.hiddenMenu) === JSON.stringify(item.hiddenMenu) &&
      prev.route === item.route &&
      prev.collapseOnClick === item.collapseOnClick &&
      JSON.stringify(prev.disabledOptions) === JSON.stringify(item.disabledOptions) &&
      prev.hostId === item.hostId &&
      prev.forceHiddenMenuOpen === item.forceHiddenMenuOpen &&
      prev.onHiddenMenuDismiss === item.onHiddenMenuDismiss &&
      JSON.stringify(prev.classifiers) === JSON.stringify(item.classifiers) &&
      prev.content === item.content &&
      prev.isNestedRail === item.isNestedRail &&
      prev.nestedRailAlignment === item.nestedRailAlignment &&
      JSON.stringify(prev.nestedRailItems) === JSON.stringify(item.nestedRailItems) &&
      prev.isHelpItem === item.isHelpItem;

    if (!isSame) {
        context.register(item);
        previousItem.current = item;
    }
  }, [context, item]);

  useEffect(() => {
      if (context) {
          // Instead of immediate unregister on ID change, just return cleanup
          return () => context.unregister(item.id);
      }
      return undefined;
  }, [context, item.id]);

  return null;
};

// --- Component Wrappers ---

export const AzRailItem: React.FC<AzNavItemProps> = (props) => {
  const context = useContext(AzNavRailContext);
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || context?.defaultShape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
  });
  return null;
};

export const AzMenuItem: React.FC<AzNavItemProps> = (props) => {
  const context = useContext(AzNavRailContext);
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || context?.defaultShape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
  });
  return null;
};

export const AzRailToggle: React.FC<AzToggleProps> = (props) => {
  const context = useContext(AzNavRailContext);
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || context?.defaultShape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
  });
  return null;
};

export const AzMenuToggle: React.FC<AzToggleProps> = (props) => {
  const context = useContext(AzNavRailContext);
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || context?.defaultShape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
  });
  return null;
};

export const AzRailCycler: React.FC<AzCyclerProps> = (props) => {
  const context = useContext(AzNavRailContext);
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || context?.defaultShape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
  });
  return null;
};

export const AzMenuCycler: React.FC<AzCyclerProps> = (props) => {
  const context = useContext(AzNavRailContext);
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || context?.defaultShape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
  });
  return null;
};

export const AzDivider: React.FC = () => {
    // ID needed?
    const context = useContext(AzNavRailContext);
    const id = useRef(context?.getDividerId?.() || `divider_${Math.random()}`).current;
    useAzItem({
        id,
        text: '',
        isRailItem: false,
        isToggle: false,
        isCycler: false,
        isDivider: true,
        collapseOnClick: false,
        shape: AzButtonShape.NONE,
        disabled: false,
        isHost: false,
        isSubItem: false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzRailHostItem: React.FC<AzHostItemProps> = (props) => {
  const context = useContext(AzNavRailContext);
    useAzItem({
        ...props,
        isRailItem: true,
        isHost: true,
        isSubItem: false,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: false, // Hosts expand/collapse, don't close rail
        shape: props.shape || context?.defaultShape || AzButtonShape.CIRCLE,
        disabled: props.disabled || false,
        isExpanded: false, // Initial state, managed by parent usually
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzMenuHostItem: React.FC<AzHostItemProps> = (props) => {
  const context = useContext(AzNavRailContext);
    useAzItem({
        ...props,
        isRailItem: false,
        isHost: true,
        isSubItem: false,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: false,
        shape: props.shape || context?.defaultShape || AzButtonShape.CIRCLE,
        disabled: props.disabled || false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzRailSubItem: React.FC<AzSubItemProps> = (props) => {
  const context = useContext(AzNavRailContext);
    useAzItem({
        ...props,
        isRailItem: true,
        isHost: false,
        isSubItem: true,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: true,
        shape: props.shape || context?.defaultShape || AzButtonShape.NONE,
        disabled: props.disabled || false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzMenuSubItem: React.FC<AzSubItemProps> = (props) => {
  const context = useContext(AzNavRailContext);
    useAzItem({
        ...props,
        isRailItem: false,
        isHost: false,
        isSubItem: true,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: true,
        shape: props.shape || context?.defaultShape || AzButtonShape.NONE,
        disabled: props.disabled || false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzRailSubToggle: React.FC<AzSubToggleProps> = (props) => {
  const context = useContext(AzNavRailContext);
    useAzItem({
        ...props,
        isRailItem: true,
        isHost: false,
        isSubItem: true,
        isToggle: true,
        isCycler: false,
        isDivider: false,
        collapseOnClick: true,
        shape: props.shape || context?.defaultShape || AzButtonShape.NONE,
        disabled: props.disabled || false,
        isExpanded: false,
    });
    return null;
};

export const AzMenuSubToggle: React.FC<AzSubToggleProps> = (props) => {
  const context = useContext(AzNavRailContext);
    useAzItem({
        ...props,
        isRailItem: false,
        isHost: false,
        isSubItem: true,
        isToggle: true,
        isCycler: false,
        isDivider: false,
        collapseOnClick: true,
        shape: props.shape || context?.defaultShape || AzButtonShape.NONE,
        disabled: props.disabled || false,
        isExpanded: false,
    });
    return null;
};

export const AzRailSubCycler: React.FC<AzSubCyclerProps> = (props) => {
  const context = useContext(AzNavRailContext);
    useAzItem({
        ...props,
        isRailItem: true,
        isHost: false,
        isSubItem: true,
        isToggle: false,
        isCycler: true,
        isDivider: false,
        collapseOnClick: true,
        shape: props.shape || context?.defaultShape || AzButtonShape.NONE,
        disabled: props.disabled || false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzMenuSubCycler: React.FC<AzSubCyclerProps> = (props) => {
  const context = useContext(AzNavRailContext);
    useAzItem({
        ...props,
        isRailItem: false,
        isHost: false,
        isSubItem: true,
        isToggle: false,
        isCycler: true,
        isDivider: false,
        collapseOnClick: true,
        shape: props.shape || context?.defaultShape || AzButtonShape.NONE,
        disabled: props.disabled || false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzRailRelocItem: React.FC<AzRailRelocItemProps> = (props) => {
    let hiddenMenuItems: any[] = [];
    if (props.hiddenMenu) {
        if (typeof props.hiddenMenu === 'function') {
            const scope: HiddenMenuScope = {
                listItem: (text, action) => {
                    if (typeof action === 'string') {
                        hiddenMenuItems.push({ id: `hidden_${hiddenMenuItems.length}`, text, route: action });
                    } else {
                        hiddenMenuItems.push({ id: `hidden_${hiddenMenuItems.length}`, text, onClick: action });
                    }
                },
                inputItem: (hint: string, ...args: any[]) => {
                    let initialValue = '';
                    let onValueChange: (value: string) => void;
                    if (args.length === 1 && typeof args[0] === 'function') {
                        onValueChange = args[0];
                    } else if (args.length === 2 && typeof args[0] === 'string' && typeof args[1] === 'function') {
                        initialValue = args[0];
                        onValueChange = args[1];
                    } else {
                        throw new Error("inputItem requires valid arguments matching overloaded signatures.");
                    }
                    hiddenMenuItems.push({ id: `input_${hiddenMenuItems.length}`, text: '', isInput: true, hint, initialValue, onValueChange });
                }
            };
            props.hiddenMenu(scope);
        } else {
            throw new Error('hiddenMenu must be a scope builder function, not an array');
        }
    }

    useAzItem({
        ...props,
        text: props.text || '',
        isRailItem: true,
        isHost: false,
        isSubItem: true,
        isRelocItem: true,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: false,
        shape: props.shape || AzButtonShape.NONE,
        disabled: props.disabled || false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
        hiddenMenu: hiddenMenuItems,
        forceHiddenMenuOpen: props.forceHiddenMenuOpen,
        onHiddenMenuDismiss: props.onHiddenMenuDismiss,
        nestedRailAlignment: props.nestedRailAlignment || AzNestedRailAlignment.VERTICAL,
    });
    return (
        <AzNavRailContext.Consumer>
            {(ctx) => (
                <AzNavRailContext.Provider value={ctx}>
                    {props.nestedContent}
                </AzNavRailContext.Provider>
            )}
        </AzNavRailContext.Consumer>
    );
};

export const AzSettings: React.FC<{ appRepositoryUrl?: string }> = (props) => {
    const context = useContext(AzNavRailContext);
    const propsJson = JSON.stringify(props);
    useEffect(() => {
        if (context) {
            context.updateSettings(props);
        }
    }, [context, propsJson]);
    return null;
};

export const AzTheme: React.FC<{ defaultShape?: AzButtonShape; activeColor?: string; headerIconShape?: AzHeaderIconShape; translucentBackground?: string }> = (props) => {
    const context = useContext(AzNavRailContext);
    const propsJson = JSON.stringify(props);
    useEffect(() => {
        if (context) {
            context.updateSettings(props);
        }
    }, [context, propsJson]);
    return null;
};

export const AzConfig: React.FC<{ displayAppNameInHeader?: boolean; packRailButtons?: boolean; expandedRailWidth?: number; collapsedRailWidth?: number; dockingSide?: AzDockingSide }> = (props) => {
    const context = useContext(AzNavRailContext);
    const propsJson = JSON.stringify(props);
    useEffect(() => {
        if (context) {
            context.updateSettings(props);
        }
    }, [context, propsJson]);
    return null;
};

export const AzAdvanced: React.FC<{ enableRailDragging?: boolean; usePhysicalDocking?: boolean; noMenu?: boolean; showFooter?: boolean; infoScreen?: boolean; onDismissInfoScreen?: () => void; activeClassifiers?: string[]; vibrate?: boolean; secLoc?: string }> = (props) => {
    const context = useContext(AzNavRailContext);
    const propsJson = JSON.stringify(props);
    useEffect(() => {
        if (context) {
            context.updateSettings(props);
        }
    }, [context, propsJson]);
    return null;
};

export const AzNestedRail: React.FC<AzNestedRailProps> = (props) => {
  const context = useContext(AzNavRailContext);
    useAzItem({
        ...props,
        text: props.text || '',
        isRailItem: true,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: false,
        shape: props.shape || context?.defaultShape || AzButtonShape.CIRCLE,
        disabled: props.disabled || false,
        isHost: false,
        isSubItem: false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
        isNestedRail: true,
        nestedRailAlignment: props.alignment || AzNestedRailAlignment.VERTICAL,
    });
    return (
        <AzNavRailContext.Consumer>
            {(ctx) => (
                <AzNavRailContext.Provider value={ctx}>
                    {props.children}
                </AzNavRailContext.Provider>
            )}
        </AzNavRailContext.Consumer>
    );
};

export const AzHelpRailItem: React.FC<AzNavItemProps> = (props) => {
  const context = useContext(AzNavRailContext);
  useAzItem({
    ...props,
    isRailItem: true,
    isHelpItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || context?.defaultShape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
  } as AzNavItem);
  return null;
};

export const AzHelpSubItem: React.FC<AzSubItemProps> = (props) => {
  if (!props.hostId) {
    throw new Error("AzHelpSubItem requires a valid hostId");
  }
  const context = useContext(AzNavRailContext);
  useAzItem({
    ...props,
    isRailItem: true,
    isHelpItem: true,
    isSubItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || context?.defaultShape || AzButtonShape.NONE,
    disabled: props.disabled || false,
    isHost: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
  } as AzNavItem);
  return null;
};
