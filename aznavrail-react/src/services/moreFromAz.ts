import { cachedGet } from './azCache';
import { parseRepo } from './githubDocs';

/** A raw link-only manifest entry. */
export interface AzMoreFromLink {
  github?: string;
  play?: string;
  web?: string;
}

/** A fully-resolved app for the carousel (metadata auto-populated from the link). */
export interface AzMoreFromApp {
  name: string;
  iconUrl: string;
  description: string;
  githubUrl?: string;
  playStoreUrl?: string;
  webUrl?: string;
}

const URL_RE = /https?:\/\/[^\s"'<>,}\]]+/g;

function classifyUrls(urls: string[]): AzMoreFromLink | null {
  const app: AzMoreFromLink = {};
  for (const raw of urls) {
    const u = raw.replace(/[.,;]+$/, '');
    if (u.includes('github.com')) app.github ??= u;
    else if (u.includes('play.google.com')) app.play ??= u;
    else app.web ??= u;
  }
  return app.github || app.play || app.web ? app : null;
}

/** Lenient fallback: pull URLs out of a not-quite-JSON manifest, grouping by `{}` block or line. */
export function parseLinksLoose(raw: string): [number, AzMoreFromLink[]] {
  const version = Number(/"version"\s*:\s*(\d+)/.exec(raw)?.[1]) || 0;
  const links: AzMoreFromLink[] = [];
  const blocks = raw.match(/\{[^{}]*\}/g) || [];
  for (const b of blocks) {
    const app = classifyUrls(b.match(URL_RE) || []);
    if (app) links.push(app);
  }
  if (links.length === 0) {
    for (const line of raw.split('\n')) {
      const app = classifyUrls(line.match(URL_RE) || []);
      if (app) links.push(app);
    }
  }
  return [version, links];
}

/**
 * Parses the manifest into `[version, links]`. Tries strict JSON first; on failure (e.g. bare URLs
 * pasted before CI normalized the file) falls back to a lenient URL scan so the carousel still works.
 */
export function parseLinks(json: string): [number, AzMoreFromLink[]] {
  try {
    const obj = JSON.parse(json);
    const version = typeof obj.version === 'number' ? obj.version : 0;
    const apps: AzMoreFromLink[] = Array.isArray(obj.apps)
      ? obj.apps
          .map((a: any) => ({ github: a?.github || undefined, play: a?.play || undefined, web: a?.web || undefined }))
          .filter((a: AzMoreFromLink) => a.github || a.play || a.web)
      : [];
    return [version, apps];
  } catch {
    return parseLinksLoose(json);
  }
}

/** Extracts an OpenGraph `content` value, tolerant of attribute order. */
export function extractOg(html: string, property: string): string | undefined {
  const a = new RegExp(`<meta[^>]+property=["']og:${property}["'][^>]+content=["']([^"']*)["']`, 'i');
  const b = new RegExp(`<meta[^>]+content=["']([^"']*)["'][^>]+property=["']og:${property}["']`, 'i');
  const raw = a.exec(html)?.[1] ?? b.exec(html)?.[1];
  if (!raw) return undefined;
  // Decode the handful of entities that appear in OG text.
  const decoded = raw
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>');
  return decoded.trim() || undefined;
}

async function resolvePlay(link: AzMoreFromLink): Promise<AzMoreFromApp | null> {
  if (!link.play) return null;
  // NOTE: on the web this is subject to CORS (Play pages send no ACAO header) and will usually fail;
  // GitHub resolution is used as the fallback. Native/Android has no such restriction.
  const res = await cachedGet(link.play);
  if (!res) return null;
  const name = extractOg(res.body, 'title')?.replace(/ - Apps on Google Play$/, '').trim();
  if (!name) return null;
  return {
    name,
    iconUrl: extractOg(res.body, 'image') ?? '',
    description: extractOg(res.body, 'description') ?? '',
    githubUrl: link.github,
    playStoreUrl: link.play,
    webUrl: link.web,
  };
}

async function resolveWeb(link: AzMoreFromLink): Promise<AzMoreFromApp | null> {
  if (!link.web) return null;
  // Subject to CORS on the web build (most sites send no ACAO); resolves natively / when the target
  // sends permissive headers. GitHub remains the fallback.
  const res = await cachedGet(link.web);
  if (!res) return null;
  const name = extractOg(res.body, 'title')?.trim();
  if (!name) return null;
  return {
    name,
    iconUrl: extractOg(res.body, 'image') ?? '',
    description: extractOg(res.body, 'description') ?? '',
    githubUrl: link.github,
    playStoreUrl: link.play,
    webUrl: link.web,
  };
}

async function resolveGithub(link: AzMoreFromLink): Promise<AzMoreFromApp | null> {
  if (!link.github) return null;
  const parsed = parseRepo(link.github);
  if (!parsed) return null;
  const [owner, repo] = parsed;
  const res = await cachedGet(`https://api.github.com/repos/${owner}/${repo}`);
  if (!res) return null;
  try {
    const o = JSON.parse(res.body);
    return {
      name: o.name ?? repo,
      iconUrl: o.owner?.avatar_url ?? '',
      description: o.description ?? '',
      githubUrl: link.github,
      playStoreUrl: link.play,
      webUrl: link.web,
    };
  } catch {
    return null;
  }
}

/**
 * Derives the conventional Play Store URL for a GitHub repo (`com.<owner>.<repo>`, lower-cased).
 * Only used if it actually resolves to a real listing (see {@link resolve}).
 */
export function derivePlayUrl(githubUrl: string): string | undefined {
  const parsed = parseRepo(githubUrl);
  if (!parsed) return undefined;
  const [owner, repo] = parsed;
  return `https://play.google.com/store/apps/details?id=com.${owner.toLowerCase()}.${repo.toLowerCase()}`;
}

/**
 * Resolves one link into an app. Order: explicit Play, derived Play (verified by fetch), web/PWA,
 * then GitHub. (On the web, Play/website fetches are CORS-limited, so GitHub is the usual resolver.)
 */
export async function resolve(link: AzMoreFromLink): Promise<AzMoreFromApp | null> {
  if (link.play) {
    const viaPlay = await resolvePlay(link);
    if (viaPlay) return viaPlay;
  }
  if (!link.play && link.github) {
    const derived = derivePlayUrl(link.github);
    if (derived) {
      const viaDerived = await resolvePlay({ ...link, play: derived });
      if (viaDerived) return viaDerived;
    }
  }
  if (link.web) {
    const viaWeb = await resolveWeb(link);
    if (viaWeb) return viaWeb;
  }
  return resolveGithub(link);
}

export interface MoreFromResult {
  version: number;
  apps: AzMoreFromApp[];
}

/** Fetches the manifest and resolves every link into a displayable app (failures dropped). */
export async function fetchMoreFromAz(jsonUrl: string): Promise<MoreFromResult | null> {
  const res = await cachedGet(jsonUrl);
  if (!res) return null;
  try {
    const [version, links] = parseLinks(res.body);
    const resolved = await Promise.all(links.map(resolve));
    return { version, apps: resolved.filter((a): a is AzMoreFromApp => a !== null) };
  } catch {
    return null;
  }
}
