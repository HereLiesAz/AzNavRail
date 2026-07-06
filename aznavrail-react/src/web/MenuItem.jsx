import React, { useEffect, useRef, useState } from 'react';
import './MenuItem.css';
import { solveHybridJustify } from '../util/AzJustify';

const WEB_BASE_FONT_PX = 16;

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
  const label = (isToggle ? (isChecked ? toggleOnText : toggleOffText) : (isCycler ? selectedOption : text)) || '';
  // Split up-front on `\n` so each line owns its own measurement + solve. Compose does this too;
  // measuring the whole label at once folds newline width into `naturalWidth` and inflates the
  // solver's `charCount`, so per-line justification would drift for any multi-line label.
  const lines = label.split('\n');

  const renderLabelBlock = (visibleText) => (
    <span className="az-menu-item-text" style={{
      flex: 1,
      color: textColor || undefined,
      display: 'flex',
      flexDirection: 'column',
      alignItems: textAlign === 'right' ? 'flex-end' : textAlign === 'center' ? 'center' : 'flex-start',
    }}>
      {lines.map((line, i) => (
        <JustifiedWebLine
          key={i}
          line={line}
          justify={justifyMenuItems}
          rowRef={rowRef}
          paddingLeft={paddingLeft}
          textAlign={textAlign}
        />
      ))}
      {/* When called from toggle/cycler, `visibleText` may differ from `label` (e.g. cycler value);
          this branch only matters for the cycler's supplementary value, which is rendered outside. */}
      {visibleText && visibleText !== label ? null : null}
    </span>
  );

  return (
    <div
      ref={rowRef}
      className="az-menu-item"
      style={{ color, paddingLeft: `${paddingLeft}px`, position: 'relative', ...kineticStyle }}
      onClick={handleClick}
      data-az-nav-id={item.id}
      {...ariaProps}
    >
      {isToggle ? (
        <div className="az-menu-item-content toggle">{renderLabelBlock(isChecked ? toggleOnText : toggleOffText)}</div>
      ) : isCycler ? (
        <div className="az-menu-item-content cycler">
          {renderLabelBlock(text)}
          <span className="az-menu-item-value">{selectedOption}</span>
        </div>
      ) : (
        <div className="az-menu-item-content button">
          {renderLabelBlock(text)}
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

/**
 * One logical line of a plain-web menu label. Splits the label up-front on `\n` in the parent, then
 * renders this per line so each has its own `naturalWidth` measurement and its own solver call.
 * `whiteSpace: nowrap` prevents auto-wrap; explicit line breaks come from the `.map` in the parent.
 */
const JustifiedWebLine = ({ line, justify, rowRef, paddingLeft, textAlign }) => {
  const measureRef = useRef(null);
  const [letterSpacing, setLetterSpacing] = useState(0);
  const [fontScale, setFontScale] = useState(1);
  useEffect(() => {
    if (!justify || !line || line.length < 1) {
      setLetterSpacing(0); setFontScale(1); return;
    }
    const row = rowRef.current;
    const meas = measureRef.current;
    if (!row || !meas) return;
    const rowWidth = row.getBoundingClientRect().width - paddingLeft - 16;
    const natural = meas.getBoundingClientRect().width;
    const solved = solveHybridJustify(natural, rowWidth, line.length, WEB_BASE_FONT_PX);
    setLetterSpacing(solved.letterSpacing);
    setFontScale(solved.scale);
  }, [line, justify, paddingLeft, rowRef]);

  const style = {
    textAlign,
    letterSpacing: `${letterSpacing}px`,
    fontSize: `${WEB_BASE_FONT_PX * fontScale}px`,
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'clip',
    alignSelf: 'stretch',
  };

  return (
    <>
      <span ref={measureRef} className="az-menu-item-measurer">{line}</span>
      <span style={style}>{line}</span>
    </>
  );
};

export default MenuItem;
