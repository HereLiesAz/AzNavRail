import React from 'react';
import { View, Text, StyleSheet, Dimensions } from 'react-native';
import type { AzGuideHighlight, AzInstruction } from '../guidance/AzStatus';

type Bounds = { x: number; y: number; width: number; height: number };

interface Props {
  instructions: AzInstruction[];
  itemBounds: Record<string, Bounds>;
  accent?: string;
}

const CALLOUT_W = 240;

function resolveBounds(h: AzGuideHighlight | undefined, cache: Record<string, Bounds>): Bounds | null {
  if (!h) return null;
  if (h.type === 'Item') return cache[h.id] || null;
  if (h.type === 'Area') return { x: h.left, y: h.top, width: h.width, height: h.height };
  return null;
}

/**
 * Renders the current guidance instructions as callouts, **each placed next to its own highlight
 * target** (so the user sees, in place, how to accomplish each active goal). The overlay never blocks
 * input — it is `box-none` throughout so taps reach the real controls; there is no Next button, the
 * app's own state change advances guidance.
 *
 * (Parity note: where the Android overlay punches a true spotlight hole per target, the React overlay
 * draws an accent ring around each target over a light dim — multi-hole masking isn't portable across
 * React Native primitives.)
 */
export const AzInstructionOverlay: React.FC<Props> = ({ instructions, itemBounds, accent = '#6200ee' }) => {
  if (!instructions || instructions.length === 0) return null;
  const screen = Dimensions.get('window');

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
      <View style={[StyleSheet.absoluteFill, styles.dim]} pointerEvents="none" />
      {instructions.map((ins, i) => {
        const b = resolveBounds(ins.highlight, itemBounds);
        if (!b) {
          return (
            <View key={i} style={styles.floatWrap} pointerEvents="none">
              <Callout ins={ins} accent={accent} />
            </View>
          );
        }
        const belowHasRoom = b.y + b.height + 96 < screen.height;
        const top = belowHasRoom ? b.y + b.height + 8 : Math.max(0, b.y - 96);
        const left = Math.max(8, Math.min(b.x, screen.width - CALLOUT_W - 8));
        return (
          <React.Fragment key={i}>
            <View
              pointerEvents="none"
              style={[styles.ring, { left: b.x - 4, top: b.y - 4, width: b.width + 8, height: b.height + 8, borderColor: accent }]}
            />
            <View pointerEvents="none" style={[styles.calloutWrap, { left, top }]}>
              <Callout ins={ins} accent={accent} />
            </View>
          </React.Fragment>
        );
      })}
    </View>
  );
};

const Callout: React.FC<{ ins: AzInstruction; accent: string }> = ({ ins, accent }) => (
  <View style={styles.callout}>
    {ins.title ? <Text style={[styles.title, { color: accent }]}>{ins.title}</Text> : null}
    <Text style={styles.text}>{ins.text}</Text>
    {ins.media ? <View style={styles.media}>{ins.media()}</View> : null}
  </View>
);

const styles = StyleSheet.create({
  dim: { backgroundColor: 'rgba(0,0,0,0.35)' },
  ring: { position: 'absolute', borderWidth: 2, borderRadius: 12 },
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
});
