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
