import React, { useEffect, useMemo, useRef } from 'react';
import {
  Animated,
  BackHandler,
  Easing,
  GestureResponderEvent,
  LayoutChangeEvent,
  PanResponder,
  PanResponderInstance,
  Platform,
  PixelRatio,
  Pressable,
  StyleSheet,
  View,
  ViewStyle,
} from 'react-native';
import { AzSheetConfig, AzSheetDetent } from '../types';
import { AzSheetController } from './useAzSheetController';

/** Props for `<AzBottomSheet>`. Mirrors the Android `AzBottomSheet` composable signature. */
export interface AzBottomSheetProps {
  /** State holder produced by `useAzSheetController`. */
  controller: AzSheetController;
  /** Static configuration; see `AzSheetConfig`. */
  config?: AzSheetConfig;
  /** Horizontal-swipe-left callback, gated by `config.horizontalSwipeEnabled`. */
  onSwipeLeft?: () => void;
  /** Horizontal-swipe-right callback, gated by `config.horizontalSwipeEnabled`. */
  onSwipeRight?: () => void;
  /** Sheet content rendered inside the card. Fill it with whatever you like. */
  children?: React.ReactNode;
  /** Optional additional style merged into the sheet card. */
  style?: ViewStyle;
}

const DEFAULTS: Required<Omit<AzSheetConfig, 'backgroundColor'>> & { backgroundColor?: string } = {
  backgroundColor: undefined,
  backgroundAlpha: 0.92,
  scrimColor: '#000000',
  scrimAlpha: 0.32,
  // 28dp keeps the HIDDEN strip touchable; below ~24dp the swipe-up target is too small to land.
  hiddenStripDp: 28,
  peekDp: 56,
  halfFraction: 0.5,
  fullFraction: 0.9,
  dragThresholdDp: 24,
  collapseOnBack: true,
  horizontalSwipeEnabled: false,
  animateInTree: true,
  cornerRadiusDp: 16,
  handleVisible: true,
};

function resolveConfig(config?: AzSheetConfig): typeof DEFAULTS {
  return { ...DEFAULTS, ...(config ?? {}) };
}

function detentHeight(detent: AzSheetDetent, parentHeight: number, cfg: typeof DEFAULTS): number {
  switch (detent) {
    case AzSheetDetent.HIDDEN:
      return cfg.hiddenStripDp;
    case AzSheetDetent.PEEK:
      return cfg.peekDp;
    case AzSheetDetent.HALF:
      return parentHeight * cfg.halfFraction;
    case AzSheetDetent.FULL:
      return parentHeight * cfg.fullFraction;
  }
}

/**
 * Bottom-sheet shell. Renders the four-detent (HIDDEN/PEEK/HALF/FULL) ladder anchored
 * to the bottom of its parent, with a drag handle, accumulated-delta vertical drag,
 * a scrim painted under the HALF/FULL detents, and optional horizontal-swipe callbacks.
 *
 * Works on both React Native and react-native-web — `Animated` and `PanResponder` are
 * imported from `react-native` and the web shim provides them via `react-native-web`,
 * so no platform fork is required.
 *
 * @example
 * ```tsx
 * function MyScreen() {
 *   const sheet = useAzSheetController(AzSheetDetent.PEEK);
 *   return (
 *     <View style={{ flex: 1 }}>
 *       <MyContent />
 *       <AzBottomSheet controller={sheet} config={{ peekDp: 64 }}>
 *         <SheetContent onClose={() => sheet.snapTo(AzSheetDetent.HIDDEN)} />
 *       </AzBottomSheet>
 *     </View>
 *   );
 * }
 * ```
 */
export function AzBottomSheet(props: AzBottomSheetProps): React.ReactElement {
  const { controller, onSwipeLeft, onSwipeRight, children, style } = props;
  const cfg = resolveConfig(props.config);
  const parentHeightRef = useRef<number>(0);
  const animatedHeight = useRef(new Animated.Value(0)).current;
  const dragAccumulatorRef = useRef<number>(0);

  // Compute the target height for the current detent and animate when it changes.
  useEffect(() => {
    const target = detentHeight(controller.detent, parentHeightRef.current, cfg);
    if (cfg.animateInTree && parentHeightRef.current > 0) {
      Animated.timing(animatedHeight, {
        toValue: target,
        duration: 220,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: false,
      }).start();
    } else {
      animatedHeight.setValue(target);
    }
  }, [controller.detent, cfg.animateInTree, animatedHeight, cfg.halfFraction, cfg.fullFraction, cfg.peekDp, cfg.hiddenStripDp]);

  // Hardware back: step down rather than dismiss when collapseOnBack is on (native only).
  useEffect(() => {
    if (Platform.OS === 'web') return undefined;
    if (!cfg.collapseOnBack) return undefined;
    if (typeof BackHandler?.addEventListener !== 'function') return undefined;
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (controller.detent !== AzSheetDetent.HIDDEN) {
        controller.stepDown();
        return true;
      }
      return false;
    });
    return () => sub.remove();
  }, [controller, cfg.collapseOnBack]);

  const panResponder: PanResponderInstance = useMemo(
    () =>
      PanResponder.create({
        onMoveShouldSetPanResponder: (_e: GestureResponderEvent, gesture) => {
          const horizontal = Math.abs(gesture.dx) > Math.abs(gesture.dy);
          if (horizontal) return cfg.horizontalSwipeEnabled;
          return Math.abs(gesture.dy) > 4;
        },
        onPanResponderGrant: () => {
          dragAccumulatorRef.current = 0;
        },
        onPanResponderMove: (_e, gesture) => {
          if (cfg.horizontalSwipeEnabled && Math.abs(gesture.dx) > Math.abs(gesture.dy)) {
            // Horizontal: emit on release, not move.
            return;
          }
          dragAccumulatorRef.current += gesture.dy - dragAccumulatorRef.current;
        },
        onPanResponderRelease: (_e, gesture) => {
          if (
            cfg.horizontalSwipeEnabled &&
            Math.abs(gesture.dx) > Math.abs(gesture.dy) &&
            Math.abs(gesture.dx) > cfg.dragThresholdDp * PixelRatio.get()
          ) {
            if (gesture.dx < 0) onSwipeLeft?.();
            else onSwipeRight?.();
            return;
          }
          const thresholdPx = cfg.dragThresholdDp * PixelRatio.get();
          if (gesture.dy <= -thresholdPx) controller.stepUp();
          else if (gesture.dy >= thresholdPx) controller.stepDown();
          dragAccumulatorRef.current = 0;
        },
      }),
    [cfg.horizontalSwipeEnabled, cfg.dragThresholdDp, controller, onSwipeLeft, onSwipeRight],
  );

  const onParentLayout = (e: LayoutChangeEvent) => {
    const next = e.nativeEvent.layout.height;
    if (next !== parentHeightRef.current) {
      parentHeightRef.current = next;
      animatedHeight.setValue(detentHeight(controller.detent, next, cfg));
    }
  };

  const scrimVisible = controller.detent === AzSheetDetent.HALF || controller.detent === AzSheetDetent.FULL;
  const backgroundColor = cfg.backgroundColor ?? '#FFFFFF';
  const isHidden = controller.detent === AzSheetDetent.HIDDEN;

  return (
    <View style={StyleSheet.absoluteFill} onLayout={onParentLayout} pointerEvents="box-none">
      {scrimVisible ? (
        <Pressable
          style={[
            StyleSheet.absoluteFill,
            { backgroundColor: cfg.scrimColor, opacity: cfg.scrimAlpha },
          ]}
          onPress={() => controller.stepDown()}
        />
      ) : null}
      <Animated.View
        style={[
          styles.sheet,
          {
            height: animatedHeight,
            backgroundColor,
            opacity: cfg.backgroundAlpha,
            borderTopLeftRadius: cfg.cornerRadiusDp,
            borderTopRightRadius: cfg.cornerRadiusDp,
          },
          style,
        ]}
        {...panResponder.panHandlers}
      >
        {/* At HIDDEN, a tap on the strip reveals PEEK — drag is unreliable on a 28dp target. */}
        {isHidden ? (
          <Pressable
            style={StyleSheet.absoluteFill}
            onPress={() => controller.stepUp()}
            accessibilityRole="button"
            accessibilityLabel="Reveal bottom sheet"
          />
        ) : null}
        {cfg.handleVisible ? (
          <View
            style={[styles.handleContainer, isHidden ? styles.handleContainerHidden : null]}
            pointerEvents="none"
          >
            <View style={[styles.handle, isHidden ? styles.handleDim : null]} />
          </View>
        ) : null}
        <View style={styles.body}>{children}</View>
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  sheet: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    overflow: 'hidden',
  },
  handleContainer: {
    alignItems: 'center',
    paddingVertical: 6,
  },
  handleContainerHidden: {
    paddingVertical: 2,
  },
  handle: {
    width: 36,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#888888',
  },
  handleDim: {
    opacity: 0.55,
  },
  body: {
    flex: 1,
  },
});

export { AzSheetDetent } from '../types';
export type { AzSheetController } from './useAzSheetController';
export { useAzSheetController } from './useAzSheetController';
