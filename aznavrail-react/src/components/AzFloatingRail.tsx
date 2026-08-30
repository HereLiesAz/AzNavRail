import React, { useRef, useState } from 'react';
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
 * Props for `AzFloatingRail`, the documented analog to the Android `AzNavRailWindowService`.
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
  /** Initial X position of the floating rail, in screen coordinates. */
  initialX?: number;
  /** Initial Y position of the floating rail, in screen coordinates. */
  initialY?: number;
  /** Optional style override for the rail container. */
  style?: ViewStyle;
}

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
 *
 * @example
 * ```tsx
 * const [floating, setFloating] = useState(false);
 * return (
 *   <AzFloatingRail
 *     visible={floating}
 *     initialX={24}
 *     initialY={120}
 *     onDismiss={() => setFloating(false)}
 *   >
 *     <MyMiniRail />
 *   </AzFloatingRail>
 * );
 * ```
 */
export function AzFloatingRail(
  props: AzFloatingRailProps
): React.ReactElement | null {
  const {
    visible,
    children,
    onDrag,
    onDragEnd,
    onDismiss,
    initialX = 16,
    initialY = 80,
    style,
  } = props;
  // `offsetRef` is the always-current authoritative position, read from `onPanResponderGrant` so a
  // second drag anchors off where the first one actually ended rather than a closure captured when
  // the (deliberately stable) PanResponder was created. `offset` state exists only to force the
  // re-render `offsetRef` alone — a plain ref mutation — could never trigger.
  const [offset, setOffset] = useState({ x: initialX, y: initialY });
  const offsetRef = useRef(offset);
  const dragStartRef = useRef(offset);
  const containerRef = useRef<View | null>(null);

  const moveTo = (x: number, y: number) => {
    offsetRef.current = { x, y };
    setOffset({ x, y });
  };

  const panResponder: PanResponderInstance = React.useMemo(
    () =>
      PanResponder.create({
        onMoveShouldSetPanResponder: () => true,
        onPanResponderGrant: () => {
          dragStartRef.current = offsetRef.current;
        },
        onPanResponderMove: (_e, gesture) => {
          moveTo(
            dragStartRef.current.x + gesture.dx,
            dragStartRef.current.y + gesture.dy
          );
          onDrag?.(gesture.dx, gesture.dy);
        },
        onPanResponderRelease: (_e, gesture) => {
          moveTo(
            dragStartRef.current.x + gesture.dx,
            dragStartRef.current.y + gesture.dy
          );
          onDragEnd?.();
        },
      }),
    // Deliberately stable across drags: everything it needs beyond onDrag/onDragEnd (`moveTo`
    // included) is read from refs at call time, so recreating it — and risking a gesture already in
    // progress — is never necessary just because the position changed.
    [onDrag, onDragEnd]
  );

  if (!visible) return null;

  const railNode = (
    <View
      ref={containerRef}
      {...panResponder.panHandlers}
      style={[styles.rail, { left: offset.x, top: offset.y }, style]}
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
