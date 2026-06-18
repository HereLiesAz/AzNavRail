import React, { createContext, useContext, useState, useRef, useEffect } from 'react';
import AzButton from './AzButton';
import { parseDropdownAnchor } from '../dropdownPlacement';
import './AzDropdownMenu.css';

const AzDropdownMenuContext = createContext(null);

/** Hook to fold the enclosing AzDropdownMenu from custom children. */
export function useAzDropdownMenu() {
  const ctx = useContext(AzDropdownMenuContext);
  if (!ctx) throw new Error('useAzDropdownMenu must be used inside an <AzDropdownMenu>');
  return ctx;
}

/**
 * A tappable menu entry. In a `menu`-design panel it renders as a full-width labeled row (the
 * expanded-drawer look); in a `rail`-design panel it renders as a compact AzButton. Folds the menu
 * on click by default.
 */
export const AzDropdownItem = ({ text, onClick, shape = 'RECTANGLE', enabled = true, color, textColor, fillColor, closeOnClick = true }) => {
  const { dismiss, design } = useAzDropdownMenu();
  const press = () => {
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
 * A standalone, hamburger-style drop-down menu (web) — used the usual, expected way.
 *
 * Drop the icon inline anywhere, like AzButton (it takes a normal slot, like a hamburger button).
 * Clicking it shows the `children` as a panel styled like the collapsed rail (`design='rail'`) or
 * the expanded menu (`design='menu'`) — width-constrained to match — pinned to the left/right
 * screen edge (per `alignment`) and dropping from the trigger. Clicking outside folds it up.
 *
 * @param {object} props
 * @param {string} [props.icon] - URL of the hamburger icon. Falls back to a "≡" glyph.
 * @param {string} [props.contentDescription='Menu']
 * @param {number} [props.iconSize=48]
 * @param {'CIRCLE'|'ROUNDED'|'NONE'} [props.iconShape='CIRCLE']
 * @param {'rail'|'menu'} [props.design='menu'] - Rail look (rail width) or menu look (menu width).
 * @param {number} [props.menuWidth] - Overrides the design width.
 * @param {string} [props.backgroundColor]
 * @param {string} [props.alignment='top-start'] - An AzDropdownAlignment value; start=left edge, end=right edge.
 * @param {{x?:number,y?:number}} [props.offset]
 * @param {boolean} [props.expanded] - Optional controlled open-state.
 * @param {function} [props.onExpandedChange]
 * @param {React.ReactNode} props.children
 */
const AzDropdownMenu = ({
  icon,
  contentDescription = 'Menu',
  iconSize = 48,
  iconShape = 'CIRCLE',
  design = 'menu',
  menuWidth,
  backgroundColor,
  alignment = 'top-start',
  offset,
  expanded,
  onExpandedChange,
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

  const { horiz, isBottom } = parseDropdownAnchor(alignment);
  const offX = offset?.x ?? 0;
  const offY = offset?.y ?? 0;
  const panelWidth = menuWidth ?? (design === 'rail' ? 100 : 160);
  const winW = typeof window !== 'undefined' ? window.innerWidth : 0;
  const winH = typeof window !== 'undefined' ? window.innerHeight : 0;

  // Horizontal: pin to the screen edge (start=left, end=right, centre=centred).
  const panelStyle = {
    position: 'fixed',
    width: panelWidth,
    backgroundColor: backgroundColor || undefined,
    left: horiz === 'end' ? 'auto' : horiz === 'center' ? (winW - panelWidth) / 2 + offX : offX,
    right: horiz === 'end' ? offX : 'auto',
  };
  // Vertical: drop from the trigger — below it for top/centre anchors, above it for bottom.
  if (rect) {
    if (isBottom) {
      panelStyle.bottom = winH - rect.top + offY;
      panelStyle.top = 'auto';
    } else {
      panelStyle.top = rect.bottom + offY;
      panelStyle.bottom = 'auto';
    }
  }

  const iconClass = iconShape === 'CIRCLE' ? 'circle' : iconShape === 'ROUNDED' ? 'rounded' : '';

  return (
    <div className="az-dropdown-menu" ref={rootRef}>
      <button
        type="button"
        ref={triggerRef}
        className={`az-dropdown-menu-trigger ${iconClass}`}
        style={{ width: iconSize, height: iconSize, fontSize: iconSize * 0.5 }}
        aria-label={contentDescription}
        aria-haspopup="menu"
        aria-expanded={isOpen}
        onClick={() => setOpen(!isOpen)}
      >
        {icon ? <img src={icon} alt={contentDescription} style={{ width: iconSize, height: iconSize }} /> : '≡'}
      </button>
      {isOpen && (
        <div className={`az-dropdown-menu-panel ${design === 'menu' ? 'menu' : 'rail'}`} style={panelStyle} role="menu">
          <AzDropdownMenuContext.Provider value={{ dismiss: () => setOpen(false), design }}>
            {children}
          </AzDropdownMenuContext.Provider>
        </div>
      )}
    </div>
  );
};

export default AzDropdownMenu;
