import React, { useRef, useState } from 'react';
import { View, Text, StyleSheet, Dimensions, PanResponder, type LayoutChangeEvent, type ViewStyle } from 'react-native';
import type {
  AzGuidanceRenderer,
  AzGuideShape,
  AzInstruction,
  AzItemBounds,
  AzShapeBounds,
} from '../guidance/AzStatus';
import { resolveShape, shapeBounds } from '../guidance/AzStatus';
import type { ResolvedInstruction } from '../guidance/AzGuidance';
import { edgeStepKey } from '../guidance/AzGuidance';
import { placeCallout } from '../guidance/AzCalloutPlacement';

interface Props {
  resolved: ResolvedInstruction[];
  itemBounds: Record<string, AzItemBounds>;
  accent?: string;
  activeItemId?: string | null;
  targets?: Record<string, () => AzGuideShape | null>;
  /** Called when a tap-advanceable info-step callout is tapped. */
  onAdvance?: (key: string) => void;
  /** Called when a callout is swiped away (cancels tutorial mode). */
  onSkip?: () => void;
  /** Host renderer replacing the built-in callout body. */
  renderSlot?: AzGuidanceRenderer | null;
}

const CALLOUT_W = 240;
const DEFAULT_H = 84;
const SWIPE_PX = 48;
const TAP_PX = 8;

/**
 * Renders guidance as a **non-blocking coach**: a thin accent outline around each step's target and a
 * small callout placed *near* (never on) that target, with a connector to it. It never dims the screen
 * and never intercepts input outside a callout. **Swiping a callout cancels tutorial mode**; tapping an
 * informational step advances it. Callouts avoid the target, other known UI, each other, and the edges.
 *
 * (Parity note: Android strokes the true shape and draws an arrowhead; React rings the target's bounding
 * box and draws a plain connector line — multi-shape masking / arrowheads aren't portable on RN.)
 */
export const AzInstructionOverlay: React.FC<Props> = ({
  resolved,
  itemBounds,
  accent = '#6200ee',
  activeItemId = null,
  targets = {},
  onAdvance,
  onSkip,
  renderSlot,
}) => {
  const [sizes, setSizes] = useState<Record<number, { width: number; height: number }>>({});
  if (!resolved || resolved.length === 0) return null;
  const screen = Dimensions.get('window');
  const safe: AzShapeBounds = { left: 8, top: 8, width: screen.width - 16, height: screen.height - 16 - 24 };

  const shapes = resolved.map((r) => resolveShape(r.instruction.highlight ?? { type: 'None' }, itemBounds, activeItemId, targets));
  const targetBounds = shapes.map((s) => (s ? shapeBounds(s) : null));
  const itemObstacles: AzShapeBounds[] = Object.values(itemBounds).map((b) => ({ left: b.x, top: b.y, width: b.width, height: b.height }));

  const placedRects: AzShapeBounds[] = [];
  const placements = resolved.map((_, i) => {
    const size = sizes[i] ?? { width: CALLOUT_W, height: DEFAULT_H };
    const obstacles = [
      ...itemObstacles,
      ...(targetBounds.filter((b, j) => j !== i && b != null) as AzShapeBounds[]),
      ...placedRects,
    ];
    const rect = placeCallout(targetBounds[i], size, obstacles, safe);
    placedRects.push(rect);
    return rect;
  });

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
      {resolved.map((r, i) => {
        const shape = shapes[i];
        const b = targetBounds[i];
        const rect = placements[i];
        const measured = sizes[i] != null;
        const stepKey = edgeStepKey(r.edge);
        const currentStep = r.edge.steps?.[r.stepIndex];
        const tappable = r.stepTotal > 1 && r.stepIndex < r.stepTotal - 1 && currentStep?.advanceWhen == null;
        const body = renderSlot
          ? renderSlot(toSnapshotLite(r, shape, b), b)
          : <Callout ins={r.instruction} accent={accent} stepIndex={r.stepIndex} stepTotal={r.stepTotal} tappable={tappable} />;
        return (
          <React.Fragment key={i}>
            {shape && b ? <View pointerEvents="none" style={ringStyle(shape, b, accent)} /> : null}
            {b ? <Connector from={center(rect)} to={center(b)} color={accent} /> : null}
            <CalloutGesture
              onSwipe={() => onSkip?.()}
              onTap={tappable && onAdvance ? () => onAdvance(stepKey) : undefined}
              style={{ position: 'absolute', left: rect.left, top: rect.top, maxWidth: CALLOUT_W, opacity: measured ? 1 : 0 }}
              onLayout={(e: LayoutChangeEvent) => {
                const { width, height } = e.nativeEvent.layout;
                setSizes((prev) => (prev[i] && prev[i].width === width && prev[i].height === height ? prev : { ...prev, [i]: { width, height } }));
              }}
            >
              {body}
            </CalloutGesture>
          </React.Fragment>
        );
      })}
    </View>
  );
};

const center = (r: AzShapeBounds) => ({ x: r.left + r.width / 2, y: r.top + r.height / 2 });

/** A callout wrapper: a swipe past a threshold cancels guidance; a clean tap advances an info step. */
const CalloutGesture: React.FC<{
  onSwipe: () => void;
  onTap?: () => void;
  style: ViewStyle;
  onLayout: (e: LayoutChangeEvent) => void;
  children: React.ReactNode;
}> = ({ onSwipe, onTap, style, onLayout, children }) => {
  const onSwipeRef = useRef(onSwipe);
  onSwipeRef.current = onSwipe;
  const onTapRef = useRef(onTap);
  onTapRef.current = onTap;
  const handlers = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: (_, g) => Math.abs(g.dx) + Math.abs(g.dy) > 4,
      onPanResponderRelease: (_, g) => {
        const dist = Math.hypot(g.dx, g.dy);
        if (dist > SWIPE_PX) onSwipeRef.current();
        else if (dist < TAP_PX) onTapRef.current?.();
      },
    }),
  ).current;
  return (
    <View {...handlers.panHandlers} style={style} onLayout={onLayout}>
      {children}
    </View>
  );
};

/** A connector line from the callout to its target (no arrowhead — see the parity note). */
const Connector: React.FC<{ from: { x: number; y: number }; to: { x: number; y: number }; color: string }> = ({ from, to, color }) => {
  const dx = to.x - from.x;
  const dy = to.y - from.y;
  const len = Math.hypot(dx, dy);
  if (len < 1) return null;
  const angle = (Math.atan2(dy, dx) * 180) / Math.PI;
  const mx = (from.x + to.x) / 2;
  const my = (from.y + to.y) / 2;
  return (
    <View
      pointerEvents="none"
      style={{ position: 'absolute', left: mx - len / 2, top: my - 1, width: len, height: 2, backgroundColor: color, transform: [{ rotate: `${angle}deg` }] }}
    />
  );
};

/** A bounding ring around the shape (circle → circular border; rect/path → rounded box). */
function ringStyle(shape: AzGuideShape, b: AzShapeBounds, accent: string): ViewStyle {
  const radius = shape.type === 'Circle' ? Math.min(b.width, b.height) / 2 : shape.type === 'Rect' ? (shape.cornerRadius ?? 12) : 12;
  return {
    position: 'absolute',
    left: b.left - 4,
    top: b.top - 4,
    width: b.width + 8,
    height: b.height + 8,
    borderWidth: 2,
    borderColor: accent,
    borderRadius: radius + 4,
  };
}

/** Minimal snapshot for the render slot (mirrors the controller's published snapshot). */
function toSnapshotLite(r: ResolvedInstruction, shape: AzGuideShape | null, b: AzShapeBounds | null) {
  const h = r.instruction.highlight ?? { type: 'None' as const };
  return {
    text: r.instruction.text,
    title: r.instruction.title,
    goalId: r.goalId,
    highlight: h,
    targetId: h.type === 'Target' ? h.id : null,
    resolvedShape: shape,
    resolvedBounds: b,
    stepIndex: r.stepIndex,
    stepTotal: r.stepTotal,
    stepKey: edgeStepKey(r.edge),
  };
}

const Callout: React.FC<{ ins: AzInstruction; accent: string; stepIndex: number; stepTotal: number; tappable: boolean }> = ({
  ins,
  accent,
  stepIndex,
  stepTotal,
  tappable,
}) => (
  <View style={styles.callout}>
    {ins.title ? <Text style={[styles.title, { color: accent }]}>{ins.title}</Text> : null}
    <Text style={styles.text}>{ins.text}</Text>
    {ins.media ? <View style={styles.media}>{ins.media()}</View> : null}
    <View style={styles.stepRow}>
      <Text style={styles.stepCount}>{stepTotal > 1 ? `${stepIndex + 1} / ${stepTotal}` : 'swipe to dismiss'}</Text>
      {tappable ? <Text style={[styles.tapHint, { color: accent }]}>Tap to continue ▸</Text> : null}
    </View>
  </View>
);

const styles = StyleSheet.create({
  callout: {
    maxWidth: CALLOUT_W,
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 12,
    elevation: 6,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
  },
  title: { fontWeight: 'bold', marginBottom: 4 },
  text: { color: '#222222' },
  media: { marginTop: 8 },
  stepRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 8 },
  stepCount: { fontSize: 12, color: '#888888' },
  tapHint: { fontSize: 12 },
});
