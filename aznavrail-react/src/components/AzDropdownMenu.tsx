import React, { createContext, useContext, useRef, useState } from 'react';
import {
  View,
  Text,
  Image,
  Modal,
  Pressable,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
  Dimensions,
  ImageSourcePropType,
  ViewStyle,
  LayoutChangeEvent,
} from 'react-native';
import { AzButton } from './AzButton';
import { AzButtonShape, AzDropdownAlignment } from '../types';
import { parseDropdownAnchor } from '../dropdownPlacement';

/** Context the menu provides so item components can fold it back up on press. */
interface AzDropdownMenuContextValue {
  dismiss: () => void;
}
const AzDropdownMenuContext = createContext<AzDropdownMenuContextValue | null>(null);

/** Hook to fold the enclosing [AzDropdownMenu] from custom children. */
export function useAzDropdownMenu(): AzDropdownMenuContextValue {
  const ctx = useContext(AzDropdownMenuContext);
  if (!ctx) throw new Error('useAzDropdownMenu must be used inside an <AzDropdownMenu>');
  return ctx;
}

export interface AzDropdownMenuProps {
  /** The hamburger icon. When omitted a simple "≡" glyph is drawn. */
  icon?: ImageSourcePropType;
  /** Accessibility label for the trigger. */
  contentDescription?: string;
  /** Diameter of the trigger icon (px). */
  iconSize?: number;
  /** Clip shape for the trigger icon. */
  iconShape?: 'CIRCLE' | 'ROUNDED' | 'NONE';
  /** Optional fixed width (px) for the panel; wraps content when omitted. */
  menuWidth?: number;
  /** Panel background color. */
  backgroundColor?: string;
  /** Where the panel anchors to the icon and which way it unfolds. */
  alignment?: AzDropdownAlignment;
  /** A fine nudge (px) applied to the panel from its anchor. */
  offset?: { x?: number; y?: number };
  /** Optional controlled open-state. When omitted the menu manages its own. */
  expanded?: boolean;
  /** Called whenever the open-state changes. */
  onExpandedChange?: (open: boolean) => void;
  /** Style applied to the trigger container. */
  style?: ViewStyle;
  /** The menu items — `AzDropdownItem`, `AzToggle`, `AzCycler`, `AzDivider`, etc. */
  children?: React.ReactNode;
}

export interface AzDropdownItemProps {
  text: string;
  onClick: () => void;
  shape?: AzButtonShape;
  enabled?: boolean;
  color?: string;
  textColor?: string;
  fillColor?: string;
  /** When true (default) the menu folds up after `onClick`. */
  closeOnClick?: boolean;
}

/** A tappable menu entry rendered as an {@link AzButton}; folds the menu on press by default. */
export const AzDropdownItem: React.FC<AzDropdownItemProps> = ({
  text,
  onClick,
  shape = AzButtonShape.RECTANGLE,
  enabled = true,
  color,
  textColor,
  fillColor,
  closeOnClick = true,
}) => {
  const { dismiss } = useAzDropdownMenu();
  return (
    <AzButton
      text={text}
      onClick={() => {
        onClick();
        if (closeOnClick) dismiss();
      }}
      shape={shape}
      enabled={enabled}
      color={color}
      textColor={textColor}
      fillColor={fillColor}
    />
  );
};

interface Anchor { x: number; y: number; width: number; height: number; }

/**
 * A standalone, hamburger-style drop-down menu — used the usual, expected way.
 *
 * Drop it inline anywhere in your own UI, like {@link AzButton}: no host, no settings, no safe
 * zones. It renders a tappable icon; tapping it shows a panel anchored to the icon holding the
 * `children`. Tapping outside folds it up.
 *
 * ```tsx
 * <AzDropdownMenu>
 *   <AzDropdownItem text="Settings" onClick={openSettings} />
 *   <AzDivider />
 *   <AzDropdownItem text="Sign out" onClick={signOut} />
 * </AzDropdownMenu>
 * ```
 */
export const AzDropdownMenu: React.FC<AzDropdownMenuProps> = ({
  icon,
  contentDescription = 'Menu',
  iconSize = 48,
  iconShape = 'CIRCLE',
  menuWidth,
  backgroundColor = '#ffffff',
  alignment = AzDropdownAlignment.TOP_START,
  offset,
  expanded,
  onExpandedChange,
  style,
  children,
}) => {
  const [internalOpen, setInternalOpen] = useState(false);
  const isOpen = expanded ?? internalOpen;
  const triggerRef = useRef<View>(null);
  const [anchor, setAnchor] = useState<Anchor>({ x: 0, y: 0, width: 0, height: 0 });
  const [panelSize, setPanelSize] = useState({ width: 0, height: 0 });

  const setOpen = (value: boolean) => {
    if (expanded === undefined) setInternalOpen(value);
    onExpandedChange?.(value);
  };

  const openMenu = () => {
    if (triggerRef.current) {
      triggerRef.current.measureInWindow((x, y, width, height) => {
        setAnchor({ x, y, width, height });
        setOpen(true);
      });
    } else {
      setOpen(true);
    }
  };

  const { horiz, isBottom } = parseDropdownAnchor(alignment);
  const offX = offset?.x ?? 0;
  const offY = offset?.y ?? 0;
  const screen = Dimensions.get('window');

  const panelPosition: ViewStyle = { position: 'absolute' };
  // Horizontal edge.
  if (horiz === 'start') {
    panelPosition.left = anchor.x + offX;
  } else if (horiz === 'end') {
    panelPosition.left = anchor.x + anchor.width - panelSize.width + offX;
  } else {
    panelPosition.left = anchor.x + anchor.width / 2 - panelSize.width / 2 + offX;
  }
  // Vertical: top/centre open downward; bottom opens upward.
  if (isBottom) {
    panelPosition.top = anchor.y - panelSize.height + offY;
  } else {
    panelPosition.top = anchor.y + anchor.height + offY;
  }
  // Keep on-screen.
  panelPosition.left = Math.max(0, Math.min(panelPosition.left as number, screen.width - panelSize.width));

  const radius = iconShape === 'CIRCLE' ? iconSize / 2 : iconShape === 'ROUNDED' ? 12 : 0;

  const onPanelLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    if (width !== panelSize.width || height !== panelSize.height) setPanelSize({ width, height });
  };

  return (
    <View style={style}>
      <TouchableOpacity
        ref={triggerRef as React.RefObject<any>}
        onPress={() => (isOpen ? setOpen(false) : openMenu())}
        accessibilityRole="button"
        accessibilityLabel={contentDescription}
        style={{ width: iconSize, height: iconSize, borderRadius: radius, alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}
      >
        {icon ? (
          <Image source={icon} style={{ width: iconSize, height: iconSize }} />
        ) : (
          <Text style={{ fontSize: iconSize * 0.5 }}>≡</Text>
        )}
      </TouchableOpacity>

      {isOpen && (
        <Modal visible transparent animationType="fade" onRequestClose={() => setOpen(false)}>
          <Pressable style={StyleSheet.absoluteFill} onPress={() => setOpen(false)}>
            <View
              onLayout={onPanelLayout}
              style={[
                styles.panel,
                panelPosition,
                { backgroundColor, maxHeight: screen.height * 0.8 },
                menuWidth ? { width: menuWidth } : null,
              ]}
            >
              {/* Stop propagation so taps inside the panel don't dismiss it. */}
              <Pressable>
                <ScrollView contentContainerStyle={{ alignItems: 'center', padding: 8 }}>
                  <AzDropdownMenuContext.Provider value={{ dismiss: () => setOpen(false) }}>
                    {children}
                  </AzDropdownMenuContext.Provider>
                </ScrollView>
              </Pressable>
            </View>
          </Pressable>
        </Modal>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  panel: {
    borderRadius: 12,
    elevation: 8,
    shadowColor: '#000',
    shadowOpacity: 0.18,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 8 },
  },
});
