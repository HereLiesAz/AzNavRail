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

/** A tappable menu entry rendered as an AzButton; folds the menu on click by default. */
export const AzDropdownItem = ({ text, onClick, shape = 'RECTANGLE', enabled = true, color, fillColor, closeOnClick = true }) => {
  const { dismiss } = useAzDropdownMenu();
  return (
    <AzButton
      text={text}
      shape={shape}
      enabled={enabled}
      color={color}
      fillColor={fillColor}
      onClick={() => {
        onClick && onClick();
        if (closeOnClick) dismiss();
      }}
    />
  );
};

/**
 * A standalone, hamburger-style drop-down menu (web) — used the usual, expected way.
 *
 * Drop it inline anywhere, like AzButton: it renders an icon trigger and an anchored panel holding
 * the `children`. Clicking outside folds it up.
 *
 * @param {object} props
 * @param {string} [props.icon] - URL of the hamburger icon. Falls back to a "≡" glyph.
 * @param {string} [props.contentDescription='Menu']
 * @param {number} [props.iconSize=48]
 * @param {'CIRCLE'|'ROUNDED'|'NONE'} [props.iconShape='CIRCLE']
 * @param {number} [props.menuWidth]
 * @param {string} [props.backgroundColor]
 * @param {string} [props.alignment='top-start'] - An AzDropdownAlignment value.
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

  const { vert, horiz, isBottom } = parseDropdownAnchor(alignment);
  const offX = offset?.x ?? 0;
  const offY = offset?.y ?? 0;
  const txCenter = horiz === 'center' ? '-50%' : '0px';

  const panelStyle = {
    left: horiz === 'end' ? 'auto' : horiz === 'center' ? '50%' : 0,
    right: horiz === 'end' ? 0 : 'auto',
    top: isBottom ? 'auto' : '100%',
    bottom: isBottom ? '100%' : 'auto',
    transform: `translate(calc(${txCenter} + ${offX}px), ${offY}px)`,
    backgroundColor: backgroundColor || undefined,
    width: menuWidth || undefined,
  };

  const iconClass = iconShape === 'CIRCLE' ? 'circle' : iconShape === 'ROUNDED' ? 'rounded' : '';

  return (
    <div className="az-dropdown-menu" ref={rootRef}>
      <button
        type="button"
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
        <div className="az-dropdown-menu-panel" style={panelStyle} role="menu">
          <AzDropdownMenuContext.Provider value={{ dismiss: () => setOpen(false) }}>
            {children}
          </AzDropdownMenuContext.Provider>
        </div>
      )}
    </div>
  );
};

export default AzDropdownMenu;
