import { useSyncExternalStore } from 'react';
import type { AzItemAlert } from '../types';

/**
 * A patch a rail item's declared props can be overridden with, keyed by item id.
 *
 * The write side of the same relationship `AzRailPalette` publishes the other way: there the rail
 * broadcasts state outward to siblings; here a sibling (an `AzPopup`) writes state *into* the item a
 * rail is about to render. Kept outside React's tree for the same reason — an `AzPopup` bound to a
 * rail item is a sibling of the rail, not a descendant, so context alone cannot reach across.
 */
export interface AzItemOverride {
  badge?: string | null;
  persistentBadge?: boolean;
  isLoading?: boolean;
  alert?: AzItemAlert | null;
}

let overrides: Map<string, AzItemOverride> = new Map();
const subscribers = new Set<() => void>();

function notify(): void {
  subscribers.forEach((fn) => fn());
}

function subscribe(onChange: () => void): () => void {
  subscribers.add(onChange);
  return () => {
    subscribers.delete(onChange);
  };
}

function readOverrides(): Map<string, AzItemOverride> {
  return overrides;
}

/** Merges `patch` onto whatever `id` already has overridden. */
export function setItemOverride(id: string, patch: AzItemOverride): void {
  const next = new Map(overrides);
  next.set(id, { ...next.get(id), ...patch });
  overrides = next;
  notify();
}

/** Drops every override on `id`, restoring whatever the DSL declared. */
export function clearItemOverride(id: string): void {
  if (!overrides.has(id)) return;
  const next = new Map(overrides);
  next.delete(id);
  overrides = next;
  notify();
}

/** The live override map, re-rendering the caller whenever any override changes. */
export function useItemOverrides(): Map<string, AzItemOverride> {
  return useSyncExternalStore(subscribe, readOverrides, readOverrides);
}
