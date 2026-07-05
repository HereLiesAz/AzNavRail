import React, { useEffect, useRef, useState } from 'react';
import './AboutOverlay.css';
import AzMarkdownWeb from './AzMarkdownWeb';
import { listDocs, fetchDoc } from '../services/githubDocs';
import { fetchMoreFromAz } from '../services/moreFromAz';

/**
 * In-app About reader for the web.
 *
 * Layout is two vertically-stacked halves:
 *  - **Top half** — auto-generated table of contents of the repo's markdown docs.
 *  - **Bottom half** — a focused-hero More-from-Az carousel with a size pattern
 *    (small · medium · LARGE · medium · small) and the active app's banner (when the repo has
 *    `docs/banner.*`), name, description, and link buttons.
 */
export default function AboutOverlay({ repoUrl, settings = {}, moreFromAzEnabled, moreFromAzJsonUrl, onDismiss }) {
  const accent = settings.activeColor || '#6200ee';
  const surface = settings.translucentBackground || '#ffffff';

  const [state, setState] = useState({ status: 'loading', entries: [], offline: false });
  const [selected, setSelected] = useState(null);
  const [docBody, setDocBody] = useState(null);
  const [moreApps, setMoreApps] = useState(null);

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

  useEffect(() => {
    if (!moreFromAzEnabled) { setMoreApps([]); return; }
    let active = true;
    fetchMoreFromAz(moreFromAzJsonUrl)
      .then((r) => active && setMoreApps(r?.apps ?? []))
      .catch(() => active && setMoreApps([]));
    return () => { active = false; };
  }, [moreFromAzEnabled, moreFromAzJsonUrl]);

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
        <>
          {/* TOP HALF — docs TOC. */}
          <div className="az-about-half">
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
                  </div>
                )}
              </>
            )}
          </div>

          {/* BOTTOM HALF — More-from-Az focused-hero carousel + active-app info. */}
          {moreFromAzEnabled && (
            <div className="az-about-half">
              <hr className="az-about-divider" style={{ borderTopColor: accent, color: accent }} />
              <MoreFromAzHeroCarousel apps={moreApps} accent={accent} />
            </div>
          )}
        </>
      )}
    </div>
  );
}

const HERO_LARGE = 132;
const HERO_MEDIUM = 96;
const HERO_SMALL = 64;
const HERO_SPACING = 12;

function MoreFromAzHeroCarousel({ apps, accent }) {
  const [activeIndex, setActiveIndex] = useState(0);
  const railRef = useRef(null);

  if (apps === null) return <div className="az-about-loading">Loading…</div>;
  if (apps.length === 0) return <div className="az-about-empty">No apps to show right now.</div>;

  const activeApp = apps[activeIndex];

  const onScroll = () => {
    const el = railRef.current;
    if (!el) return;
    const center = el.scrollLeft + el.clientWidth / 2;
    let closest = 0;
    let closestDelta = Infinity;
    for (let i = 0; i < apps.length; i += 1) {
      const child = el.children[i];
      if (!child) continue;
      const c = child.offsetLeft + child.offsetWidth / 2;
      const d = Math.abs(c - center);
      if (d < closestDelta) { closestDelta = d; closest = i; }
    }
    if (closest !== activeIndex) setActiveIndex(closest);
  };

  const scrollTo = (i) => {
    const el = railRef.current;
    if (!el) return;
    const child = el.children[i];
    if (!child) return;
    const target = child.offsetLeft + child.offsetWidth / 2 - el.clientWidth / 2;
    el.scrollTo({ left: target, behavior: 'smooth' });
    setActiveIndex(i);
  };

  const isAppIcon = (u) => !!u && !u.includes('avatars.githubusercontent.com');
  const open = (u) => { if (u) window.open(u, '_blank', 'noopener,noreferrer'); };

  return (
    <div className="az-about-hero">
      <div
        ref={railRef}
        className="az-about-hero-rail"
        onScroll={onScroll}
        style={{
          padding: `0 calc(50% - ${HERO_LARGE / 2}px)`,
          gap: `${HERO_SPACING}px`,
        }}
      >
        {apps.map((app, i) => {
          const distance = Math.abs(i - activeIndex);
          const size = distance === 0 ? HERO_LARGE : distance === 1 ? HERO_MEDIUM : HERO_SMALL;
          const isActive = i === activeIndex;
          return (
            <button
              key={i}
              className="az-about-hero-card"
              onClick={() => (isActive ? open(app.webUrl || app.playStoreUrl || app.githubUrl) : scrollTo(i))}
              style={{
                width: size,
                height: size,
                borderColor: isActive ? accent : accent + '66',
                borderWidth: isActive ? 2 : 1,
              }}
              aria-label={app.name}
            >
              {isAppIcon(app.iconUrl) ? (
                <img src={app.iconUrl} alt={app.name} />
              ) : (
                <span style={{ color: accent, fontSize: 28, fontWeight: 'bold' }}>
                  {(app.name || '').slice(0, 2).toUpperCase()}
                </span>
              )}
            </button>
          );
        })}
      </div>
      {activeApp && (
        <div className="az-about-hero-info">
          {activeApp.bannerUrl ? (
            <img src={activeApp.bannerUrl} alt={`${activeApp.name} banner`} className="az-about-hero-banner" />
          ) : null}
          <div className="az-about-hero-name">{activeApp.name}</div>
          {activeApp.description && <div className="az-about-hero-desc">{activeApp.description}</div>}
          <div className="az-about-hero-actions">
            {activeApp.playStoreUrl && (
              <button style={{ borderColor: accent, color: accent }} onClick={() => open(activeApp.playStoreUrl)}>Play</button>
            )}
            {activeApp.webUrl && (
              <button style={{ borderColor: accent, color: accent }} onClick={() => open(activeApp.webUrl)}>
                {activeApp.isPwa ? 'Open' : 'Website'}
              </button>
            )}
            {activeApp.githubUrl && (
              <button style={{ borderColor: accent, color: accent }} onClick={() => open(activeApp.githubUrl)}>GitHub</button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
