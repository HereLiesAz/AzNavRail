import React, { useEffect, useRef, useState } from 'react';
import './AboutOverlay.css';
import AzMarkdownWeb from './AzMarkdownWeb';
import { listDocs, fetchDoc, fetchAuthorProfile } from '../services/githubDocs';
import { fetchMoreFromAz } from '../services/moreFromAz';
import { useAzAccent, AZ_ACCENT_FALLBACK } from '../AzRailPalette';

/** Where the About page's tip button sends anyone who wants to leave one. */
const AZ_DONATE_URL = 'https://paypal.me/HereLiesAz';

/** The free-forever pitch shown above the tip button. */
const AZ_TIP_PITCH =
  'Every single one of my apps is available on Github in full, without ads or conditions of ' +
  'any kind, for free and forever. But I never say no to just a the-tip.';

/**
 * The reader's own palette — dark ground, light ink, in every theme. It is a full-screen surface the
 * user has stepped *aside* into for long-form reading, and a bright white field is the wrong call
 * regardless of what the surrounding app is doing. Matches the Android/CMP/RN readers.
 */
const AZ_ABOUT_GROUND = '#101014';
const AZ_ABOUT_INK = '#ececf0';

/**
 * In-app About reader for the web.
 *
 * Layout is vertically stacked, each section sharing the screen equally:
 *  - **Docs** — auto-generated table of contents of the repo's markdown docs.
 *  - **More from Az** (when enabled) — a focused-hero carousel with a size pattern
 *    (small · medium · LARGE · medium · small) and the active app's banner (when the repo has
 *    `docs/banner.*`), name, description, and link buttons.
 *  - **Tip jar, author, and contact** — the free-forever pitch and tip button, the author's GitHub
 *    avatar/name/bio (fetched live from the GitHub users API), and the @HereLiesAz/Feedback/website
 *    links, stacked in large type. Scrolls independently of the sections above it.
 */
export default function AboutOverlay({
  repoUrl,
  settings = {},
  moreFromAzEnabled,
  moreFromAzJsonUrl,
  railGutter,
  dockingSide = 'LEFT',
  onDismiss,
}) {
  // The reader wears the rail's colour, not the app's. `activeColor` when the developer set one;
  // failing that, the colour the rail's own items are drawn in (see `resolveRailAccent`).
  const railAccent = useAzAccent(AZ_ACCENT_FALLBACK);
  const accent = settings.activeColor || railAccent;
  // `translucentBackground` supplies the hue; a see-through full-screen reader is unreadable, so
  // the reader always draws on an opaque ground.
  const surface = settings.translucentBackground || AZ_ABOUT_GROUND;
  // Never cover the rail's own gutter — the app icon behind it has to stay tappable.
  const gutter =
    dockingSide === 'RIGHT'
      ? { right: railGutter || 0 }
      : { left: railGutter || 0 };

  const [state, setState] = useState({
    status: 'loading',
    entries: [],
    offline: false,
  });
  const [selected, setSelected] = useState(null);
  const [docBody, setDocBody] = useState(null);
  const [moreApps, setMoreApps] = useState(null);
  const [authorProfile, setAuthorProfile] = useState(null);

  useEffect(() => {
    let active = true;
    listDocs(repoUrl)
      .then(
        (r) =>
          active &&
          setState({ status: 'loaded', entries: r.entries, offline: r.offline })
      )
      .catch(
        () =>
          active && setState({ status: 'error', entries: [], offline: false })
      );
    return () => {
      active = false;
    };
  }, [repoUrl]);

  useEffect(() => {
    if (!selected) {
      setDocBody(null);
      return;
    }
    let active = true;
    setDocBody(null);
    fetchDoc(selected).then(
      (b) => active && setDocBody(b ?? '_Could not load this document._')
    );
    return () => {
      active = false;
    };
  }, [selected]);

  useEffect(() => {
    if (!moreFromAzEnabled) {
      setMoreApps([]);
      return;
    }
    let active = true;
    fetchMoreFromAz(moreFromAzJsonUrl)
      .then((r) => active && setMoreApps(r?.apps ?? []))
      .catch(() => active && setMoreApps([]));
    return () => {
      active = false;
    };
  }, [moreFromAzEnabled, moreFromAzJsonUrl]);

  useEffect(() => {
    let active = true;
    fetchAuthorProfile()
      .then((profile) => active && setAuthorProfile(profile))
      .catch(() => {});
    return () => {
      active = false;
    };
  }, []);

  return (
    <div
      className="az-about-overlay"
      style={{ background: surface, color: AZ_ABOUT_INK, ...gutter }}
    >
      <div className="az-about-header">
        {selected && (
          <button
            className="az-about-iconbtn"
            style={{ color: accent }}
            onClick={() => setSelected(null)}
            aria-label="Back to contents"
          >
            ←
          </button>
        )}
        <span className="az-about-title" style={{ color: accent }}>
          {selected ? selected.title : 'About'}
        </span>
        <button
          className="az-about-iconbtn"
          style={{ color: accent }}
          onClick={onDismiss}
          aria-label="Close"
        >
          ✕
        </button>
      </div>

      {selected ? (
        <div className="az-about-reader">
          {docBody === null ? (
            <div className="az-about-loading">Loading…</div>
          ) : (
            <AzMarkdownWeb markdown={docBody} accent={accent} />
          )}
        </div>
      ) : (
        <>
          {/* TOP HALF — docs TOC. */}
          <div className="az-about-half">
            {state.status === 'loading' && (
              <div className="az-about-loading">Loading…</div>
            )}
            {state.status === 'error' && (
              <div className="az-about-empty">Couldn't load documentation.</div>
            )}
            {state.status === 'loaded' && (
              <>
                {state.offline && (
                  <div className="az-about-banner">
                    Showing cached docs (offline or rate-limited).
                  </div>
                )}
                {state.entries.length === 0 ? (
                  <div className="az-about-empty">
                    No documentation found in this repository.
                  </div>
                ) : (
                  <div className="az-about-toc">
                    {state.entries.map((e) => (
                      <button
                        key={e.path}
                        className="az-about-tocrow"
                        style={{ borderColor: accent, color: accent }}
                        onClick={() => setSelected(e)}
                      >
                        {e.title}
                      </button>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>

          {/* MIDDLE — More-from-Az focused-hero carousel + active-app info ("the app links"). */}
          {moreFromAzEnabled && (
            <div className="az-about-half">
              <hr
                className="az-about-divider"
                style={{ borderTopColor: accent, color: accent }}
              />
              <MoreFromAzHeroCarousel apps={moreApps} accent={accent} />
            </div>
          )}

          {/* BOTTOM — the tip pitch, the author, and the way to reach them. Scrolls on its own so it
              always has room for all three regardless of how much the sections above claim; the page
              ends where every other surface in this library ends: a way to write to the author, and
              the author. About is where someone goes to find out who made this, so making them hunt
              through a menu for that would be a joke at their expense. */}
          <hr
            className="az-about-divider"
            style={{ borderTopColor: accent, color: accent }}
          />
          <div className="az-about-footer-section">
            <AzTipJar accent={accent} />
            <div className="az-about-tip-gap" />
            <AzAuthorHeader accent={accent} profile={authorProfile} />
            <hr
              className="az-about-divider"
              style={{ borderTopColor: accent, color: accent }}
            />
            <div className="az-about-page-footer">
              <AzAboutFooterLink
                text="@HereLiesAz"
                color={accent}
                href="https://instagram.com/HereLiesAz"
              />
              <AzAboutFooterLink
                text="Feedback"
                color={accent}
                href="mailto:hereliesaz@gmail.com?subject=Feedback"
              />
              <AzAboutFooterLink
                text="hereliesaz.com"
                color={accent}
                href="https://hereliesaz.com"
              />
            </div>
          </div>
        </>
      )}
    </div>
  );
}

/**
 * The free-forever pitch and its tip button — sits between the app carousel and the author header,
 * where "here's more of my work" naturally turns into "here's how you can thank me for it".
 */
function AzTipJar({ accent }) {
  return (
    <div className="az-about-tipjar">
      <p className="az-about-tip-text">{AZ_TIP_PITCH}</p>
      <button
        className="az-about-tip-button"
        style={{ borderColor: accent, color: accent }}
        onClick={() =>
          window.open(AZ_DONATE_URL, '_blank', 'noopener,noreferrer')
        }
      >
        Leave a Tip
      </button>
    </div>
  );
}

/**
 * The author header: GitHub avatar, name, and bio. The avatar and bio are fetched live from the
 * GitHub users API rather than baked in, so this stays current with no release needed; the name is
 * fixed — it is always "Az" regardless of what GitHub's `name` field says.
 */
function AzAuthorHeader({ accent, profile }) {
  return (
    <div className="az-about-author">
      <div className="az-about-author-avatar" style={{ borderColor: accent }}>
        {profile?.avatarUrl ? (
          <img src={profile.avatarUrl} alt="Az" />
        ) : (
          <span style={{ color: accent }}>AZ</span>
        )}
      </div>
      <div className="az-about-author-name">Az</div>
      {profile?.bio && <p className="az-about-author-bio">{profile.bio}</p>}
    </div>
  );
}

/** One big, centered link row in the About page's own footer — see `AboutOverlay`. */
function AzAboutFooterLink({ text, color, href }) {
  return (
    <a
      className="az-about-footer-link"
      style={{ color }}
      href={href}
      target={href.startsWith('mailto:') ? undefined : '_blank'}
      rel={href.startsWith('mailto:') ? undefined : 'noopener noreferrer'}
    >
      {text}
    </a>
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
  if (apps.length === 0)
    return <div className="az-about-empty">No apps to show right now.</div>;

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
      if (d < closestDelta) {
        closestDelta = d;
        closest = i;
      }
    }
    if (closest !== activeIndex) setActiveIndex(closest);
  };

  const scrollTo = (i) => {
    const el = railRef.current;
    if (!el) return;
    const child = el.children[i];
    if (!child) return;
    const target =
      child.offsetLeft + child.offsetWidth / 2 - el.clientWidth / 2;
    el.scrollTo({ left: target, behavior: 'smooth' });
    setActiveIndex(i);
  };

  const isAppIcon = (u) => !!u && !u.includes('avatars.githubusercontent.com');
  const open = (u) => {
    if (u) window.open(u, '_blank', 'noopener,noreferrer');
  };

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
          const size =
            distance === 0
              ? HERO_LARGE
              : distance === 1
                ? HERO_MEDIUM
                : HERO_SMALL;
          const isActive = i === activeIndex;
          return (
            <button
              key={i}
              className="az-about-hero-card"
              onClick={() =>
                isActive
                  ? open(app.webUrl || app.playStoreUrl || app.githubUrl)
                  : scrollTo(i)
              }
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
                <span
                  style={{ color: accent, fontSize: 28, fontWeight: 'bold' }}
                >
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
            <img
              src={activeApp.bannerUrl}
              alt={`${activeApp.name} banner`}
              className="az-about-hero-banner"
            />
          ) : null}
          <div className="az-about-hero-name">{activeApp.name}</div>
          {activeApp.description && (
            <div className="az-about-hero-desc">{activeApp.description}</div>
          )}
          <div className="az-about-hero-actions">
            {activeApp.playStoreUrl && (
              <button
                style={{ borderColor: accent, color: accent }}
                onClick={() => open(activeApp.playStoreUrl)}
              >
                Play
              </button>
            )}
            {activeApp.webUrl && (
              <button
                style={{ borderColor: accent, color: accent }}
                onClick={() => open(activeApp.webUrl)}
              >
                {activeApp.isPwa ? 'Open' : 'Website'}
              </button>
            )}
            {activeApp.githubUrl && (
              <button
                style={{ borderColor: accent, color: accent }}
                onClick={() => open(activeApp.githubUrl)}
              >
                GitHub
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
