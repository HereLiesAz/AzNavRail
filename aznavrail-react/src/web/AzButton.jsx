import React from 'react';
import useFitText from '../hooks/useFitText';
import AzLoad from './AzLoad';
import './AzButton.css';

/**
 * A standalone button component that matches the AzNavRail style.
 *
 * @param {object} props - The component props.
 * @param {string} props.text - The button text.
 * @param {function} props.onClick - The click handler.
 * @param {string} [props.shape='RECTANGLE'] - The shape of the button (CIRCLE, SQUARE, RECTANGLE, ROUNDED, NONE).
 * @param {string} [props.color='currentColor'] - The color of the button (border and text).
 * @param {string} [props.fillColor] - The custom background fill color of the button.
 * @param {string} [props.translucentBackgroundColor] - Exact translucent fill color (alpha
 *   included), used verbatim in place of `fillColor` and the hardcoded default-fill alpha.
 *   Distinct from the rail-level `translucentBackground` setting, which styles panels/overlays,
 *   not button fills.
 * @param {boolean} [props.isLoading=false] - Whether the button is in a loading state.
 * @param {boolean} [props.enabled=true] - Whether the button is enabled.
 * @param {object} [props.contentPadding] - Custom padding.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
const AzButton = ({
  text,
  onClick,
  shape = 'RECTANGLE',
  color = 'currentColor',
  fillColor,
  translucentBackgroundColor,
  isLoading = false,
  enabled = true,
  contentPadding,
  className = '',
  style = {},
}) => {
  // A borderless shape keeps the footprint of the base shape it names, so dropping the border never
  // reflows the rail around the item.
  const base =
    shape === 'NONE'
      ? 'RECTANGLE'
      : shape === 'NONE_SQUARE'
        ? 'SQUARE'
        : shape === 'NONE_CIRCLE'
          ? 'CIRCLE'
          : shape;
  const isFixedSize = base === 'CIRCLE' || base === 'SQUARE';
  const isNoneShape =
    shape === 'NONE' || shape === 'NONE_SQUARE' || shape === 'NONE_CIRCLE';
  const fitTextRef = useFitText();
  const textRef = isFixedSize ? fitTextRef : null;

  const shapeClass = `az-button-shape-${shape.toLowerCase().replace(/_/g, '-')}`;

  const lowerColor = color.toLowerCase();
  const computedFillColor =
    lowerColor === 'black' || lowerColor === '#000000' || lowerColor === '#000'
      ? 'rgba(255, 255, 255, 0.25)'
      : 'rgba(0, 0, 0, 0.25)';
  // `translucentBackgroundColor` is an exact, alpha-included override — it wins over `fillColor`
  // and the hardcoded default-fill alpha alike, since it exists specifically to bypass both.
  const finalFillColor =
    translucentBackgroundColor || fillColor || computedFillColor;

  // Feature 2: the stroke must read as drawn OUTSIDE the fill, never overlapping it. CSS `outline`
  // (unlike `border`) paints outside the border-box without affecting layout or background paint
  // order, which is exactly that model — so the button carries no `border` at all, only an
  // `outline` for its stroke.
  const customStyle = {
    outlineStyle: isNoneShape ? 'none' : 'solid',
    outlineWidth: isNoneShape ? 0 : '2px',
    outlineColor: color,
    outlineOffset: 0,
    color: color,
    ...style,
    ...(contentPadding ? { padding: contentPadding } : {}),
  };

  return (
    <button
      className={`az-button ${shapeClass} ${className}`}
      onClick={enabled && !isLoading ? onClick : undefined}
      disabled={!enabled}
      style={customStyle}
    >
      {/* A dedicated, absolutely-positioned fill layer covers the button's full box (including its
          padding, edge-to-edge, matching what a `background-color` on the button itself used to
          paint) — kept separate from the outline above and from `.az-button-content` below, which
          stays a normal-flow child so the padding/centering that positions the label is untouched. */}
      <div
        className="az-button-fill"
        style={{ backgroundColor: finalFillColor }}
      />
      <div className="az-button-content" style={{ opacity: isLoading ? 0 : 1 }}>
        <span
          ref={textRef}
          className="az-button-text"
          style={!isFixedSize ? { fontSize: '14px' } : {}}
        >
          {text}
        </span>
      </div>
      {isLoading && <AzLoad color={color} size={20} />}
    </button>
  );
};

export default AzButton;
