/**
 * Tiny GET-with-cache helper shared by the About reader and the "More from Az" carousel, mirroring
 * the Android `AzHttpCache`. Mitigates GitHub's unauthenticated rate limit via a TTL short-circuit,
 * `ETag`/`If-None-Match` conditional requests, and graceful fallback to the last cached body.
 *
 * Storage is `localStorage` on the web; it falls back to an in-memory map when unavailable (e.g.
 * React Native without a DOM), so callers never need to special-case the platform.
 */
interface CacheEntry {
  body: string;
  etag?: string;
  ts: number;
}

export interface CachedResult {
  body: string;
  fromCache: boolean;
  rateLimited: boolean;
}

const DEFAULT_TTL_MS = 6 * 60 * 60 * 1000; // 6h
const PREFIX = 'aznavrail_about:';
const memory = new Map<string, CacheEntry>();

function readEntry(url: string): CacheEntry | undefined {
  try {
    if (typeof localStorage !== 'undefined') {
      const raw = localStorage.getItem(PREFIX + url);
      return raw ? (JSON.parse(raw) as CacheEntry) : undefined;
    }
  } catch {
    /* fall through to memory */
  }
  return memory.get(url);
}

function writeEntry(url: string, entry: CacheEntry): void {
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(PREFIX + url, JSON.stringify(entry));
      return;
    }
  } catch {
    /* fall through to memory */
  }
  memory.set(url, entry);
}

/**
 * Fetches [url] honoring the cache. Returns null only on a true cold failure (no network response
 * AND no cached body), or a 404 with no cache (so callers can treat e.g. a missing docs/ folder as
 * empty rather than an error).
 */
export async function cachedGet(url: string, ttlMs = DEFAULT_TTL_MS): Promise<CachedResult | null> {
  const cached = readEntry(url);
  if (cached && Date.now() - cached.ts < ttlMs) {
    return { body: cached.body, fromCache: true, rateLimited: false };
  }

  try {
    const headers: Record<string, string> = { Accept: 'application/vnd.github+json' };
    if (cached?.etag) headers['If-None-Match'] = cached.etag;
    const res = await fetch(url, { headers });

    if (res.status === 304 && cached) {
      writeEntry(url, { ...cached, ts: Date.now() });
      return { body: cached.body, fromCache: true, rateLimited: false };
    }
    if (res.ok) {
      const body = await res.text();
      writeEntry(url, { body, etag: res.headers.get('ETag') ?? undefined, ts: Date.now() });
      return { body, fromCache: false, rateLimited: false };
    }
    if (res.status === 404 && !cached) return null;

    const remaining = Number(res.headers.get('X-RateLimit-Remaining'));
    const limited = res.status === 403 || res.status === 429 || remaining === 0;
    return cached ? { body: cached.body, fromCache: true, rateLimited: limited } : null;
  } catch {
    return cached ? { body: cached.body, fromCache: true, rateLimited: false } : null;
  }
}
