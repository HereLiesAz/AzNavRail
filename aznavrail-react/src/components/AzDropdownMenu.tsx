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
  useWindowDimensions,
  I18nManager,
  ImageSourcePropType,
  ViewStyle,
  LayoutChangeEvent,
} from 'react-native';
import { AzButton } from './AzButton';
import { AzButtonShape, AzDropdownAlignment, AzDropdownDesign } from '../types';
import { parseDropdownAnchor } from '../dropdownPlacement';

/** Context the menu provides so item components can fold it back up on press. */
interface AzDropdownMenuContextValue {
  dismiss: () => void;
  /** The panel design, so items can render rail-style or menu-style. */
  design: AzDropdownDesign;
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
  /** Whether the panel imitates the collapsed rail or the expanded menu. */
  design?: AzDropdownDesign;
  /** Optional fixed width (px) for the panel; follows `design` (100/160) when omitted. */
  menuWidth?: number;
  /** Panel background color. */
  backgroundColor?: string;
  /** Which screen edge the panel pins to (start=left, end=right) and how it drops from the trigger. */
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

/**
 * A tappable menu entry. In a {@link AzDropdownDesign.MENU} panel it renders as a full-width labeled
 * row (the expanded-drawer look); in a {@link AzDropdownDesign.RAIL} panel it renders as a compact
 * {@link AzButton}. Folds the menu on press by default.
 */
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
  const { dismiss, design } = useAzDropdownMenu();
  const press = () => {
    onClick();
    if (closeOnClick) dismiss();
  };
  if (design === AzDropdownDesign.MENU) {
    return (
      <TouchableOpacity
        style={styles.menuRow}
        onPress={enabled ? press : undefined}
        disabled={!enabled}
        accessibilityRole="button"
        accessibilityLabel={text}
      >
        <Text style={[styles.menuRowText, { color: textColor || color || '#6750A4', opacity: enabled ? 1 : 0.5 }]}>
          {text}
        </Text>
      </TouchableOpacity>
    );
  }
  return (
    <AzButton
      text={text}
      onClick={press}
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
 * Drop the icon inline anywhere in your own UI, like {@link AzButton}: it takes a normal slot, like
 * a hamburger button. No host, no settings, no safe zones. Tapping it shows the `children` as a
 * panel styled like the collapsed rail ({@link AzDropdownDesign.RAIL}) or expanded menu
 * ({@link AzDropdownDesign.MENU}) — width-constrained to match — pinned to the left/right screen
 * edge (per `alignment`) and dropping from the trigger. Tapping outside folds it up.
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
  design = AzDropdownDesign.MENU,
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
  // Reactive so the panel re-positions on orientation / split-screen changes.
  const screen = useWindowDimensions();

  // The panel matches the rail/menu it imitates unless an explicit menuWidth is pinned.
  const panelWidth = menuWidth ?? (design === AzDropdownDesign.RAIL ? 100 : 160);

  const panelPosition: ViewStyle = { position: 'absolute' };
  // Horizontal: pin to the edge named by the anchor. `start`/`end` are layout-direction-relative —
  // start hugs the leading edge (left in LTR, right in RTL), end the trailing edge; centre is centred.
  const isRTL = I18nManager.isRTL;
  if (horiz === 'start') {
    panelPosition.left = (isRTL ? screen.width - panelWidth : 0) + offX;
  } else if (horiz === 'end') {
    panelPosition.left = (isRTL ? 0 : screen.width - panelWidth) + offX;
  } else {
    panelPosition.left = (screen.width - panelWidth) / 2 + offX;
  }
  // Vertical: drop from the trigger — top/centre open downward, bottom opens upward.
  if (isBottom) {
    panelPosition.top = anchor.y - panelSize.height + offY;
  } else {
    panelPosition.top = anchor.y + anchor.height + offY;
  }

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
              testID="az-dropdown-panel"
              onLayout={onPanelLayout}
              style={[
                styles.panel,
                panelPosition,
                { backgroundColor, maxHeight: screen.height * 0.8, width: panelWidth },
              ]}
            >
              {/* Stop propagation so taps inside the panel don't dismiss it. */}
              <Pressable>
                <ScrollView
                  contentContainerStyle={
                    design === AzDropdownDesign.MENU
                      ? { alignItems: 'stretch' }
                      : { alignItems: 'center', padding: 8 }
                  }
                >
                  <AzDropdownMenuContext.Provider value={{ dismiss: () => setOpen(false), design }}>
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
  // Expanded-drawer look: full-width labeled row with the menu's 24/12 padding.
  menuRow: {
    width: '100%',
    paddingHorizontal: 24,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  menuRowText: {
    fontSize: 22,
    textAlign: 'center',
  },
});
