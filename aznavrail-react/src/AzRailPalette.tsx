import { createContext, useContext } from 'react';
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

export const AzRailPaletteContext = createContext<AzRailPalette>({});

/**
 * The accent the current composition should draw itself in: the host rail's accent when there is
 * one, otherwise `fallback`.
 */
export function useAzAccent(fallback: string = AZ_ACCENT_FALLBACK): string {
  return useContext(AzRailPaletteContext).accent || fallback;
}

/** The host rail's panel colour, or `fallback` when there is no rail. */
export function useAzRailSurface(fallback?: string): string | undefined {
  return useContext(AzRailPaletteContext).surface || fallback;
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
