import { useEffect, useRef, useSyncExternalStore } from 'react';

/**
 * Every place an About affordance can be offered, ranked.
 *
 * The rank is the whole point: when more than one surface could show About, only the highest-ranked
 * one does. The order reads as "the most deliberate placement wins":
 *
 *  1. `RAIL_ITEM_EXPLICIT` — the developer rendered an `AzAboutRailItem` themselves.
 *  2. `RAIL_MENU_FOOTER` — the rail's expanded menu footer, the library's canonical home for it.
 *  3. `DROPDOWN_FOOTER` — a drop-down menu's footer.
 *  4. `RAIL_ITEM_AUTO` — the automatic trailing `?`, which the library added on its own and is
 *     therefore the first to give way.
 */
export enum AzAboutSurface {
  RAIL_ITEM_EXPLICIT = 40,
  RAIL_MENU_FOOTER = 30,
  DROPDOWN_FOOTER = 20,
  RAIL_ITEM_AUTO = 10,
}

/**
 * Keeps track of which surfaces currently offer an About affordance, so the library can render it
 * in exactly one of them.
 *
 * Without this, an app with a rail *and* a drop-down shows "About" twice — once in each footer —
 * plus a third time on the rail's own `?` button. Each surface registers what it *could* show; the
 * registry answers who actually shows it.
 *
 * Registration is by availability, not visibility: a drop-down claims its footer as soon as the
 * drop-down mounts, not when its panel opens. Resolving on availability keeps the answer stable —
 * the rail's `?` doesn't blink out of existence every time a panel opens over it.
 */
class AzAboutRegistry {
  private claims = new Map<number, AzAboutSurface>();
  private listeners = new Set<() => void>();
  private nextKey = 0;

  newKey(): number {
    return ++this.nextKey;
  }

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  claim(key: number, surface: AzAboutSurface): void {
    if (this.claims.get(key) === surface) return;
    this.claims.set(key, surface);
    this.emit();
  }

  release(key: number): void {
    if (this.claims.delete(key)) this.emit();
  }

  /**
   * True when a surface of `surface` rank, registered under `key`, is the one that should draw the
   * About affordance.
   *
   * The caller's own claim is deliberately excluded from the comparison, so the answer is correct on
   * the very first render — before the effect that registers it has run. Ties are shared: two
   * independent rails each keep their own footer About.
   */
  owns(key: number, surface: AzAboutSurface): boolean {
    let best: AzAboutSurface | null = null;
    this.claims.forEach((claimed, claimKey) => {
      if (claimKey === key) return;
      if (best === null || claimed > best) best = claimed;
    });
    return best === null || surface >= best;
  }

  /** A value that changes whenever the claim set does, for `useSyncExternalStore`. */
  version = 0;

  private emit(): void {
    this.version += 1;
    this.listeners.forEach((l) => l());
  }

  /** Drops every claim. Test seam. */
  reset(): void {
    this.claims.clear();
    this.emit();
  }
}

/**
 * The registry every rail and drop-down shares. A module-wide default is what lets two *unrelated*
 * components — a rail here, a drop-down there — agree on who shows About without the developer
 * wiring them together.
 */
export const azAboutRegistry = new AzAboutRegistry();

/**
 * Registers this surface as a place that offers About, and answers whether it is the one that
 * should actually draw it.
 *
 * @param surface Which surface is asking.
 * @param offered Whether this surface has an About affordance to offer at all.
 * @param dedupe When false the caller opts out entirely: it always draws its About and never
 *   suppresses anyone else's.
 */
export function useAzAboutOwnership(
  surface: AzAboutSurface,
  offered: boolean,
  dedupe = true
): boolean {
  const keyRef = useRef<number | null>(null);
  if (keyRef.current === null) keyRef.current = azAboutRegistry.newKey();
  const key = keyRef.current;
  const claiming = offered && dedupe;

  useEffect(() => {
    if (claiming) azAboutRegistry.claim(key, surface);
    else azAboutRegistry.release(key);
    return () => azAboutRegistry.release(key);
  }, [key, surface, claiming]);

  // Re-render this surface whenever anyone else claims or releases, so ownership stays live.
  useSyncExternalStore(
    azAboutRegistry.subscribe,
    () => azAboutRegistry.version,
    () => azAboutRegistry.version
  );

  if (!offered) return false;
  if (!dedupe) return true;
  return azAboutRegistry.owns(key, surface);
}
