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

/** Parses the manifest into `[version, links]`. */
export function parseLinks(json: string): [number, AzMoreFromLink[]] {
  const obj = JSON.parse(json);
  const version = typeof obj.version === 'number' ? obj.version : 0;
  const apps: AzMoreFromLink[] = Array.isArray(obj.apps)
    ? obj.apps
        .map((a: any) => ({ github: a?.github || undefined, play: a?.play || undefined, web: a?.web || undefined }))
        .filter((a: AzMoreFromLink) => a.github || a.play || a.web)
    : [];
  return [version, apps];
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

/** Resolves one link into an app (richest metadata first: Play, then web/PWA, then GitHub). */
export async function resolve(link: AzMoreFromLink): Promise<AzMoreFromApp | null> {
  return (
    (link.play ? await resolvePlay(link) : null) ??
    (link.web ? await resolveWeb(link) : null) ??
    (await resolveGithub(link))
  );
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
