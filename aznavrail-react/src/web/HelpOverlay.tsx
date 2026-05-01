/// <reference lib="dom" />
import React, { useRef, useEffect, useState, useCallback } from 'react';
import { AzNavItem, AzTutorial } from '../types';
import { useAzWebTutorialController } from './AzTutorialController';
import './HelpOverlay.css';

interface HelpOverlayProps {
  items: AzNavItem[];
  railWidth: string | number;
  onDismiss: () => void;
  itemBounds?: Record<string, { x: number; y: number; width: number; height: number }>;
  helpList?: Record<string, string>;
  nestedRailVisibleId?: string | null;
  tutorials?: Record<string, AzTutorial>;
}

const HelpOverlay: React.FC<HelpOverlayProps> = ({
  items,
  railWidth,
  onDismiss,
  itemBounds = {},
  helpList = {},
  nestedRailVisibleId = null,
  tutorials = {},
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const descriptionsRef = useRef<HTMLDivElement>(null);
  const [expandedItemId, setExpandedItemId] = useState<string | null>(null);
  const tutorialController = useAzWebTutorialController();

  const allItems = React.useMemo(() => {
    const list = [...items];
    if (nestedRailVisibleId) {
      const host = items.find((i) => i.id === nestedRailVisibleId);
      if (host?.nestedRailItems) list.push(...host.nestedRailItems);
    }
    return list;
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
          const hasTutorial = !!tutorials[item.id];

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

              {hasTutorial && !isExpanded && (
                <div style={tutorialHintStyle}>Tutorial available</div>
              )}

              {hasTutorial && isExpanded && (
                <button
                  style={startTutorialButtonStyle}
                  onClick={(e) => {
                    e.stopPropagation();
                    tutorialController.startTutorial(item.id);
                    onDismiss();
                  }}
                >
                  Start Tutorial
                </button>
              )}

              {isExpanded && !hasTutorial && (
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

const tutorialHintStyle: React.CSSProperties = {
  marginTop: 6,
  fontSize: '0.75em',
  color: '#aaa',
  fontStyle: 'italic',
};

const startTutorialButtonStyle: React.CSSProperties = {
  marginTop: 12,
  background: '#6200EE',
  color: '#fff',
  border: 'none',
  borderRadius: 16,
  padding: '8px 16px',
  fontSize: 13,
  fontWeight: 600,
  cursor: 'pointer',
};

export default HelpOverlay;
