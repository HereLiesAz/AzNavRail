import { cachedGet } from './azCache';

/** One entry in the auto-generated About table of contents. */
export interface AzDocEntry {
  title: string;
  path: string;
  downloadUrl: string;
}

const REPO_REGEX = /github\.com[/:]([^/]+)\/([^/?#]+)/;

/** Extracts `[owner, repo]` from a GitHub URL, stripping a trailing `.git`. Null if not GitHub. */
export function parseRepo(repoUrl: string): [string, string] | null {
  const m = REPO_REGEX.exec(repoUrl);
  if (!m) return null;
  const owner = m[1];
  const repo = m[2].replace(/\.git$/, '');
  if (!owner || !repo) return null;
  return [owner, repo];
}

/** Turns `MIGRATION_GUIDE.md` into "Migration Guide". */
export function humanize(fileName: string): string {
  return fileName
    .replace(/\.md$/i, '')
    .replace(/[-_]/g, ' ')
    .split(' ')
    .filter(Boolean)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

/** Parses a GitHub contents-API JSON array into the `.md` entries it contains. */
export function parseContents(json: string): AzDocEntry[] {
  const arr = JSON.parse(json);
  if (!Array.isArray(arr)) return [];
  return arr
    .filter((o) => o && o.type === 'file' && typeof o.name === 'string' && /\.md$/i.test(o.name) && o.download_url)
    .map((o) => ({ title: humanize(o.name), path: o.path ?? o.name, downloadUrl: o.download_url }));
}

/** Orders the TOC: README first, then other root docs, then docs/ — each group alphabetised. */
export function orderToc(root: AzDocEntry[], docs: AzDocEntry[]): AzDocEntry[] {
  const isReadme = (e: AzDocEntry) => e.path.split('/').pop()!.toUpperCase().startsWith('README');
  const readme = root.filter(isReadme);
  const otherRoot = root.filter((e) => !isReadme(e)).sort((a, b) => a.title.localeCompare(b.title));
  const docsSorted = [...docs].sort((a, b) => a.title.localeCompare(b.title));
  return [...readme, ...otherRoot, ...docsSorted];
}

export interface DocsResult {
  entries: AzDocEntry[];
  offline: boolean;
}

/** Lists the repo's root + docs/ markdown files. Empty TOC is a valid (not error) result. */
export async function listDocs(repoUrl: string): Promise<DocsResult> {
  const parsed = parseRepo(repoUrl);
  if (!parsed) throw new Error(`Not a GitHub repo URL: ${repoUrl}`);
  const [owner, repo] = parsed;

  const rootRes = await cachedGet(`https://api.github.com/repos/${owner}/${repo}/contents/`);
  const docsRes = await cachedGet(`https://api.github.com/repos/${owner}/${repo}/contents/docs`);

  if (!rootRes && !docsRes) throw new Error(`Could not reach ${repoUrl}`);

  const root = rootRes ? safeParse(rootRes.body) : [];
  const docs = docsRes ? safeParse(docsRes.body) : [];
  const offline = Boolean(rootRes?.rateLimited || docsRes?.rateLimited);
  return { entries: orderToc(root, docs), offline };
}

function safeParse(body: string): AzDocEntry[] {
  try {
    return parseContents(body);
  } catch {
    return [];
  }
}

/** Fetches the raw markdown for a single TOC entry (cached). */
export async function fetchDoc(entry: AzDocEntry): Promise<string | null> {
  const res = await cachedGet(entry.downloadUrl);
  return res?.body ?? null;
}
