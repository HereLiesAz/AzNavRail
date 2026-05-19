import React, { useRef } from 'react';
import {
  Modal,
  PanResponder,
  PanResponderInstance,
  Platform,
  StyleSheet,
  View,
  ViewStyle,
} from 'react-native';

/**
 * Documented analog to the Android `AzNavRailWindowService`.
 *
 * Web/React Native have no true system overlay window (the closest the platform exposes is a
 * fullscreen modal or a portaled element). This component emulates an in-app floating rail:
 *
 * - **Native**: a transparent fullscreen `<Modal>` hosting the rail content. The user can drag
 *   the floating rail anywhere in the modal area. The host app remains interactive only when
 *   `passthrough` is true (touches outside the rail propagate via `pointerEvents="box-none"`).
 * - **Web**: a `position: fixed` element rendered on top of the page, draggable via
 *   `PanResponder` (which is shimmed by `react-native-web` to pointer events).
 *
 * See `KNOWN_GAPS.md` for the parity caveats. True SYSTEM_ALERT_WINDOW behaviour is not
 * reachable from JavaScript on either platform.
 */
export interface AzFloatingRailProps {
  /** When false, the floating rail is unmounted. */
  visible: boolean;
  /** Sheet contents — typically a rail or a draggable widget. */
  children: React.ReactNode;
  /** Fires while the rail is being dragged. Mirrors Android `onRailDrag` / `onOverlayDrag`. */
  onDrag?: (dx: number, dy: number) => void;
  /** Fires when the user finishes dragging and releases the rail. */
  onDragEnd?: () => void;
  /** Fires when the modal is dismissed (native back press). Mirrors Android `onUndock`. */
  onDismiss?: () => void;
  /** Initial position of the floating rail, in screen coordinates. */
  initialX?: number;
  initialY?: number;
  /** Optional style override for the rail container. */
  style?: ViewStyle;
}

export function AzFloatingRail(props: AzFloatingRailProps): React.ReactElement | null {
  const { visible, children, onDrag, onDragEnd, onDismiss, initialX = 16, initialY = 80, style } = props;
  const offsetRef = useRef({ x: initialX, y: initialY });
  const containerRef = useRef<View | null>(null);

  const panResponder: PanResponderInstance = React.useMemo(
    () =>
      PanResponder.create({
        onMoveShouldSetPanResponder: () => true,
        onPanResponderMove: (_e, gesture) => {
          onDrag?.(gesture.dx, gesture.dy);
        },
        onPanResponderRelease: (_e, gesture) => {
          offsetRef.current = {
            x: offsetRef.current.x + gesture.dx,
            y: offsetRef.current.y + gesture.dy,
          };
          onDragEnd?.();
        },
      }),
    [onDrag, onDragEnd],
  );

  if (!visible) return null;

  const railNode = (
    <View
      ref={containerRef}
      {...panResponder.panHandlers}
      style={[
        styles.rail,
        { left: offsetRef.current.x, top: offsetRef.current.y },
        style,
      ]}
    >
      {children}
    </View>
  );

  if (Platform.OS === 'web') {
    return (
      <View style={styles.webOverlay} pointerEvents="box-none">
        {railNode}
      </View>
    );
  }

  return (
    <Modal
      visible={visible}
      transparent
      animationType="none"
      onRequestClose={onDismiss}
    >
      <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
        {railNode}
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  rail: {
    position: 'absolute',
  },
  webOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 9999,
  },
});
