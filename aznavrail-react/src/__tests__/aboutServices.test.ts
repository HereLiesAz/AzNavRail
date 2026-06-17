import { parseRepo, humanize, parseContents, orderToc } from '../services/githubDocs';
import { parseLinks, extractOg, derivePlayUrl } from '../services/moreFromAz';

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
  it('parseLinks reads version and drops empty entries', () => {
    const [version, links] = parseLinks(
      JSON.stringify({ version: 7, apps: [{ github: 'https://github.com/a/b' }, { play: 'p', web: 'w' }, {}] })
    );
    expect(version).toBe(7);
    expect(links).toHaveLength(2);
    expect(links[1].web).toBe('w');
  });

  it('derivePlayUrl builds the conventional package id from a github repo', () => {
    expect(derivePlayUrl('https://github.com/HereLiesAz/CueDetat')).toBe(
      'https://play.google.com/store/apps/details?id=com.hereliesaz.cuedetat'
    );
    expect(derivePlayUrl('https://gitlab.com/a/b')).toBeUndefined();
  });

  it('parseLinks tolerates pasted bare-URL manifests', () => {
    const messy = `{ "version": 5, "apps": [
      { https://github.com/HereLiesAz/AzNavRail }
      {
        https://github.com/HereLiesAz/CueDetat
        https://play.google.com/store/apps/details?id=com.hereliesaz.cuedetat
      }
      { }
    ] }`;
    const [version, links] = parseLinks(messy);
    expect(version).toBe(5);
    expect(links).toHaveLength(2);
    expect(links[0].github).toBe('https://github.com/HereLiesAz/AzNavRail');
    expect(links[1].play).toContain('id=com.hereliesaz.cuedetat');
  });

  it('extractOg pulls content regardless of attribute order', () => {
    const html =
      '<meta property="og:title" content="My App">' +
      '<meta content="https://img/icon.png" property="og:image">';
    expect(extractOg(html, 'title')).toBe('My App');
    expect(extractOg(html, 'image')).toBe('https://img/icon.png');
    expect(extractOg(html, 'description')).toBeUndefined();
  });
});
