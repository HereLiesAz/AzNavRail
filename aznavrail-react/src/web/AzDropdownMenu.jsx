import React, { createContext, useContext, useState, useRef, useEffect } from 'react';
import AzButton from './AzButton';
import AboutOverlay from './AboutOverlay';
import './AzDropdownMenu.css';

const AzDropdownMenuContext = createContext(null);

/** Internal accessor for the enclosing menu's dismiss/design/navigation. */
function useDropdownContext() {
  const ctx = useContext(AzDropdownMenuContext);
  if (!ctx) throw new Error('AzDropdownItem must be used inside an <AzDropdownMenu>');
  return ctx;
}

/**
 * A tappable menu entry. In a `menu`-design panel it renders as a full-width labeled row (the
 * expanded-drawer look); in a `rail`-design panel it renders as a compact AzButton. Navigates
 * `route` (if set, via the menu's `onNavigate`) then runs `onClick`, folding the menu by default.
 */
export const AzDropdownItem = ({
  text,
  onClick,
  route,
  shape = 'RECTANGLE',
  enabled = true,
  color,
  textColor,
  fillColor,
  closeOnClick = true,
  index = 0,
}) => {
  const {
    dismiss,
    design,
    onNavigate,
    dockingSide,
    menuItemAlignment,
    justifyMenuItems,
    entranceStartAngle,
    entranceStaggerMs,
    entranceDurationMs,
  } = useDropdownContext();
  const press = () => {
    if (route && onNavigate) onNavigate(route);
    onClick && onClick();
    if (closeOnClick) dismiss();
  };
  const rowRef = useRef(null);
  const measureRef = useRef(null);
  const [letterSpacing, setLetterSpacing] = useState(0);
  useEffect(() => {
    if (!justifyMenuItems || !text || text.length < 2) { setLetterSpacing(0); return; }
    const row = rowRef.current;
    const meas = measureRef.current;
    if (!row || !meas) return;
    const w = row.getBoundingClientRect().width;
    const nat = meas.getBoundingClientRect().width;
    if (w > nat && nat > 0) setLetterSpacing((w - nat) / (text.length - 1));
    else setLetterSpacing(0);
  }, [text, justifyMenuItems]);
  if (design === 'menu') {
    const hingeSide = dockingSide === 'RIGHT' ? 'right' : 'left';
    const textAlign =
      menuItemAlignment === 'center'
        ? 'center'
        : dockingSide === 'RIGHT'
          ? 'right'
          : 'left';
    return (
      <div
        ref={rowRef}
        className={`az-dropdown-menu-item--menu${enabled ? '' : ' disabled'}`}
        style={{
          color: textColor || color || undefined,
          textAlign,
          letterSpacing: `${letterSpacing}px`,
          animation: `azTurnstile ${entranceDurationMs}ms cubic-bezier(0.1, 0.9, 0.2, 1) ${index * entranceStaggerMs}ms both`,
          transformOrigin: `${hingeSide} center`,
          '--az-start-angle': `${dockingSide === 'RIGHT' ? -entranceStartAngle : entranceStartAngle}deg`,
        }}
        role="menuitem"
        tabIndex={enabled ? 0 : -1}
        onClick={enabled ? press : undefined}
      >
        <span
          ref={measureRef}
          style={{ position: 'absolute', visibility: 'hidden', whiteSpace: 'nowrap', left: -9999 }}
        >
          {text}
        </span>
        {text}
      </div>
    );
  }
  return (
    <AzButton
      text={text}
      shape={shape}
      enabled={enabled}
      color={color}
      textColor={textColor}
      fillColor={fillColor}
      onClick={press}
    />
  );
};

/**
 * A standalone, hamburger-style drop-down menu (web), declared with the same opinionated surface as
 * the rail. The trigger is the **app icon** (`/app-icon.png`, like the rail header; its shape/size
 * set via `headerIconShape`/`headerIconSize`); the panel is configured by `design` + `dockingSide`,
 * width-constrained to match, pinned to the chosen screen edge, and dropped from the trigger. Items
 * may carry a `route` dispatched through `onNavigate`. Clicking outside folds it up.
 *
 * @param {object} props
 * @param {'rail'|'menu'} [props.design='menu'] - Rail look (rail width) or menu look (menu width).
 * @param {'LEFT'|'RIGHT'} [props.dockingSide='LEFT'] - Which screen edge the panel pins to.
 * @param {boolean} [props.vibrate=false] - Haptic feedback on tap (where supported).
 * @param {number} [props.expandedWidth=160] - Panel width in the menu design.
 * @param {number} [props.collapsedWidth=100] - Panel width in the rail design.
 * @param {'CIRCLE'|'ROUNDED'|'SQUARE'} [props.headerIconShape='CIRCLE'] - App-icon clip shape.
 * @param {number} [props.headerIconSize=48] - App-icon trigger diameter (px).
 * @param {boolean} [props.expanded] - Optional controlled open-state.
 * @param {function} [props.onExpandedChange]
 * @param {function} [props.onNavigate] - Called with an item's `route` before its callback.
 * @param {boolean} [props.showFooter=true] - Whether the menu design shows the About/Feedback/@HereLiesAz footer.
 * @param {string} [props.appRepositoryUrl] - URL backing the footer's "About" item. When unset/blank, "About" is hidden.
 * @param {boolean} [props.inAppAbout=true] - When true, "About" opens the in-app reader; else the URL. Requires `appRepositoryUrl`.
 * @param {boolean} [props.moreFromAzEnabled=true] - Whether the in-app About reader offers a "More from Az" carousel.
 * @param {string} [props.moreFromAzJsonUrl] - Raw URL of the `more-from-az.json` manifest.
 * @param {React.ReactNode} props.children
 */
const AzDropdownMenu = ({
  design = 'menu',
  dockingSide = 'LEFT',
  vibrate = false,
  expandedWidth = 160,
  collapsedWidth = 100,
  headerIconShape = 'CIRCLE',
  headerIconSize = 48,
  showFooter = true,
  appRepositoryUrl,
  inAppAbout = true,
  moreFromAzEnabled = true,
  moreFromAzJsonUrl = 'https://raw.githubusercontent.com/HereLiesAz/AzNavRail/main/more-from-az.json',
  expanded,
  onExpandedChange,
  onNavigate,
  entranceStaggerMs = 60,
  entranceDurationMs = 720,
  entranceStartAngle = 90,
  dimBehindMenu = false,
  dimBehindMenuAlpha = 0.4,
  menuItemAlignment = 'side',
  justifyMenuItems = true,
  children,
}) => {
  const [internalOpen, setInternalOpen] = useState(false);
  const isOpen = expanded ?? internalOpen;
  const rootRef = useRef(null);
  const triggerRef = useRef(null);
  const [rect, setRect] = useState(null);
  // Full-screen in-app About reader reachable from the dropdown footer (parity with the rail).
  const [showAbout, setShowAbout] = useState(false);

  const setOpen = (value) => {
    if (expanded === undefined) setInternalOpen(value);
    if (onExpandedChange) onExpandedChange(value);
  };

  const toggle = () => {
    if (!isOpen && vibrate && typeof navigator !== 'undefined' && navigator.vibrate) navigator.vibrate(10);
    setOpen(!isOpen);
  };

  // Fold up on outside click.
  useEffect(() => {
    if (!isOpen) return undefined;
    const onDocClick = (e) => {
      if (rootRef.current && !rootRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onDocClick);
    return () => document.removeEventListener('mousedown', onDocClick);
  });

  // Measure the trigger so the (fixed-positioned) panel can drop from it while hugging the edge.
  // Scroll/resize fire at a high rate, so coalesce measurements to one per animation frame to
  // avoid layout thrashing.
  useEffect(() => {
    if (!isOpen) return undefined;
    let frame = 0;
    const measure = () => { if (triggerRef.current) setRect(triggerRef.current.getBoundingClientRect()); };
    const schedule = () => {
      if (frame) return;
      frame = window.requestAnimationFrame(() => { frame = 0; measure(); });
    };
    measure();
    window.addEventListener('resize', schedule);
    window.addEventListener('scroll', schedule, true);
    return () => {
      if (frame) window.cancelAnimationFrame(frame);
      window.removeEventListener('resize', schedule);
      window.removeEventListener('scroll', schedule, true);
    };
  }, [isOpen]);

  const panelWidth = design === 'rail' ? collapsedWidth : expandedWidth;
  const winH = typeof window !== 'undefined' ? window.innerHeight : 0;

  // Horizontal: pin to the physical docking-side edge.
  const panelStyle = {
    position: 'fixed',
    width: panelWidth,
    left: dockingSide === 'RIGHT' ? 'auto' : 0,
    right: dockingSide === 'RIGHT' ? 0 : 'auto',
  };
  // Vertical: drop from the trigger — below it normally, above it when the trigger sits low.
  if (rect) {
    const openUp = rect.top > winH * 0.6;
    if (openUp) {
      panelStyle.bottom = winH - rect.top;
      panelStyle.top = 'auto';
    } else {
      panelStyle.top = rect.bottom;
      panelStyle.bottom = 'auto';
    }
  }

  // Clip radius mirrors the rail's header icon: circle = half, rounded = 8, anything else = 0.
  const triggerRadius =
    headerIconShape === 'ROUNDED' ? 8 :
    headerIconShape === 'CIRCLE' ? headerIconSize / 2 :
    0;

  return (
    <div className="az-dropdown-menu" ref={rootRef}>
      <button
        type="button"
        ref={triggerRef}
        className="az-dropdown-menu-trigger"
        style={{ width: headerIconSize, height: headerIconSize, borderRadius: triggerRadius }}
        aria-label="Menu"
        aria-haspopup="menu"
        aria-expanded={isOpen}
        onClick={toggle}
      >
        {/* The app icon — drawn like the rail header, clipped to the configured shape/size. */}
        <img src="/app-icon.png" alt="App Icon" className="az-dropdown-menu-app-icon" style={{ borderRadius: triggerRadius }} />
      </button>
      {isOpen && dimBehindMenu && (
        <div
          className="az-nav-rail__scrim"
          style={{ '--az-scrim-alpha': String(Math.max(0, Math.min(1, dimBehindMenuAlpha))), position: 'fixed', inset: 0, zIndex: 999 }}
          onClick={() => setOpen(false)}
        />
      )}
      {isOpen && (
        <div className={`az-dropdown-menu-panel ${design === 'menu' ? 'menu' : 'rail'}`} style={{ ...panelStyle, zIndex: 1000 }} role="menu">
          <AzDropdownMenuContext.Provider
            value={{
              dismiss: () => setOpen(false),
              design,
              onNavigate,
              dockingSide,
              menuItemAlignment,
              justifyMenuItems,
              entranceStartAngle,
              entranceStaggerMs,
              entranceDurationMs,
            }}
          >
            {React.Children.map(children, (child, i) =>
              React.isValidElement(child) ? React.cloneElement(child, { index: i }) : child
            )}
          </AzDropdownMenuContext.Provider>
          {/* The expanded-menu design carries the rail's footer. Its accordion unfold starts when
              the LAST item starts its own kinetic entrance. */}
          {design === 'menu' && showFooter && (
            <div
              className="az-dropdown-menu-footer az-nav-rail__footer-accordion"
              style={{
                animationDelay: `${Math.max(0, React.Children.count(children) - 1) * entranceStaggerMs}ms`,
                animationDuration: `${entranceDurationMs}ms`,
              }}
            >
              {/* About is hidden entirely when no repository URL is configured. */}
              {!!appRepositoryUrl && (
                <div className="az-dropdown-menu-footer-item" onClick={() => {
                  if (inAppAbout) {
                    setShowAbout(true);
                  } else if (appRepositoryUrl.startsWith('http://') || appRepositoryUrl.startsWith('https://')) {
                    // Only follow plain web URLs, never an injected scheme (e.g. javascript:).
                    window.open(appRepositoryUrl, '_blank', 'noopener,noreferrer');
                  }
                }}>About</div>
              )}
              <div className="az-dropdown-menu-footer-item" onClick={() => window.open('mailto:hereliesaz@gmail.com?subject=Feedback', '_self')}>Feedback</div>
              <div className="az-dropdown-menu-footer-item" style={{ opacity: 0.5 }} onClick={() => window.open('https://instagram.com/HereLiesAz', '_blank', 'noopener,noreferrer')}>@HereLiesAz</div>
            </div>
          )}
        </div>
      )}
      {/* Full-screen in-app About reader above the dropdown panel — mirrors the rail. The
          "More from Az" carousel is reachable from within AboutOverlay itself. */}
      {showAbout && !!appRepositoryUrl && (
        <AboutOverlay
          repoUrl={appRepositoryUrl}
          moreFromAzEnabled={moreFromAzEnabled}
          moreFromAzJsonUrl={moreFromAzJsonUrl}
          onDismiss={() => setShowAbout(false)}
        />
      )}
    </div>
  );
};

export default AzDropdownMenu;
