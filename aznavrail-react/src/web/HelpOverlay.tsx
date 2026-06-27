/// <reference lib="dom" />
import React, { useRef, useEffect, useState, useCallback } from 'react';
import { AzNavItem } from '../types';
import './HelpOverlay.css';

/** Props for {@link HelpOverlay}. */
interface HelpOverlayProps {
  /** The top-level nav items whose help cards will be rendered. */
  items: AzNavItem[];
  /** Width of the navigation rail, used to offset the card panel so it clears the rail. */
  railWidth: string | number;
  /** Called when the overlay should be dismissed. */
  onDismiss: () => void;
  /**
   * Pre-measured layout rectangles for nav items keyed by item ID; used as the source for
   * elbow-arrow connector endpoints instead of live `getBoundingClientRect` queries.
   */
  itemBounds?: Record<string, { x: number; y: number; width: number; height: number }>;
  /**
   * Additional help text keyed by item ID that supplements or replaces each item's `info` field;
   * items without an entry here and without an `info` value are omitted from the overlay.
   */
  helpList?: Record<string, string>;
  /**
   * ID of the nav item whose nested rail is currently open; when set, only that item's
   * `nestedRailItems` are shown in the overlay (the base rail's items are hidden behind
   * the nested popup, so listing their help cards would be noise).
   */
  nestedRailVisibleId?: string | null;
}

/**
 * Help/info overlay that renders a scrollable panel of cards — one per nav item that has help
 * text — and draws elbow-arrow connectors via a full-screen `<canvas>` element pointing from each
 * card back to its corresponding nav item.
 *
 * @param props.items - Nav items to render help cards for.
 * @param props.railWidth - Width of the rail, used to position the card panel.
 * @param props.onDismiss - Called when the close button is pressed.
 * @param props.itemBounds - Optional pre-measured bounds used as arrow connector endpoints.
 * @param props.helpList - Supplemental help text keyed by item ID.
 * @param props.nestedRailVisibleId - Item ID whose nested rail is open; when set, only that nested rail's items are shown in the overlay.
 */
const HelpOverlay: React.FC<HelpOverlayProps> = ({
  items,
  railWidth,
  onDismiss,
  itemBounds = {},
  helpList = {},
  nestedRailVisibleId = null,
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const descriptionsRef = useRef<HTMLDivElement>(null);
  const [expandedItemId, setExpandedItemId] = useState<string | null>(null);

  const allItems = React.useMemo(() => {
    if (nestedRailVisibleId) {
      const host = items.find((i) => i.id === nestedRailVisibleId);
      return host?.nestedRailItems ?? [];
    }
    return items;
  }, [items, nestedRailVisibleId]);

  const drawArrows = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.strokeStyle = 'gray';
    ctx.lineWidth = 2;

    allItems.forEach((item) => {
      const infoText = item.info?.trim();
      const listText = helpList[item.id]?.trim();
      if (!infoText && !listText) return;

      const itemRect =
        itemBounds[item.id] ??
        document.querySelector<HTMLElement>(`[data-az-nav-id="${item.id}"]`)?.getBoundingClientRect();
      const descEl = document.querySelector(`[data-az-desc-id="${item.id}"]`);

      if (itemRect && descEl) {
        const descRect = descEl.getBoundingClientRect();
        const buttonX = itemRect.x + itemRect.width;
        const buttonY = itemRect.y + itemRect.height / 2;
        const descX = descRect.left;
        const descY = descRect.top + descRect.height / 2;
        const elbowX = (buttonX + descX) / 2;

        ctx.beginPath();
        ctx.moveTo(descX, descY);
        ctx.lineTo(elbowX, descY);
        ctx.lineTo(elbowX, buttonY);
        ctx.lineTo(buttonX, buttonY);
        ctx.stroke();

        const arrowSize = 8;
        ctx.beginPath();
        ctx.moveTo(buttonX, buttonY);
        ctx.lineTo(buttonX + arrowSize, buttonY - arrowSize / 2);
        ctx.lineTo(buttonX + arrowSize, buttonY + arrowSize / 2);
        ctx.closePath();
        ctx.fillStyle = 'gray';
        ctx.fill();
      }
    });
  }, [allItems, itemBounds, helpList]);

  useEffect(() => {
    drawArrows();
    const descContainer = descriptionsRef.current;
    const railContainer = document.querySelector('.rail');
    const handleScroll = () => requestAnimationFrame(drawArrows);
    window.addEventListener('resize', handleScroll);
    descContainer?.addEventListener('scroll', handleScroll);
    railContainer?.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('resize', handleScroll);
      descContainer?.removeEventListener('scroll', handleScroll);
      railContainer?.removeEventListener('scroll', handleScroll);
    };
  }, [drawArrows]);

  const isNestedRailOpen = nestedRailVisibleId !== null;
  const effectiveMarginLeft = isNestedRailOpen
    ? `calc(${typeof railWidth === 'number' ? `${railWidth}px` : railWidth} + 120px)`
    : typeof railWidth === 'number' ? `${railWidth}px` : railWidth;

  return (
    <div className="az-help-overlay">
      <div
        className="az-help-descriptions"
        style={{ marginLeft: effectiveMarginLeft }}
        ref={descriptionsRef}
      >
        {allItems.map((item) => {
          const infoText = item.info?.trim();
          const listText = helpList[item.id]?.trim();
          if (!infoText && !listText) return null;

          const titleText = (item.text ?? '').trim() || `Item ${item.id}`;
          const isExpanded = expandedItemId === item.id;

          return (
            <div
              key={item.id}
              className="az-help-card"
              data-az-desc-id={item.id}
              onClick={() => setExpandedItemId(isExpanded ? null : item.id)}
              style={{ cursor: 'pointer' }}
            >
              <div style={cardTitleStyle}>{titleText}</div>

              {infoText && (
                <div style={isExpanded ? {} : clampStyle}>{infoText}</div>
              )}
              {listText && (
                <div style={{ ...(isExpanded ? {} : clampStyle), marginTop: infoText ? 8 : 0 }}>
                  {listText}
                </div>
              )}

              {isExpanded && (
                <div style={{ marginTop: 8, fontSize: '0.8em', color: 'gray' }}>
                  Tap to collapse
                </div>
              )}
            </div>
          );
        })}
      </div>
      <canvas ref={canvasRef} className="az-help-canvas" />
      <button className="az-fab-exit" onClick={onDismiss}>{'✕'}</button>
    </div>
  );
};

const cardTitleStyle: React.CSSProperties = {
  fontWeight: 'bold',
  color: 'var(--md-sys-color-primary, #6200ee)',
  marginBottom: 8,
};

const clampStyle: React.CSSProperties = {
  display: '-webkit-box',
  WebkitLineClamp: 1,
  WebkitBoxOrient: 'vertical',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
};

export default HelpOverlay;
