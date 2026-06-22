import React, { useEffect, useState } from 'react';
import './AboutOverlay.css';
import { fetchMoreFromAz } from '../services/moreFromAz';

/**
 * Full-screen "More from Az" overlay for the web: a horizontal scroll-snap carousel of app-icon
 * cards with a detail pane below. Cards reuse the rail's transparent-shape-with-colored-stroke
 * language. Data comes from the link-only `more-from-az.json` (metadata auto-resolved).
 */
export default function MoreFromAzOverlay({ jsonUrl, settings = {}, onDismiss }) {
  const accent = settings.activeColor || '#6200ee';
  const surface = settings.translucentBackground || '#ffffff';
  const [apps, setApps] = useState(null);
  const [selected, setSelected] = useState(0);

  useEffect(() => {
    let active = true;
    fetchMoreFromAz(jsonUrl).then((r) => active && setApps(r?.apps ?? []));
    return () => { active = false; };
  }, [jsonUrl]);

  const current = apps && apps.length ? apps[Math.min(selected, apps.length - 1)] : null;

  return (
    <div className="az-about-overlay" style={{ background: surface }}>
      <div className="az-about-header">
        <button className="az-about-iconbtn" style={{ color: accent }} onClick={onDismiss} aria-label="Back">←</button>
        <span className="az-about-title" style={{ color: accent }}>More from Az</span>
      </div>

      {apps === null && <div className="az-about-loading">Loading…</div>}
      {apps && apps.length === 0 && <div className="az-about-empty">Couldn't load apps right now.</div>}

      {current && (
        <div className="az-more-body">
          <div className="az-more-carousel">
            {apps.map((app, idx) => (
              <button
                key={app.githubUrl || app.playStoreUrl || app.name}
                className={`az-more-card${idx === selected ? ' focused' : ''}`}
                style={{ borderColor: idx === selected ? accent : `${accent}66` }}
                onClick={() => setSelected(idx)}
              >
                {app.iconUrl ? <img src={app.iconUrl} alt={app.name} /> : <span style={{ color: accent }}>{app.name.slice(0, 2).toUpperCase()}</span>}
              </button>
            ))}
          </div>

          <hr className="az-about-divider" />
          <div className="az-more-detail">
            <div className="az-more-name">{current.name}</div>
            {current.description && <div className="az-more-desc">{current.description}</div>}
            <div className="az-more-actions">
              {current.webUrl && (
                <a className="az-about-repo" href={current.webUrl} target="_blank" rel="noreferrer" style={{ borderColor: accent, color: accent }}>{current.isPwa ? 'Open' : 'Website'}</a>
              )}
              {current.playStoreUrl && (
                <a className="az-about-repo" href={current.playStoreUrl} target="_blank" rel="noreferrer" style={{ borderColor: accent, color: accent }}>Play Store</a>
              )}
              {current.githubUrl && (
                <a className="az-about-repo" href={current.githubUrl} target="_blank" rel="noreferrer" style={{ borderColor: accent, color: accent }}>GitHub</a>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
