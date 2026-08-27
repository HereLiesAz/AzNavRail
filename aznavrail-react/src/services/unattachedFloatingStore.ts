import { AzFloatingDock } from './floatingDockMath';

const KEY_PREFIX = 'az_unattached_floating_pos_';

// Optional AsyncStorage (React Native) — used if installed, no-op otherwise. Same optional-require
// idiom as `AzGuidanceController.tsx`'s own persistence.
let AsyncStorage: {
  getItem: (k: string) => Promise<string | null>;
  setItem: (k: string, v: string) => Promise<void>;
} | null = null;
try {
  AsyncStorage = require('@react-native-async-storage/async-storage').default;
} catch {
  /* not installed — fall back to localStorage / no-op */
}

/**
 * A `FLOATING` rail's persisted resting spot — the React twin of Kotlin's `AzFloatingSave`.
 *
 * @param a While `dock` is `FREE`, the x fraction of the window. Otherwise, the rail's sort key
 *   among peers docked to the same edge — a fraction too, so it survives a resize/rotation the
 *   same way.
 * @param b While `dock` is `FREE`, the y fraction of the window. Unused otherwise.
 */
export interface AzFloatingSave {
  dock: AzFloatingDock;
  a: number;
  b: number;
}

function clamp01(v: number): number {
  return Math.min(1, Math.max(0, v));
}

function parse(raw: string | null | undefined): AzFloatingSave | null {
  if (!raw) return null;
  const parts = raw.split(',');
  if (parts.length !== 3) return null;
  const dock = (Object.values(AzFloatingDock) as string[]).includes(parts[0]!)
    ? (parts[0] as AzFloatingDock)
    : null;
  if (!dock) return null;
  const a = Number(parts[1]);
  const b = Number(parts[2]);
  if (Number.isNaN(a) || Number.isNaN(b)) return null;
  return { dock, a: clamp01(a), b: clamp01(b) };
}

function serialize(save: AzFloatingSave): string {
  return `${save.dock},${clamp01(save.a)},${clamp01(save.b)}`;
}

/**
 * Persistence for each `FLOATING` unattached rail's resting spot — which screen edge (if any) it
 * is docked to, and its position along/off that edge. Everything is stored as a **fraction of the
 * window**, so a rail dropped near the right edge comes back near the right edge after a resize or
 * on a different device. Keyed per rail id, since every `FLOATING` rail floats and docks
 * independently. Rail-to-rail docking (two rails snapped to each other, forming a group) is
 * deliberately NOT persisted here — only where each rail personally rests is; see the KDoc on
 * Kotlin's `AzFloatingRailState` for why.
 *
 * Backed by `localStorage` (synchronous, so the first frame can render already-docked) with a
 * fire-and-forget mirror to `@react-native-async-storage/async-storage` when installed, matching
 * the persistence idiom `AzGuidanceController.tsx` already uses elsewhere in this package — no new
 * storage dependency is introduced.
 */
export const AzUnattachedFloatingStore = {
  /** The saved resting spot for `hostId`, read synchronously from `localStorage`, if any. */
  loadFloatingSync(hostId: string): AzFloatingSave | null {
    try {
      if (typeof localStorage !== 'undefined') {
        return parse(localStorage.getItem(KEY_PREFIX + hostId));
      }
    } catch {
      /* ignore */
    }
    return null;
  },

  /**
   * Asynchronously loads `hostId`'s saved resting spot from `AsyncStorage`, for hosts on a native
   * runtime with no `localStorage`. Resolves `null` when nothing is saved, `AsyncStorage` isn't
   * installed, or the read fails.
   */
  async loadFloatingAsync(hostId: string): Promise<AzFloatingSave | null> {
    if (!AsyncStorage) return null;
    try {
      return parse(await AsyncStorage.getItem(KEY_PREFIX + hostId));
    } catch {
      return null;
    }
  },

  /** Saves `hostId`'s resting spot to every available store. */
  saveFloating(hostId: string, save: AzFloatingSave): void {
    const raw = serialize(save);
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(KEY_PREFIX + hostId, raw);
      }
    } catch {
      /* ignore */
    }
    AsyncStorage?.setItem(KEY_PREFIX + hostId, raw).catch(() => {});
  },
};
