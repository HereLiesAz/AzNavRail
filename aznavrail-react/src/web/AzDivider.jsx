import React from 'react';
import './AzDivider.css';

/**
 * A divider component for use within AzNavRail.
 * Renders a horizontal line with standard padding.
 *
 * The default color is `currentColor` so the divider inherits the surrounding font color and
 * belongs to the same visual family as the text next to it — not a muted outline.
 *
 * @param {object} props
 * @param {string} [props.color='currentColor'] - CSS color; defaults to inheriting the parent's text color.
 */
const AzDivider = ({ color = 'currentColor' }) => {
  return <div className="az-divider" style={{ backgroundColor: color }} />;
};

export default AzDivider;
