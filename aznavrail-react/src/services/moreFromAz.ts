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
  /** Optional banner (docs/banner.* in the app's repo) shown at the top of the app info. */
  bannerUrl?: string;
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
      bannerUrl: typeof o.bannerUrl === 'string' && o.bannerUrl ? o.bannerUrl : undefined,
    };
  }
  // Un-baked link object { github?, play?, web? } -> degraded card.
  const anchor = o?.github || o?.play || o?.web;
  if (typeof anchor !== 'string' || !anchor) return null;
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
 * Reads the **baked** "More from Az" manifest from [jsonUrl]. Play/website/WIP resolution is done in
 * CI so this is a pure parse.  After parsing, entries whose `iconUrl` is blank (or the owner's GH
 * avatar) trigger a repo-level walk against raw.githubusercontent.com — standard mipmap paths first,
 * then the OpenGraph social preview.  Entries without a `bannerUrl` also get a `docs/banner.*` walk
 * so the About page can render a banner strip above the active app's info.
 */
export async function fetchMoreFromAz(jsonUrl: string): Promise<MoreFromResult | null> {
  const res = await cachedGet(jsonUrl);
  if (!res) return null;
  let parsed: MoreFromResult;
  try {
    parsed = parse(res.body);
  } catch {
    return null;
  }
  const enriched = await enrichWithRepoAssets(parsed.apps);
  return { version: parsed.version, apps: enriched };
}

const LAUNCHER_ICON_PATHS = [
  'app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp',
  'app/src/main/res/mipmap-xxxhdpi/ic_launcher.png',
  'app/src/main/res/mipmap-xxhdpi/ic_launcher.webp',
  'app/src/main/res/mipmap-xxhdpi/ic_launcher.png',
  'app/src/main/res/mipmap-xhdpi/ic_launcher.webp',
  'app/src/main/res/mipmap-xhdpi/ic_launcher.png',
  'app/src/main/res/mipmap-hdpi/ic_launcher.png',
];

const BANNER_NAMES = [
  'docs/banner.png', 'docs/banner.webp', 'docs/banner.jpg', 'docs/banner.jpeg',
  'docs/Banner.png', 'docs/Banner.webp',
  'docs/hero.png', 'docs/hero.webp',
];

const BRANCHES = ['main', 'master', 'HEAD'];

async function headOk(url: string): Promise<boolean> {
  try {
    // HEAD would be ideal, but on the web CORS blocks preflight for many hosts. GET with cache: 'no-store'
    // (and no-cors mode so we get an opaque response) is the safest cross-platform ping.
    const r = await fetch(url, { method: 'GET', mode: 'no-cors', cache: 'no-store' as any } as any);
    // In `no-cors` mode the status is 0 with type "opaque"; treat that as "reachable" so we don't
    // gate on CORS misconfig. On native platforms (no CORS), we get the real status.
    return (r.type === 'opaque') || (r.status >= 200 && r.status < 400);
  } catch {
    return false;
  }
}

export async function resolveRepoIcon(owner: string, repo: string): Promise<string> {
  for (const branch of BRANCHES) {
    for (const path of LAUNCHER_ICON_PATHS) {
      const url = `https://raw.githubusercontent.com/${owner}/${repo}/${branch}/${path}`;
      if (await headOk(url)) return url;
    }
  }
  return `https://opengraph.githubassets.com/1/${owner}/${repo}`;
}

export async function resolveRepoBanner(owner: string, repo: string): Promise<string | undefined> {
  for (const branch of BRANCHES) {
    for (const name of BANNER_NAMES) {
      const url = `https://raw.githubusercontent.com/${owner}/${repo}/${branch}/${name}`;
      if (await headOk(url)) return url;
    }
  }
  return undefined;
}

export async function enrichWithRepoAssets(apps: AzMoreFromApp[]): Promise<AzMoreFromApp[]> {
  return Promise.all(
    apps.map(async (app) => {
      const gh = app.githubUrl;
      if (!gh) return app;
      const parsed = parseRepo(gh);
      if (!parsed) return app;
      const [owner, repo] = parsed;
      const needsIcon = !app.iconUrl || app.iconUrl.includes('avatars.githubusercontent.com');
      const needsBanner = !app.bannerUrl;
      if (!needsIcon && !needsBanner) return app;
      const [iconUrl, bannerUrl] = await Promise.all([
        needsIcon ? resolveRepoIcon(owner, repo) : Promise.resolve(app.iconUrl),
        needsBanner ? resolveRepoBanner(owner, repo) : Promise.resolve(app.bannerUrl),
      ]);
      return { ...app, iconUrl: iconUrl || app.iconUrl, bannerUrl };
    }),
  );
}
