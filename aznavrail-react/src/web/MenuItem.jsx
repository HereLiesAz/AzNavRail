import React, { useEffect, useRef, useState } from 'react';
import './MenuItem.css';

/**
 * A menu item component for the expanded navigation rail (plain-web build).
 *
 * Extras vs. the RN component:
 *  - Kinetic entrance: a per-item CSS animation that swings the row from 90° edge-on to 0° flat,
 *    hinged on the docked side (`transform-origin: left|right center`). The stagger delay is set
 *    by `index * staggerMs` so items overlap when `staggerMs` is much smaller than `durationMs`.
 *  - Side-alignment: labels are hard-aligned to the docked side (or centered) via `text-align`.
 *  - Kerning-justify: for labels shorter than the row, `letter-spacing` is measured & applied so
 *    the label fills the row edge-to-edge.
 */
const MenuItem = ({
  item,
  depth = 0,
  onToggle,
  onCyclerClick,
  isHost,
  isExpanded,
  onHostClick,
  infoScreen,
  // WP7 kinetic entrance
  index = 0,
  count = 1,
  visible = true,
  entrance = 'Turnstile',
  startAngle = 90,
  staggerMs = 60,
  durationMs = 720,
  dockingSide = 'LEFT',
  // Menu-drawer look
  menuItemAlignment = 'side',
  justifyMenuItems = true,
}) => {
  const {
    text,
    isToggle,
    isChecked,
    toggleOnText,
    toggleOffText,
    isCycler,
    selectedOption,
    textColor,
    onClick,
    isDivider,
    color = 'currentColor',
    onFocus,
  } = item;

  if (isDivider) {
    return <div className="az-menu-divider" style={{ backgroundColor: color, opacity: 0.2 }} />;
  }

  const isInteractive = infoScreen ? (isHost || item.isHelpItem) : !item.disabled;

  const handleClick = () => {
    if (!isInteractive) return;

    if (infoScreen) {
      if (item.isHelpItem && onClick) {
        onClick();
      } else if (isHost) {
        onHostClick();
      }
      return;
    }

    if (onFocus) onFocus();

    if (isHost) {
      onHostClick();
    } else if (isToggle) {
      onClick && onClick();
    } else if (isCycler) {
      onCyclerClick();
    } else {
      onClick && onClick();
      onToggle(); // Collapse menu
    }
  };

  const paddingLeft = 16 + (depth * 16);

  const ariaProps = {};
  if (isToggle) {
    ariaProps['aria-checked'] = isChecked;
    ariaProps.role = 'switch';
  } else if (isHost) {
    ariaProps['aria-expanded'] = isExpanded;
  } else if (isCycler) {
    ariaProps['aria-label'] = `${text} ${selectedOption}`;
  } else if (item.isNestedRail) {
    ariaProps['aria-expanded'] = isExpanded;
    ariaProps['aria-haspopup'] = 'true';
  }

  // Kinetic entrance: rotateY from `startAngle` → 0, hinged on the docked edge.
  const useTurnstile = entrance === 'Turnstile';
  const hingeSide = dockingSide === 'RIGHT' ? 'right' : 'left';
  const kineticStyle = useTurnstile && visible
    ? {
        animation: `azTurnstile ${durationMs}ms cubic-bezier(0.1, 0.9, 0.2, 1) ${index * staggerMs}ms both`,
        transformOrigin: `${hingeSide} center`,
        // Custom prop the keyframes read.
        '--az-start-angle': `${dockingSide === 'RIGHT' ? -startAngle : startAngle}deg`,
      }
    : undefined;

  // Side-align + kerning-justify.
  const textAlign =
    menuItemAlignment === 'center'
      ? 'center'
      : dockingSide === 'RIGHT'
        ? 'right'
        : 'left';

  const rowRef = useRef(null);
  const measureRef = useRef(null);
  const [letterSpacing, setLetterSpacing] = useState(0);
  const label = (isToggle ? (isChecked ? toggleOnText : toggleOffText) : (isCycler ? selectedOption : text)) || '';
  useEffect(() => {
    if (!justifyMenuItems || !label || label.length < 2) { setLetterSpacing(0); return; }
    const row = rowRef.current;
    const meas = measureRef.current;
    if (!row || !meas) return;
    const rowWidth = row.getBoundingClientRect().width - paddingLeft - 16;
    const natural = meas.getBoundingClientRect().width;
    if (rowWidth > natural && natural > 0) {
      setLetterSpacing((rowWidth - natural) / (label.length - 1));
    } else {
      setLetterSpacing(0);
    }
  }, [label, justifyMenuItems, paddingLeft]);

  const labelStyle = { textAlign, letterSpacing: `${letterSpacing}px`, flex: 1, color: textColor || undefined };

  return (
    <div
      ref={rowRef}
      className="az-menu-item"
      style={{ color, paddingLeft: `${paddingLeft}px`, position: 'relative', ...kineticStyle }}
      onClick={handleClick}
      data-az-nav-id={item.id}
      {...ariaProps}
    >
      {/* Off-screen measurer — same font-size as the visible label. */}
      <span ref={measureRef} className="az-menu-item-measurer">{label}</span>

      {isToggle ? (
        <div className="az-menu-item-content toggle">
          <span className="az-menu-item-text" style={labelStyle}>
            {isChecked ? toggleOnText : toggleOffText}
          </span>
        </div>
      ) : isCycler ? (
        <div className="az-menu-item-content cycler">
          <span className="az-menu-item-text" style={labelStyle}>{text}</span>
          <span className="az-menu-item-value">{selectedOption}</span>
        </div>
      ) : (
        <div className="az-menu-item-content button">
          <span className="az-menu-item-text" style={labelStyle}>{text}</span>
          {isHost && (
            <span className="az-menu-item-arrow">
              {isExpanded ? '▼' : '▶'}
            </span>
          )}
        </div>
      )}
    </div>
  );
};

export default MenuItem;
