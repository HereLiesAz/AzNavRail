import React, { useMemo, useRef, useState } from 'react';
import {
  Animated,
  Dimensions,
  PanResponder,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  ViewStyle,
} from 'react-native';

/** Shared metrics for the library's floating windows. */
export const AzWindowDefaults = {
  /** Corner radius of the panel. */
  radius: 12,
  /** Height of the grab bar — a real touch target, not a hairline. */
  chromeHeight: 36,
  /** How much of the window is always kept on screen, however far it is dragged. */
  minVisible: 96,
} as const;

export interface AzWindowProps {
  /** Shown in the grab bar. Blank draws a bare bar — right when the body has a heading already. */
  title?: string;
  /** Border and chrome colour. Pass the rail's accent so a window matches the rail that raised it. */
  accent: string;
  /** Panel fill. */
  surfaceColor?: string;
  /** Whether the grab bar drags the window. */
  movable?: boolean;
  /** Whether the grab bar offers the fold control. */
  minimizable?: boolean;
  /** Starts folded. */
  initiallyMinimized?: boolean;
  /** Close handler; omit to draw no close control. */
  onDismiss?: () => void;
  /** Applied to the window surface — use it to place the window. */
  style?: ViewStyle;
  testID?: string;
  children?: React.ReactNode;
}

/**
 * The library's floating window: a bordered panel with a grab bar, drawn in the rail's own colours.
 *
 * Every panel this library floats over an app — a popup, a hidden menu, anything a developer wants
 * to put in front of the user — is one of these, and they all behave the same way:
 *
 *  - **It moves.** Drag the bar and the window follows, clamped so it can never be lost off-screen.
 *    A panel that lands on top of the thing you needed to read is otherwise a dead end.
 *  - **It minimizes.** Tap the bar's fold control and the window collapses to just that bar, still
 *    where you left it, still one tap from coming back.
 *  - **It closes**, when the caller gave it a way to.
 */
export const AzWindow: React.FC<AzWindowProps> = ({
  title = '',
  accent,
  surfaceColor = '#1A1A1F',
  movable = true,
  minimizable = true,
  initiallyMinimized = false,
  onDismiss,
  style,
  testID,
  children,
}) => {
  const [minimized, setMinimized] = useState(initiallyMinimized);
  const pan = useRef(new Animated.ValueXY({ x: 0, y: 0 })).current;
  const offset = useRef({ x: 0, y: 0 });
  const size = useRef({ width: 0, height: 0 });

  const panResponder = useMemo(
    () =>
      PanResponder.create({
        onMoveShouldSetPanResponder: () => movable,
        onPanResponderMove: (_evt, gesture) => {
          if (!movable) return;
          const win = Dimensions.get('window');
          const keep = AzWindowDefaults.minVisible;
          // Clamp so the window can be pushed to any edge but never entirely off it: at least a
          // title-bar's worth stays inside on every side. A window you can lose is a window you
          // have to reopen.
          const maxX = win.width - keep;
          const minX = Math.min(0, -(size.current.width - keep));
          const maxY = win.height - keep;
          const minY = Math.min(0, -(size.current.height - keep));
          const x = Math.min(maxX, Math.max(minX, offset.current.x + gesture.dx));
          const y = Math.min(maxY, Math.max(minY, offset.current.y + gesture.dy));
          pan.setValue({ x, y });
        },
        onPanResponderRelease: (_evt, gesture) => {
          offset.current = {
            x: offset.current.x + gesture.dx,
            y: offset.current.y + gesture.dy,
          };
        },
      }),
    [movable, pan]
  );

  return (
    <Animated.View
      testID={testID}
      onLayout={(e) => {
        size.current = {
          width: e.nativeEvent.layout.width,
          height: e.nativeEvent.layout.height,
        };
      }}
      style={[
        styles.window,
        { borderColor: accent, backgroundColor: surfaceColor },
        style,
        { transform: pan.getTranslateTransform() },
      ]}
    >
      <View
        style={styles.chrome}
        {...(movable ? panResponder.panHandlers : {})}
      >
        {movable && (
          <View style={styles.grip}>
            <View style={[styles.gripBar, { backgroundColor: accent }]} />
            <View style={[styles.gripBar, { backgroundColor: accent }]} />
          </View>
        )}
        <Text numberOfLines={1} style={[styles.title, { color: accent }]}>
          {title}
        </Text>
        {minimizable && (
          <TouchableOpacity
            accessibilityLabel={minimized ? 'Restore' : 'Minimize'}
            onPress={() => setMinimized((m) => !m)}
            style={styles.control}
          >
            <View
              style={
                minimized
                  ? [styles.restoreMark, { backgroundColor: accent }]
                  : [styles.minimizeMark, { backgroundColor: accent }]
              }
            />
          </TouchableOpacity>
        )}
        {!!onDismiss && (
          <TouchableOpacity
            accessibilityLabel="Close"
            onPress={onDismiss}
            style={styles.control}
          >
            <Text style={[styles.closeMark, { color: accent }]}>×</Text>
          </TouchableOpacity>
        )}
      </View>
      {!minimized && children}
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  window: {
    borderWidth: 2,
    borderRadius: AzWindowDefaults.radius,
    overflow: 'hidden',
  },
  chrome: {
    height: AzWindowDefaults.chromeHeight,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
  },
  grip: { marginRight: 8 },
  gripBar: { width: 18, height: 2, borderRadius: 1, marginVertical: 1.5, opacity: 0.55 },
  title: { flex: 1, fontSize: 13, fontWeight: 'bold' },
  control: {
    width: AzWindowDefaults.chromeHeight,
    height: AzWindowDefaults.chromeHeight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  minimizeMark: { width: 12, height: 2 },
  restoreMark: { width: 12, height: 12, borderRadius: 2 },
  closeMark: { fontSize: 20, lineHeight: 20 },
});

export default AzWindow;
