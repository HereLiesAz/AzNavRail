import { parseRepo, humanize, parseContents, orderToc } from '../services/githubDocs';
import { parse } from '../services/moreFromAz';

describe('githubDocs', () => {
  it('parseRepo extracts owner/repo and strips .git', () => {
    expect(parseRepo('https://github.com/HereLiesAz/AzNavRail')).toEqual(['HereLiesAz', 'AzNavRail']);
    expect(parseRepo('https://github.com/HereLiesAz/AzNavRail.git')).toEqual(['HereLiesAz', 'AzNavRail']);
    expect(parseRepo('https://gitlab.com/a/b')).toBeNull();
  });

  it('humanize turns filenames into titles', () => {
    expect(humanize('MIGRATION_GUIDE.md')).toBe('Migration Guide');
    expect(humanize('project-structure.md')).toBe('Project Structure');
  });

  it('parseContents keeps only markdown files', () => {
    const json = JSON.stringify([
      { type: 'file', name: 'README.md', path: 'README.md', download_url: 'u' },
      { type: 'file', name: 'build.gradle', path: 'build.gradle', download_url: 'u' },
      { type: 'dir', name: 'docs', path: 'docs' },
      { type: 'file', name: 'API.md', path: 'API.md', download_url: 'u' },
    ]);
    expect(parseContents(json)).toHaveLength(2);
  });

  it('orderToc puts README first and docs last', () => {
    const root = parseContents(JSON.stringify([
      { type: 'file', name: 'API.md', path: 'API.md', download_url: 'u' },
      { type: 'file', name: 'README.md', path: 'README.md', download_url: 'u' },
    ]));
    const docs = parseContents(JSON.stringify([
      { type: 'file', name: 'DSL.md', path: 'docs/DSL.md', download_url: 'u' },
    ]));
    const toc = orderToc(root, docs);
    expect(toc[0].title).toBe('README');
    expect(toc[toc.length - 1].path).toBe('docs/DSL.md');
  });
});

describe('moreFromAz', () => {
  it('parse reads a baked manifest and sorts Play-first', () => {
    const baked = JSON.stringify({
      version: 7,
      apps: [
        { name: 'NoPlay', iconUrl: '', description: 'd', github: 'https://github.com/a/b' },
        { name: 'HasPlay', iconUrl: 'i', description: 'd', github: 'https://github.com/a/c',
          play: 'https://play.google.com/store/apps/details?id=com.a.c', web: 'https://c.example.com', isPwa: true },
      ],
    });
    const { version, apps } = parse(baked);
    expect(version).toBe(7);
    expect(apps).toHaveLength(2);
    expect(apps[0].name).toBe('HasPlay'); // Play app sorted first
    expect(apps[0].isPwa).toBe(true);
    expect(apps[0].webUrl).toBe('https://c.example.com');
  });

  it('parse tolerates an un-baked github-link list (degraded cards)', () => {
    const raw = JSON.stringify({
      version: 2,
      apps: ['https://github.com/HereLiesAz/AzNavRail', 'https://github.com/HereLiesAz/CueDetat'],
    });
    const { apps } = parse(raw);
    expect(apps).toHaveLength(2);
    expect(apps[0].name).toBe('AzNavRail');
    expect(apps[1].githubUrl).toBe('https://github.com/HereLiesAz/CueDetat');
  });
});
