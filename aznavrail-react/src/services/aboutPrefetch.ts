import { useEffect } from 'react';
import {
  AzAuthorProfile,
  AzDocEntry,
  DocsResult,
  listDocs,
  fetchDoc,
  fetchAuthorProfile,
} from './githubDocs';
import { AzMoreFromApp, fetchMoreFromAz } from './moreFromAz';

/**
 * Fetches everything the About reader needs *before* the user asks for it.
 *
 * The About page is a network screen: a GitHub contents listing, a markdown body, and a manifest of
 * other apps. Fetching those when the user taps "About" means the first thing they get is a spinner,
 * for work that could have been done while they were doing something else. So the rail warms all
 * three as it mounts, holds the parsed results for the life of the page, and the reader opens
 * already populated.
 *
 * The bodies themselves still go through `azCache`, which is what keeps this cheap: a warm start
 * revalidates with an etag and usually gets a 304.
 */
class AzAboutPrefetch {
  private docsRepoUrl: string | null = null;
  private docsResult: DocsResult | null = null;
  private moreUrl: string | null = null;
  private moreApps: AzMoreFromApp[] | null = null;
  private bodies = new Map<string, string>();
  private authorProfileValue: AzAuthorProfile | null = null;
  private authorProfileWarmed = false;
  private inFlight = new Map<string, Promise<unknown>>();
  private listeners = new Set<() => void>();

  /** Subscribe to warm-ups landing, so a reader opened mid-flight fills itself in. */
  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  private emit(): void {
    this.listeners.forEach((l) => l());
  }

  /** The warmed table of contents for `repoUrl`, or null when nothing has been fetched for it. */
  docsFor(repoUrl: string): DocsResult | null {
    return this.docsRepoUrl === repoUrl ? this.docsResult : null;
  }

  /** The warmed manifest for `jsonUrl`, or null when it hasn't been fetched yet. */
  moreAppsFor(jsonUrl: string): AzMoreFromApp[] | null {
    return this.moreUrl === jsonUrl ? this.moreApps : null;
  }

  /** The warmed markdown for `entry`, or null when it wasn't among the ones warmed ahead. */
  bodyFor(entry: AzDocEntry): string | null {
    return this.bodies.get(entry.downloadUrl) ?? null;
  }

  /** The author's warmed GitHub profile (avatar + bio), or null when not fetched yet / failed. */
  get authorProfile(): AzAuthorProfile | null {
    return this.authorProfileValue;
  }

  /**
   * Fetches the repo's table of contents, and the body of its first document — almost always the
   * README, and almost always the one opened first. Idempotent per repo.
   */
  async warmDocs(repoUrl: string): Promise<void> {
    if (!repoUrl) return;
    if (this.docsFor(repoUrl)) return;
    const key = `docs:${repoUrl}`;
    const existing = this.inFlight.get(key);
    if (existing) {
      await existing;
      return;
    }
    const run = listDocs(repoUrl)
      .then(async (result) => {
        this.docsRepoUrl = repoUrl;
        this.docsResult = result;
        this.emit();
        const first = result.entries[0];
        if (first) await this.warmDoc(first);
      })
      .catch(() => undefined)
      .finally(() => this.inFlight.delete(key));
    this.inFlight.set(key, run);
    await run;
  }

  /** Fetches one document's markdown so opening it is instantaneous. */
  async warmDoc(entry: AzDocEntry): Promise<void> {
    if (this.bodies.has(entry.downloadUrl)) return;
    const key = `doc:${entry.downloadUrl}`;
    const existing = this.inFlight.get(key);
    if (existing) {
      await existing;
      return;
    }
    const run = fetchDoc(entry)
      .then((body) => {
        if (body) {
          this.bodies.set(entry.downloadUrl, body);
          this.emit();
        }
      })
      .catch(() => undefined)
      .finally(() => this.inFlight.delete(key));
    this.inFlight.set(key, run);
    await run;
  }

  /** Fetches the "More from Az" manifest backing the About page's carousel. */
  async warmMoreFromAz(jsonUrl: string): Promise<void> {
    if (!jsonUrl) return;
    if (this.moreAppsFor(jsonUrl)) return;
    const key = `more:${jsonUrl}`;
    const existing = this.inFlight.get(key);
    if (existing) {
      await existing;
      return;
    }
    const run = fetchMoreFromAz(jsonUrl)
      .then((result) => {
        this.moreUrl = jsonUrl;
        this.moreApps = result?.apps ?? [];
        this.emit();
      })
      .catch(() => undefined)
      .finally(() => this.inFlight.delete(key));
    this.inFlight.set(key, run);
    await run;
  }

  /**
   * Fetches the author's GitHub profile (avatar + bio) for the About page's author header.
   *
   * Idempotent for the session lifetime: the profile doesn't vary per repo or per app, so it is
   * fetched at most once regardless of how many times this is called.
   */
  async warmAuthorProfile(): Promise<void> {
    if (this.authorProfileWarmed) return;
    const key = 'authorProfile';
    const existing = this.inFlight.get(key);
    if (existing) {
      await existing;
      return;
    }
    const run = fetchAuthorProfile()
      .then((profile) => {
        this.authorProfileWarmed = true;
        if (profile) {
          this.authorProfileValue = profile;
          this.emit();
        }
      })
      .catch(() => {
        this.authorProfileWarmed = true;
      })
      .finally(() => this.inFlight.delete(key));
    this.inFlight.set(key, run);
    await run;
  }

  /** Drops everything warmed. Test seam; nothing in the library calls it. */
  clear(): void {
    this.docsRepoUrl = null;
    this.docsResult = null;
    this.moreUrl = null;
    this.moreApps = null;
    this.bodies.clear();
    this.authorProfileValue = null;
    this.authorProfileWarmed = false;
    this.emit();
  }
}

export const azAboutPrefetch = new AzAboutPrefetch();

/**
 * Warms the About reader's content in the background, from wherever the About affordance lives.
 *
 * Costs one contents listing, one markdown body and one manifest per session — all cached,
 * conditional requests — and buys an About page that is already drawn when it opens.
 */
export function useAzAboutWarmup(
  repoUrl: string | undefined,
  moreFromAzEnabled: boolean,
  moreFromAzJsonUrl: string | undefined
): void {
  useEffect(() => {
    if (repoUrl) void azAboutPrefetch.warmDocs(repoUrl);
  }, [repoUrl]);
  useEffect(() => {
    if (moreFromAzEnabled && moreFromAzJsonUrl)
      void azAboutPrefetch.warmMoreFromAz(moreFromAzJsonUrl);
  }, [moreFromAzEnabled, moreFromAzJsonUrl]);
  useEffect(() => {
    void azAboutPrefetch.warmAuthorProfile();
  }, []);
}
