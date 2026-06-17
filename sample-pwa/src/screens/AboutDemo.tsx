/**
 * Showcases the in-app About reader and the "More from Az" carousel.
 *
 * The rail itself provides both screens — this page just explains how to reach them and how they
 * are configured. Tap **About** in the rail's footer (expand the rail first) to open the in-app
 * markdown reader, which auto-discovers this repo's docs from GitHub. The pinned **More** rail item
 * (enabled in `App.tsx` via `moreRailItem`) opens the apps carousel directly.
 */
export default function AboutDemo() {
  return (
    <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16, maxWidth: 720 }}>
      <h2 style={{ margin: 0 }}>About &amp; More from Az</h2>
      <p style={{ opacity: 0.85 }}>
        Two built-in, themed overlays the rail provides for free:
      </p>

      <section>
        <h3 style={{ marginBottom: 4 }}>In-app About reader</h3>
        <p style={{ opacity: 0.85, marginTop: 0 }}>
          Expand the rail and tap <strong>About</strong> in the footer. The reader auto-discovers the
          markdown docs in the configured repository's root and <code>docs/</code> folder
          (<code>appRepositoryUrl</code>), builds a table of contents, and renders each doc inline. A
          GitHub button is pinned at the bottom. Configure with{' '}
          <code>inAppAbout</code> (set false to open the repo in a browser instead).
        </p>
      </section>

      <section>
        <h3 style={{ marginBottom: 4 }}>More from Az</h3>
        <p style={{ opacity: 0.85, marginTop: 0 }}>
          A carousel of other apps. You just paste <strong>GitHub repo links, one per line</strong>,
          into <code>more-from-az.json</code>; a GitHub Action resolves each repo (name, icon,
          description, a constructed+verified Play link, and the website/PWA from the repo homepage),
          drops WIP apps, sorts Play-first, and bakes the manifest the rail renders. Reach it from the
          About screen's “More from Az” entry or the pinned <strong>More</strong> rail item
          (<code>moreRailItem</code>). <code>version</code> is auto-incremented, so the list refreshes
          without a release.
        </p>
        <p style={{ opacity: 0.6, marginTop: 0, fontSize: 13 }}>
          Because resolution happens in CI, metadata is identical on web and Android — no CORS limits
          and no runtime GitHub rate-limit usage.
        </p>
      </section>
    </div>
  )
}
