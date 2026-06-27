import React from 'react';
import { View, Text, StyleSheet, Dimensions, Pressable } from 'react-native';
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

interface Props {
  resolved: ResolvedInstruction[];
  itemBounds: Record<string, AzItemBounds>;
  accent?: string;
  activeItemId?: string | null;
  targets?: Record<string, () => AzGuideShape | null>;
  /** Called when a tap-advanceable step's callout is pressed. */
  onAdvance?: (key: string) => void;
  /** Host renderer replacing the built-in callout body. */
  renderSlot?: AzGuidanceRenderer | null;
}

const CALLOUT_W = 240;

/**
 * Renders the current guidance instructions as callouts, **each placed next to its own highlight
 * target** (so the user sees, in place, how to accomplish each active goal). Targets may be rail items,
 * the active item, or host-registered arbitrary shapes (circle / rect / path). The overlay never blocks
 * input except on a tap-advanceable info-step callout, which advances the paged edge on press.
 *
 * (Parity note: where the Android overlay punches a true spotlight hole per target, the React overlay
 * draws an accent ring around each target over a light dim. A `Path` target is ringed by its bounding
 * box — multi-hole / path masking isn't portable across React Native primitives.)
 */
export const AzInstructionOverlay: React.FC<Props> = ({
  resolved,
  itemBounds,
  accent = '#6200ee',
  activeItemId = null,
  targets = {},
  onAdvance,
  renderSlot,
}) => {
  if (!resolved || resolved.length === 0) return null;
  const screen = Dimensions.get('window');

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
      <View style={[StyleSheet.absoluteFill, styles.dim]} pointerEvents="none" />
      {resolved.map((r, i) => {
        const ins = r.instruction;
        const highlight = ins.highlight ?? { type: 'None' };
        const shape = resolveShape(highlight, itemBounds, activeItemId, targets);
        const b: AzShapeBounds | null = shape ? shapeBounds(shape) : null;
        const stepKey = edgeStepKey(r.edge);
        const currentStep = r.edge.steps?.[r.stepIndex];
        const tappable = r.stepTotal > 1 && r.stepIndex < r.stepTotal - 1 && currentStep?.advanceWhen == null;
        const onPress = tappable && onAdvance ? () => onAdvance(stepKey) : undefined;

        const body = renderSlot
          ? renderSlot(toSnapshotLite(r, shape, b), b)
          : <Callout ins={ins} accent={accent} stepIndex={r.stepIndex} stepTotal={r.stepTotal} tappable={!!onPress} />;

        if (!b) {
          return (
            <View key={i} style={styles.floatWrap} pointerEvents="box-none">
              <Tappable onPress={onPress}>{body}</Tappable>
            </View>
          );
        }
        const ring = ringStyle(shape!, b, accent);
        const belowHasRoom = b.top + b.height + 96 < screen.height;
        const top = belowHasRoom ? b.top + b.height + 8 : Math.max(0, b.top - 96);
        const left = Math.max(8, Math.min(b.left, screen.width - CALLOUT_W - 8));
        return (
          <React.Fragment key={i}>
            <View pointerEvents="none" style={ring} />
            <View pointerEvents="box-none" style={[styles.calloutWrap, { left, top }]}>
              <Tappable onPress={onPress}>{body}</Tappable>
            </View>
          </React.Fragment>
        );
      })}
    </View>
  );
};

const Tappable: React.FC<{ onPress?: () => void; children: React.ReactNode }> = ({ onPress, children }) => {
  if (!onPress) return <View pointerEvents="none">{children}</View>;
  return <Pressable onPress={onPress}>{children}</Pressable>;
};

/** A bounding ring around the shape (circle gets a circular border; rect/path get a rounded box). */
function ringStyle(shape: AzGuideShape, b: AzShapeBounds, accent: string) {
  const radius = shape.type === 'Circle' ? Math.min(b.width, b.height) / 2 : shape.type === 'Rect' ? (shape.cornerRadius ?? 12) : 12;
  return {
    position: 'absolute' as const,
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
    {stepTotal > 1 ? (
      <View style={styles.stepRow}>
        <Text style={styles.stepCount}>{`${stepIndex + 1} / ${stepTotal}`}</Text>
        {tappable ? <Text style={[styles.tapHint, { color: accent }]}>Tap to continue ▸</Text> : null}
      </View>
    ) : null}
  </View>
);

const styles = StyleSheet.create({
  dim: { backgroundColor: 'rgba(0,0,0,0.35)' },
  calloutWrap: { position: 'absolute', maxWidth: CALLOUT_W },
  floatWrap: { position: 'absolute', left: 0, right: 0, bottom: 24, alignItems: 'center' },
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
