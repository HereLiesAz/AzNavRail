import React, { useEffect, useRef, useState } from 'react';
import { Animated, Easing, ViewStyle, StyleProp } from 'react-native';
import { AzDockingSide, AzEasing, AzEntrance, AzExit } from '../types';

const CASCADE_DIST = 20; // px vertical slide for FAB-mode / SlideUp cascade

/**
 * Drives the "closing state" for a panel that wants an exit: returns whether the items should stay
 * mounted given the `open` target. When `open` flips false it stays true for the length of the
 * staggered exit so the items can animate out, then flips false so the caller can tear down. With
 * `exit === None` it tracks `open` exactly (immediate teardown).
 */
export function useAzClosing(
  open: boolean,
  exit: AzExit,
  count: number,
  staggerMs: number,
  durationMs: number,
): boolean {
  const [rendered, setRendered] = useState(open);
  useEffect(() => {
    if (open) {
      setRendered(true);
      return;
    }
    if (exit === AzExit.None) {
      setRendered(false);
      return;
    }
    const total = durationMs + Math.max(0, count - 1) * staggerMs;
    const t = setTimeout(() => setRendered(false), total);
    return () => clearTimeout(t);
  }, [open, exit, count, staggerMs, durationMs]);
  return rendered;
}

export interface AzKineticItemProps {
  index: number;
  count: number;
  /** When true the item plays its entrance; when false (during a closing state) it plays its exit. */
  visible: boolean;
  entrance: AzEntrance;
  exit: AzExit;
  staggerMs: number;
  durationMs: number;
  startAngle: number;
  tiltOnPress: boolean;
  maxTiltDegrees: number;
  dockingSide: AzDockingSide;
  /** FAB / floating mode — no docked edge, so the cascade degrades to a vertical up/down slide. */
  floating?: boolean;
  style?: StyleProp<ViewStyle>;
  children?: React.ReactNode;
}

/**
 * Wraps a menu/rail item in the WP7 kinetic entrance/exit (and optional press-tilt), mirroring the
 * Android `rememberAzKineticModifier`. The animation is driven by `visible`: true → entrance
 * (staggered by `index`), false → exit (reverse-staggered). The tilt only observes pointer events so
 * the wrapped item's own press still fires.
 */
export const AzKineticItem: React.FC<AzKineticItemProps> = ({
  index,
  count,
  visible,
  entrance,
  exit,
  staggerMs,
  durationMs,
  startAngle,
  tiltOnPress,
  maxTiltDegrees,
  dockingSide,
  floating = false,
  style,
  children,
}) => {
  const animates = entrance !== AzEntrance.None || exit !== AzExit.None;
  // 0 = hidden (pre-entrance / exited), 1 = settled.
  const vis = useRef(new Animated.Value(entrance === AzEntrance.None ? 1 : 0)).current;
  const tiltX = useRef(new Animated.Value(0)).current;
  const tiltY = useRef(new Animated.Value(0)).current;

  const easing = Easing.bezier(...AzEasing.Wp7Decelerate);

  useEffect(() => {
    if (!animates) return;
    if (visible) {
      if (entrance === AzEntrance.None) {
        vis.setValue(1);
        return;
      }
      const anim = Animated.timing(vis, {
        toValue: 1,
        duration: durationMs,
        delay: index * staggerMs,
        easing,
        useNativeDriver: true,
      });
      anim.start();
      return () => anim.stop();
    } else {
      if (exit === AzExit.None) return;
      const anim = Animated.timing(vis, {
        toValue: 0,
        duration: durationMs,
        delay: Math.max(0, count - 1 - index) * staggerMs,
        easing,
        useNativeDriver: true,
      });
      anim.start();
      return () => anim.stop();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible]);

  // Out-pose: a turnstile hinge (docked) or a vertical slide (FAB / SlideUp).
  const useTurnstile = !floating && (entrance === AzEntrance.Turnstile || exit === AzExit.Turnstile);
  const useSlide = floating || entrance === AzEntrance.SlideUp;
  const outAngle = useTurnstile ? startAngle : 0;
  const outDist = useSlide ? CASCADE_DIST : 0;

  const opacity = animates ? vis : 1;
  const rotateY = vis.interpolate({ inputRange: [0, 1], outputRange: [`${outAngle}deg`, '0deg'] });
  const translateY = vis.interpolate({ inputRange: [0, 1], outputRange: [outDist, 0] });
  const rotateXTilt = tiltX.interpolate({ inputRange: [-1, 1], outputRange: [`${-maxTiltDegrees}deg`, `${maxTiltDegrees}deg`] });
  const rotateYTilt = tiltY.interpolate({ inputRange: [-1, 1], outputRange: [`${-maxTiltDegrees}deg`, `${maxTiltDegrees}deg`] });

  const springBack = () => {
    Animated.spring(tiltX, { toValue: 0, useNativeDriver: true }).start();
    Animated.spring(tiltY, { toValue: 0, useNativeDriver: true }).start();
  };
  // Web-only press tilt: read the pointer position within the element and tilt toward it. Mouse/touch
  // handlers are passed via `as any` because react-native-web forwards them to the DOM node while the
  // core RN View types don't declare them (no-ops on native).
  const tiltHandlers = tiltOnPress
    ? ({
        onMouseDown: (e: any) => {
          const r = e.currentTarget.getBoundingClientRect?.();
          if (!r) return;
          const nx = ((e.clientX - r.left) / r.width) * 2 - 1;
          const ny = ((e.clientY - r.top) / r.height) * 2 - 1;
          Animated.spring(tiltY, { toValue: nx, useNativeDriver: true }).start();
          Animated.spring(tiltX, { toValue: -ny, useNativeDriver: true }).start();
        },
        onMouseUp: springBack,
        onMouseLeave: springBack,
        onTouchEnd: springBack,
        onTouchCancel: springBack,
      } as any)
    : {};

  // Hinge the turnstile on the docked edge (web only; ignored on native).
  const transformOrigin = (
    dockingSide === AzDockingSide.RIGHT ? 'right center' : 'left center'
  ) as any;

  return (
    <Animated.View
      {...tiltHandlers}
      style={[
        style,
        {
          opacity,
          transform: [
            { perspective: 600 },
            { rotateY },
            { rotateX: rotateXTilt },
            { rotateY: rotateYTilt },
            { translateY },
          ],
          transformOrigin, // web-only; ignored on native
        } as any,
      ]}
    >
      {children}
    </Animated.View>
  );
};

export interface AzKineticTitleProps {
  /** The active title; changing it restarts the entrance (keyed remount). */
  title: string;
  entrance: AzEntrance;
  durationMs?: number;
  startAngle?: number;
  dockingSide: AzDockingSide;
  style?: StyleProp<ViewStyle>;
  children?: React.ReactNode;
}

/**
 * Plays the big screen-title's WP7 sweep. Remount it with `key={title}` so the entrance replays each
 * time the active screen changes.
 */
export const AzKineticTitle: React.FC<AzKineticTitleProps> = ({
  entrance,
  durationMs = 420,
  startAngle = 70,
  dockingSide,
  style,
  children,
}) => {
  const vis = useRef(new Animated.Value(entrance === AzEntrance.None ? 1 : 0)).current;
  const easing = Easing.bezier(...AzEasing.Wp7Decelerate);

  useEffect(() => {
    if (entrance === AzEntrance.None) {
      vis.setValue(1);
      return;
    }
    const anim = Animated.timing(vis, { toValue: 1, duration: durationMs, easing, useNativeDriver: true });
    anim.start();
    return () => anim.stop();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const outAngle = entrance === AzEntrance.Turnstile ? startAngle : 0;
  const rotateY = vis.interpolate({ inputRange: [0, 1], outputRange: [`${outAngle}deg`, '0deg'] });
  const transformOrigin = (dockingSide === AzDockingSide.RIGHT ? 'right center' : 'left center') as any;

  return (
    <Animated.View
      style={[
        style,
        {
          opacity: entrance === AzEntrance.None ? 1 : vis,
          transform: [{ perspective: 800 }, { rotateY }],
          transformOrigin, // web-only; ignored on native
        } as any,
      ]}
    >
      {children}
    </Animated.View>
  );
};
