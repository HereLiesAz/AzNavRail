import { cachedGet } from './azCache';
import { parseRepo } from './githubDocs';

/** A fully-resolved app for the carousel (baked by CI; the rail just renders it). */
export interface AzMoreFromApp {
  name: string;
  iconUrl: string;
  description: string;
  githubUrl?: string;
  playStoreUrl?: string;
  webUrl?: string;
  /** When true, `webUrl` is a PWA (an "Open" button); otherwise it's a "Website". */
  isPwa?: boolean;
}

/** Stable sort placing apps with a Play link first. */
export function sortPlayFirst(apps: AzMoreFromApp[]): AzMoreFromApp[] {
  return [...apps].sort((a, b) => (a.playStoreUrl ? 0 : 1) - (b.playStoreUrl ? 0 : 1));
}

/** Best-effort display name from a URL (GitHub repo name, else last path segment / host). */
function displayNameFor(url: string): string {
  const repo = parseRepo(url);
  if (repo) return repo[1];
  const cleaned = url.split('?')[0].replace(/\/+$/, '');
  const last = cleaned.split('/').pop() || '';
  if (last && !last.includes('.')) return last;
  return cleaned.replace(/^https?:\/\//, '').split('/')[0];
}

function appFromObject(o: any): AzMoreFromApp | null {
  if (o && typeof o.name === 'string' && o.name) {
    return {
      name: o.name,
      iconUrl: o.iconUrl || '',
      description: o.description || '',
      githubUrl: o.github || undefined,
      playStoreUrl: o.play || undefined,
      webUrl: o.web || undefined,
      isPwa: Boolean(o.isPwa),
    };
  }
  // Un-baked link object { github?, play?, web? } -> degraded card.
  const anchor = o?.github || o?.play || o?.web;
  if (!anchor) return null;
  return {
    name: displayNameFor(anchor),
    iconUrl: '',
    description: '',
    githubUrl: o.github || undefined,
    playStoreUrl: o.play || undefined,
    webUrl: o.web || undefined,
  };
}

function degradedFromUrl(url: string): AzMoreFromApp {
  const name = displayNameFor(url);
  if (url.includes('github.com')) return { name, iconUrl: '', description: '', githubUrl: url };
  if (url.includes('play.google.com')) return { name, iconUrl: '', description: '', playStoreUrl: url };
  return { name, iconUrl: '', description: '', webUrl: url };
}

export interface MoreFromResult {
  version: number;
  apps: AzMoreFromApp[];
}

/** Parses the baked manifest JSON; tolerant of an un-baked URL/link list (degraded cards). */
export function parse(json: string): MoreFromResult {
  const obj = JSON.parse(json);
  const version = typeof obj.version === 'number' ? obj.version : 0;
  const arr: any[] = Array.isArray(obj.apps) ? obj.apps : [];
  const apps: AzMoreFromApp[] = [];
  for (const entry of arr) {
    if (typeof entry === 'string') {
      if (entry.trim()) apps.push(degradedFromUrl(entry.trim()));
    } else {
      const a = appFromObject(entry);
      if (a) apps.push(a);
    }
  }
  return { version, apps: sortPlayFirst(apps) };
}

/**
 * Reads the **baked** "More from Az" manifest from [jsonUrl]. All resolution (Play link, website/PWA,
 * WIP filtering, sorting, name/icon/description) is done in CI, so this is a pure parse — which also
 * closes the previous web CORS gap (no client-side Play/website fetches).
 */
export async function fetchMoreFromAz(jsonUrl: string): Promise<MoreFromResult | null> {
  const res = await cachedGet(jsonUrl);
  if (!res) return null;
  try {
    return parse(res.body);
  } catch {
    return null;
  }
}
