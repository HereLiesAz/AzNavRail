import React, { useEffect, useMemo, useRef, useState } from 'react';
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
  /**
   * How long an off-screen window is left alone, once released and untouched, before this
   * library rescues it — pulling it back onscreen, or dismissing it if there's basically nothing
   * left to grab. Touching the window (dragging it again) restarts this wait from zero.
   */
  abandonedGraceMs: 5000,
  /**
   * How much of a window's own area must be outside the screen before the abandonment timer
   * decides there is nothing left worth pulling back — and dismisses the window instead.
   */
  nearlyGoneFraction: 0.9,
} as const;

/**
 * Fraction of a `width` x `height` rect at (`left`, `top`) that lies outside the `screenWidth` x
 * `screenHeight` screen — `0` when fully onscreen, `1` when there is no overlap at all. Lets the
 * abandonment timer tell "just drifted past an edge" apart from "essentially gone."
 */
export function offscreenFraction(
  left: number,
  top: number,
  width: number,
  height: number,
  screenWidth: number,
  screenHeight: number
): number {
  const area = width * height;
  if (area <= 0) return 0;
  const visibleWidth = Math.max(
    0,
    Math.min(left + width, screenWidth) - Math.max(left, 0)
  );
  const visibleHeight = Math.max(
    0,
    Math.min(top + height, screenHeight) - Math.max(top, 0)
  );
  return Math.min(1, Math.max(0, 1 - (visibleWidth * visibleHeight) / area));
}

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
  // The window's own on-screen position *before* any drag offset — i.e. wherever the caller placed
  // it via `style`. The clamp has to work in absolute screen coordinates (anchor + offset), or a
  // window placed away from the origin is clamped as if it sat at (0, 0): dragged left, it could
  // vanish off the real left edge long before `offset.x` reached its clamp bound, and dragged right
  // it could be stopped well short of the real right edge. Re-measured on every layout so a caller
  // that repositions the window (rotation, a resized container) keeps a correct anchor.
  const anchor = useRef({ x: 0, y: 0 });
  const viewRef = useRef<View>(null);
  const abandonTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  // Read inside the timeout below instead of `onDismiss` directly, so a caller passing a fresh
  // callback each render never leaves a stale one captured in an already-scheduled timer.
  const onDismissRef = useRef(onDismiss);
  onDismissRef.current = onDismiss;

  const clearAbandonTimer = () => {
    if (abandonTimer.current != null) {
      clearTimeout(abandonTimer.current);
      abandonTimer.current = null;
    }
  };

  // Checks whether the window is currently off the screen edge at all and, if so, (re)starts the
  // grace period before this library rescues it. Called after every settle point — the initial
  // layout, a drag's release or cancellation — never while a drag is actively in progress. Calling
  // it again (from a fresh drag starting) cancels whatever countdown was already running, which is
  // what "touch it again and it resets" means here.
  const scheduleAbandonmentCheck = () => {
    clearAbandonTimer();
    const win = Dimensions.get('window');
    const fraction = offscreenFraction(
      anchor.current.x + offset.current.x,
      anchor.current.y + offset.current.y,
      size.current.width,
      size.current.height,
      win.width,
      win.height
    );
    if (fraction <= 0) return;
    abandonTimer.current = setTimeout(() => {
      abandonTimer.current = null;
      const win2 = Dimensions.get('window');
      const currentFraction = offscreenFraction(
        anchor.current.x + offset.current.x,
        anchor.current.y + offset.current.y,
        size.current.width,
        size.current.height,
        win2.width,
        win2.height
      );
      if (currentFraction <= 0) return;
      if (currentFraction >= AzWindowDefaults.nearlyGoneFraction) {
        onDismissRef.current?.();
        return;
      }
      // Pull the window fully back onscreen — not merely the `minVisible` sliver a live drag
      // guarantees — the same way `AzWindowState.settleFullyOnscreen` does on the Kotlin side.
      const targetX = Math.min(
        Math.max(anchor.current.x + offset.current.x, 0),
        Math.max(win2.width - size.current.width, 0)
      );
      const targetY = Math.min(
        Math.max(anchor.current.y + offset.current.y, 0),
        Math.max(win2.height - size.current.height, 0)
      );
      offset.current = {
        x: targetX - anchor.current.x,
        y: targetY - anchor.current.y,
      };
      pan.setValue(offset.current);
    }, AzWindowDefaults.abandonedGraceMs);
  };

  useEffect(() => clearAbandonTimer, []);

  // Commits the same clamped value the last move already rendered — not the raw, unclamped
  // gesture accumulation — so a drag that ended pinned against an edge doesn't leave `offset`
  // holding a value the window was never actually shown at. Shared by a normal release and a
  // stolen-touch termination, which both need the identical commit.
  const commitRelease = (dx: number, dy: number) => {
    const win = Dimensions.get('window');
    const keep = AzWindowDefaults.minVisible;
    const targetX = anchor.current.x + offset.current.x + dx;
    const targetY = anchor.current.y + offset.current.y + dy;
    const minX = -Math.max(size.current.width - keep, 0);
    const maxX = Math.max(win.width - keep, minX);
    const minY = -Math.max(size.current.height - keep, 0);
    const maxY = Math.max(win.height - keep, minY);
    offset.current = {
      x: Math.min(maxX, Math.max(minX, targetX)) - anchor.current.x,
      y: Math.min(maxY, Math.max(minY, targetY)) - anchor.current.y,
    };
  };

  const panResponder = useMemo(
    () =>
      PanResponder.create({
        onMoveShouldSetPanResponder: () => movable,
        onPanResponderGrant: () => {
          // The window can never be allowed to disappear while a finger is still on it — cancel
          // any pending rescue the moment a new drag starts, gesture or otherwise.
          clearAbandonTimer();
        },
        onPanResponderMove: (_evt, gesture) => {
          if (!movable) return;
          const win = Dimensions.get('window');
          const keep = AzWindowDefaults.minVisible;
          // Clamp so the window can be pushed to any edge but never entirely off it: at least a
          // title-bar's worth stays inside on every side. A window you can lose is a window you
          // have to reopen.
          const targetX = anchor.current.x + offset.current.x + gesture.dx;
          const targetY = anchor.current.y + offset.current.y + gesture.dy;
          const minX = -Math.max(size.current.width - keep, 0);
          const maxX = Math.max(win.width - keep, minX);
          const minY = -Math.max(size.current.height - keep, 0);
          const maxY = Math.max(win.height - keep, minY);
          const clampedX = Math.min(maxX, Math.max(minX, targetX));
          const clampedY = Math.min(maxY, Math.max(minY, targetY));
          pan.setValue({
            x: clampedX - anchor.current.x,
            y: clampedY - anchor.current.y,
          });
        },
        onPanResponderRelease: (_evt, gesture) => {
          commitRelease(gesture.dx, gesture.dy);
          scheduleAbandonmentCheck();
        },
        // The touch can be stolen mid-drag (a parent scroll view, another gesture). Whatever the
        // window's position was at that moment still has to be committed the same way a normal
        // release commits it — left uncommitted, the visible `pan` position and `offset.current`
        // would quietly diverge — and the abandonment timer still has to get its chance to run.
        onPanResponderTerminate: (_evt, gesture) => {
          commitRelease(gesture.dx, gesture.dy);
          scheduleAbandonmentCheck();
        },
      }),
    // `clearAbandonTimer`/`commitRelease`/`scheduleAbandonmentCheck` deliberately excluded: they
    // are new function objects every render but read only refs, so a stale closure is never
    // actually stale — including them would recreate the responder (and risk a gesture already in
    // progress) on every render instead of only when `movable`/`pan` change.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [movable, pan]
  );

  return (
    <Animated.View
      ref={viewRef}
      testID={testID}
      onLayout={(e) => {
        size.current = {
          width: e.nativeEvent.layout.width,
          height: e.nativeEvent.layout.height,
        };
        viewRef.current?.measureInWindow((pageX, pageY) => {
          anchor.current = {
            x: pageX - offset.current.x,
            y: pageY - offset.current.y,
          };
          // A window can open already off-screen (placed by the caller's `style` near an edge) —
          // give it the same grace-period rescue a drag's release does, rather than leaving it
          // stranded until the user happens to find and drag it.
          scheduleAbandonmentCheck();
        });
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
