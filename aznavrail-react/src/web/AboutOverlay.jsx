import React, { useEffect, useState } from 'react';
import './AboutOverlay.css';
import AzMarkdownWeb from './AzMarkdownWeb';
import MoreFromAzOverlay from './MoreFromAzOverlay';
import { listDocs, fetchDoc } from '../services/githubDocs';

/**
 * Full-screen in-app About reader for the web. Auto-discovers the repo's markdown docs
 * (root + docs/), lists them as a themed table of contents, and renders the selected doc inline.
 * A "View on GitHub" button sits pinned at the bottom with extra spacing; an optional "More from Az"
 * entry opens the author's other-apps carousel. Themed from `settings.activeColor` /
 * `settings.translucentBackground` to match the rail.
 */
export default function AboutOverlay({ repoUrl, settings = {}, moreFromAzEnabled, moreFromAzJsonUrl, onDismiss }) {
  const accent = settings.activeColor || '#6200ee';
  const surface = settings.translucentBackground || '#ffffff';

  const [state, setState] = useState({ status: 'loading', entries: [], offline: false });
  const [selected, setSelected] = useState(null);
  const [docBody, setDocBody] = useState(null);
  const [showMore, setShowMore] = useState(false);

  useEffect(() => {
    let active = true;
    listDocs(repoUrl)
      .then((r) => active && setState({ status: 'loaded', entries: r.entries, offline: r.offline }))
      .catch(() => active && setState({ status: 'error', entries: [], offline: false }));
    return () => { active = false; };
  }, [repoUrl]);

  useEffect(() => {
    if (!selected) { setDocBody(null); return; }
    let active = true;
    setDocBody(null);
    fetchDoc(selected).then((b) => active && setDocBody(b ?? '_Could not load this document._'));
    return () => { active = false; };
  }, [selected]);

  return (
    <div className="az-about-overlay" style={{ background: surface }}>
      <div className="az-about-header">
        {selected && (
          <button className="az-about-iconbtn" style={{ color: accent }} onClick={() => setSelected(null)} aria-label="Back to contents">←</button>
        )}
        <span className="az-about-title" style={{ color: accent }}>{selected ? selected.title : 'About'}</span>
        <button className="az-about-iconbtn" style={{ color: accent }} onClick={onDismiss} aria-label="Close">✕</button>
      </div>

      {selected ? (
        <div className="az-about-reader">
          {docBody === null ? <div className="az-about-loading">Loading…</div> : <AzMarkdownWeb markdown={docBody} accent={accent} />}
        </div>
      ) : (
        <div className="az-about-body">
          {state.status === 'loading' && <div className="az-about-loading">Loading…</div>}
          {state.status === 'error' && <div className="az-about-empty">Couldn't load documentation.</div>}
          {state.status === 'loaded' && (
            <>
              {state.offline && <div className="az-about-banner">Showing cached docs (offline or rate-limited).</div>}
              {state.entries.length === 0 ? (
                <div className="az-about-empty">No documentation found in this repository.</div>
              ) : (
                <div className="az-about-toc">
                  {state.entries.map((e) => (
                    <button key={e.path} className="az-about-tocrow" style={{ borderColor: accent, color: accent }} onClick={() => setSelected(e)}>
                      {e.title}
                    </button>
                  ))}
                  {moreFromAzEnabled && (
                    <button className="az-about-tocrow emphasized" style={{ borderColor: accent, color: accent }} onClick={() => setShowMore(true)}>
                      More from Az
                    </button>
                  )}
                </div>
              )}
              <div className="az-about-spacer" />
              <hr className="az-about-divider" />
              <a className="az-about-repo" href={repoUrl} target="_blank" rel="noreferrer" style={{ borderColor: accent, color: accent }}>
                View on GitHub
              </a>
            </>
          )}
        </div>
      )}

      {showMore && (
        <MoreFromAzOverlay jsonUrl={moreFromAzJsonUrl} settings={settings} onDismiss={() => setShowMore(false)} />
      )}
    </div>
  );
}
