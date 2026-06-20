import React, { createContext, useContext, useState, useRef, useEffect } from 'react';
import AzButton from './AzButton';
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
export const AzDropdownItem = ({ text, onClick, route, shape = 'RECTANGLE', enabled = true, color, textColor, fillColor, closeOnClick = true }) => {
  const { dismiss, design, onNavigate } = useDropdownContext();
  const press = () => {
    if (route && onNavigate) onNavigate(route);
    onClick && onClick();
    if (closeOnClick) dismiss();
  };
  if (design === 'menu') {
    return (
      <div
        className={`az-dropdown-menu-item--menu${enabled ? '' : ' disabled'}`}
        style={{ color: textColor || color || undefined }}
        role="menuitem"
        tabIndex={enabled ? 0 : -1}
        onClick={enabled ? press : undefined}
      >
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
 * the rail. The trigger is the **app icon** (`/app-icon.png`, like the rail header — not
 * customizable); the panel is configured by `design` + `dockingSide`, width-constrained to match,
 * pinned to the chosen screen edge, and dropped from the trigger. Items may carry a `route`
 * dispatched through `onNavigate`. Clicking outside folds it up.
 *
 * @param {object} props
 * @param {'rail'|'menu'} [props.design='menu'] - Rail look (rail width) or menu look (menu width).
 * @param {'LEFT'|'RIGHT'} [props.dockingSide='LEFT'] - Which screen edge the panel pins to.
 * @param {boolean} [props.vibrate=false] - Haptic feedback on tap (where supported).
 * @param {number} [props.expandedWidth=160] - Panel width in the menu design.
 * @param {number} [props.collapsedWidth=100] - Panel width in the rail design.
 * @param {boolean} [props.expanded] - Optional controlled open-state.
 * @param {function} [props.onExpandedChange]
 * @param {function} [props.onNavigate] - Called with an item's `route` before its callback.
 * @param {React.ReactNode} props.children
 */
const AzDropdownMenu = ({
  design = 'menu',
  dockingSide = 'LEFT',
  vibrate = false,
  expandedWidth = 160,
  collapsedWidth = 100,
  expanded,
  onExpandedChange,
  onNavigate,
  children,
}) => {
  const [internalOpen, setInternalOpen] = useState(false);
  const isOpen = expanded ?? internalOpen;
  const rootRef = useRef(null);
  const triggerRef = useRef(null);
  const [rect, setRect] = useState(null);

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

  return (
    <div className="az-dropdown-menu" ref={rootRef}>
      <button
        type="button"
        ref={triggerRef}
        className="az-dropdown-menu-trigger circle"
        aria-label="Menu"
        aria-haspopup="menu"
        aria-expanded={isOpen}
        onClick={toggle}
      >
        {/* The app icon — drawn like the rail header, not customizable. */}
        <img src="/app-icon.png" alt="App Icon" className="az-dropdown-menu-app-icon" />
      </button>
      {isOpen && (
        <div className={`az-dropdown-menu-panel ${design === 'menu' ? 'menu' : 'rail'}`} style={panelStyle} role="menu">
          <AzDropdownMenuContext.Provider value={{ dismiss: () => setOpen(false), design, onNavigate }}>
            {children}
          </AzDropdownMenuContext.Provider>
        </div>
      )}
    </div>
  );
};

export default AzDropdownMenu;
