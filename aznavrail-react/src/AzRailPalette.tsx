import {
  createContext,
  useContext,
  useEffect,
  useSyncExternalStore,
} from 'react';
import type { AzNavItem } from './types';

/** The default accent every AzNavRail surface falls back to when no rail states a colour. */
export const AZ_ACCENT_FALLBACK = '#6200ee';

/**
 * The colours the rail on screen is actually wearing, published so that everything else the library
 * draws can wear them too.
 *
 * A second (floating) rail, a drop-down menu, the About reader and the Help overlay are all part of
 * the *same* piece of chrome as the rail the user is already looking at. When they fall back to the
 * app's theme instead, they announce themselves as somebody else's UI — which is exactly what a
 * navigation system must never do.
 */
export interface AzRailPalette {
  /** The rail's accent, or undefined when no rail is present. */
  accent?: string;
  /** The rail's panel background, for surfaces drawn over the app. */
  surface?: string;
}

export const AzRailPaletteContext = createContext<AzRailPalette | null>(null);

/**
 * The palette of the last rail that mounted, kept outside React's tree.
 *
 * Context alone is not enough here. On Compose the host provides the palette above *everything* it
 * draws, so a second rail parked in an `onscreen` layer is inside it. In React a second floating
 * rail — or anything the host renders outside `<AzNavRail>`'s own children — is a *sibling* of the
 * rail whose colour it must borrow, and a sibling is somewhere context cannot reach. So the rail
 * also publishes here, and `useAzAccent` falls back to it. Context still wins when present, which is
 * what keeps a nested rail with its own theme from being overwritten by the outer one.
 */
let publishedPalette: AzRailPalette = {};
const paletteSubscribers = new Set<() => void>();

function subscribeToPalette(onChange: () => void): () => void {
  paletteSubscribers.add(onChange);
  return () => {
    paletteSubscribers.delete(onChange);
  };
}

function readPublishedPalette(): AzRailPalette {
  return publishedPalette;
}

/** Publishes [palette] as the ambient rail palette. Called by the rail; no-ops if nothing changed. */
export function publishRailPalette(palette: AzRailPalette): void {
  if (
    publishedPalette.accent === palette.accent &&
    publishedPalette.surface === palette.surface
  ) {
    return;
  }
  publishedPalette = palette;
  paletteSubscribers.forEach((notify) => notify());
}

/**
 * Publishes the rail's palette for the lifetime of the calling component. Used by `AzNavRail` (both
 * builds) alongside the context provider, so siblings can inherit too.
 */
export function usePublishRailPalette(palette: AzRailPalette): void {
  useEffect(() => {
    publishRailPalette(palette);
  }, [palette]);
}

/** The ambient palette: the nearest provider, else the last rail that mounted. */
export function useAzRailPalette(): AzRailPalette {
  const fromContext = useContext(AzRailPaletteContext);
  const published = useSyncExternalStore(
    subscribeToPalette,
    readPublishedPalette,
    readPublishedPalette
  );
  return fromContext ?? published;
}

/**
 * The accent the current tree should draw itself in: the host rail's accent when there is one,
 * otherwise `fallback`.
 */
export function useAzAccent(fallback: string = AZ_ACCENT_FALLBACK): string {
  return useAzRailPalette().accent || fallback;
}

/** The host rail's panel colour, or `fallback` when there is no rail. */
export function useAzRailSurface(fallback?: string): string | undefined {
  return useAzRailPalette().surface || fallback;
}

/**
 * The library's own panel colours, for chrome that must stay legible whatever the host theme is
 * doing. Same ground and ink the About reader uses, so every surface the library puts over the app
 * reads as one piece.
 */
export const AzChromeColors = {
  /** Near-black, very slightly cool, so an accent reads warm against it. */
  Ground: '#101014',
  /** Primary ink on a dark ground. Not pure white — pure white on near-black glares. */
  Ink: '#ECECF0',
  /** Primary ink on a light ground. */
  InkInverse: '#101014',
} as const;

/** Parsed sRGB channels in 0..1, plus alpha. Returns null for anything unrecognised. */
function parseColor(
  color: string
): { r: number; g: number; b: number; a: number } | null {
  const value = color.trim();
  const hex = /^#([0-9a-f]{3,8})$/i.exec(value);
  if (hex) {
    let d = hex[1];
    if (d.length === 3 || d.length === 4) {
      d = d
        .split('')
        .map((c) => c + c)
        .join('');
    }
    if (d.length !== 6 && d.length !== 8) return null;
    return {
      r: parseInt(d.slice(0, 2), 16) / 255,
      g: parseInt(d.slice(2, 4), 16) / 255,
      b: parseInt(d.slice(4, 6), 16) / 255,
      a: d.length === 8 ? parseInt(d.slice(6, 8), 16) / 255 : 1,
    };
  }
  const rgb = /^rgba?\(([^)]+)\)$/i.exec(value);
  if (rgb) {
    const parts = rgb[1].split(',').map((p) => parseFloat(p));
    if (parts.length < 3 || parts.some((n) => Number.isNaN(n))) return null;
    return {
      r: parts[0] / 255,
      g: parts[1] / 255,
      b: parts[2] / 255,
      a: parts.length > 3 ? parts[3] : 1,
    };
  }
  return null;
}

/** Relative luminance per WCAG. Alpha is ignored; composite first if it matters. */
function luminance(color: string): number | null {
  const c = parseColor(color);
  if (!c) return null;
  const channel = (v: number) =>
    v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
  return 0.2126 * channel(c.r) + 0.7152 * channel(c.g) + 0.0722 * channel(c.b);
}

/** WCAG contrast ratio between two colours, from 1 (identical) to 21 (black on white). */
export function azContrastRatio(a: string, b: string): number {
  const la = luminance(a);
  const lb = luminance(b);
  // An unparseable colour is treated as the worst case, so the guard errs toward plain ink.
  if (la === null || lb === null) return 1;
  return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
}

/** Plain ink — light or dark — guaranteed to read on `background`. */
export function azInkOn(background: string): string {
  const l = luminance(background);
  return l !== null && l > 0.5 ? AzChromeColors.InkInverse : AzChromeColors.Ink;
}

/** `preferred` flattened onto `background`, so a translucent accent is judged as it will be seen. */
function compositeOver(preferred: string, background: string): string {
  const f = parseColor(preferred);
  const b = parseColor(background);
  if (!f || !b || f.a >= 1) return preferred;
  const mix = (x: number, y: number) =>
    Math.round((x * f.a + y * (1 - f.a)) * 255);
  return `rgb(${mix(f.r, b.r)}, ${mix(f.g, b.g)}, ${mix(f.b, b.b)})`;
}

/**
 * `preferred` when it can actually be read on `background`, plain ink otherwise.
 *
 * Only ever applied to a colour the library *chose* (the rail's accent, or the fallback behind it) —
 * never to one a developer named on an item. If you pick the colour you get the colour; if you leave
 * it to the library, the library owes you a legible one. The threshold is WCAG's 3:1 for large text,
 * which is what every label on these panels is.
 */
export function azReadableOn(background: string, preferred?: string): string {
  if (!preferred) return azInkOn(background);
  const flattened = compositeOver(preferred, background);
  return azContrastRatio(flattened, background) >= 3
    ? preferred
    : azInkOn(background);
}

/**
 * The colour of a panel the library drops over the app — a drop-down's dropped menu, and anything
 * else presented as a slice of the rail.
 *
 * The rail's `translucentBackground` supplies the hue; **its alpha is not honoured.** A rail is a
 * strip of buttons over the app and can afford to be see-through; a panel of words cannot. With no
 * rail colour to borrow it lands on `AzChromeColors.Ground`, for the same reason the About reader
 * does: the panel belongs to the rail, not to the host theme.
 */
export function useAzPanelColor(): string {
  const surface = useAzRailPalette().surface;
  if (!surface) return AzChromeColors.Ground;
  const c = parseColor(surface);
  if (!c || c.a >= 1) return surface;
  const to255 = (v: number) => Math.round(v * 255);
  return `rgb(${to255(c.r)}, ${to255(c.g)}, ${to255(c.b)})`;
}

/**
 * The accent a rail reads as.
 *
 * `activeColor` wins when the developer set one. Otherwise it is derived from the rail's own items —
 * the colour most of them are drawn in — because a rail whose every button is white is a white rail,
 * whatever the app's theme happens to say. Returns undefined when the rail expressed no colour at
 * all, leaving the fallback in charge.
 */
export function resolveRailAccent(
  activeColor: string | undefined,
  items: Pick<AzNavItem, 'color'>[]
): string | undefined {
  if (activeColor) return activeColor;
  const counts = new Map<string, number>();
  for (const item of items) {
    if (!item.color) continue;
    counts.set(item.color, (counts.get(item.color) || 0) + 1);
  }
  let best: string | undefined;
  let bestCount = 0;
  for (const [color, count] of counts) {
    if (count > bestCount) {
      best = color;
      bestCount = count;
    }
  }
  return best;
}
