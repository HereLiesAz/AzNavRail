import React, { useRef, useEffect, useState } from 'react';
import './HelpOverlay.css';
const HelpOverlay = ({
  items,
  railWidth,
  onDismiss,
  itemBounds,
  helpList = {},
  nestedRailVisibleId = null
}) => {
  const canvasRef = useRef(null);
  const descriptionsRef = useRef(null);
  const [expandedItemId, setExpandedItemId] = useState(null);
  const allItems = React.useMemo(() => {
    const list = [...items];
    if (nestedRailVisibleId) {
      const nestedHost = items.find(i => i.id === nestedRailVisibleId);
      if (nestedHost !== null && nestedHost !== void 0 && nestedHost.nestedRailItems) {
        list.push(...nestedHost.nestedRailItems);
      }
    }
    return list;
  }, [items, nestedRailVisibleId]);
  const drawArrows = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    // Use full window size for the overlay
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.strokeStyle = 'gray';
    ctx.lineWidth = 2;
    allItems.forEach(item => {
      var _item$info, _helpList$item$id, _document$querySelect;
      const infoText = (_item$info = item.info) === null || _item$info === void 0 ? void 0 : _item$info.trim();
      const listText = (_helpList$item$id = helpList[item.id]) === null || _helpList$item$id === void 0 ? void 0 : _helpList$item$id.trim();
      if (!infoText && !listText) return;

      // Use itemBounds if provided, fallback to DOM lookup
      const itemRect = (itemBounds === null || itemBounds === void 0 ? void 0 : itemBounds[item.id]) || ((_document$querySelect = document.querySelector(`[data-az-nav-id="${item.id}"]`)) === null || _document$querySelect === void 0 ? void 0 : _document$querySelect.getBoundingClientRect());
      const descEl = document.querySelector(`[data-az-desc-id="${item.id}"]`);
      if (itemRect && descEl) {
        const descRect = descEl.getBoundingClientRect();

        // Calculate points
        // Button Point: Right-Center
        const buttonX = itemRect.right;
        const buttonY = itemRect.top + itemRect.height / 2;

        // Desc Point: Left-Center
        const descX = descRect.left;
        const descY = descRect.top + descRect.height / 2;
        const elbowX = (buttonX + descX) / 2;
        ctx.beginPath();
        ctx.moveTo(descX, descY);
        ctx.lineTo(elbowX, descY);
        ctx.lineTo(elbowX, buttonY);
        ctx.lineTo(buttonX, buttonY);
        ctx.stroke();

        // Arrowhead
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
  };
  useEffect(() => {
    drawArrows();

    // Add scroll listeners to update arrows
    const descContainer = descriptionsRef.current;
    // We also need to listen to the rail scroll if it's separate.
    // The rail is in a separate component tree structure (AzNavRail -> .rail).
    // We can try to find it via DOM or pass a ref?
    // Finding via class is easiest here.
    const railContainer = document.querySelector('.rail');
    const handleScroll = () => {
      requestAnimationFrame(drawArrows);
    };
    window.addEventListener('resize', handleScroll);
    if (descContainer) descContainer.addEventListener('scroll', handleScroll);
    if (railContainer) railContainer.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('resize', handleScroll);
      if (descContainer) descContainer.removeEventListener('scroll', handleScroll);
      if (railContainer) railContainer.removeEventListener('scroll', handleScroll);
    };
  }, [allItems, railWidth, itemBounds, helpList]);
  const isNestedRailOpen = nestedRailVisibleId !== null;
  const effectiveMarginLeft = isNestedRailOpen ? `calc(${railWidth} + 120px)` : railWidth;
  return /*#__PURE__*/React.createElement("div", {
    className: "az-help-overlay"
  }, /*#__PURE__*/React.createElement("div", {
    className: "az-help-descriptions",
    style: {
      marginLeft: effectiveMarginLeft
    },
    ref: descriptionsRef
  }, allItems.map(item => {
    var _item$info2, _helpList$item$id2;
    const infoText = (_item$info2 = item.info) === null || _item$info2 === void 0 ? void 0 : _item$info2.trim();
    const listText = (_helpList$item$id2 = helpList[item.id]) === null || _helpList$item$id2 === void 0 ? void 0 : _helpList$item$id2.trim();
    if (!infoText && !listText) return null;
    const titleText = (item.text || '').trim() || `Item ${item.id}`;
    const isExpanded = expandedItemId === item.id;
    return /*#__PURE__*/React.createElement("div", {
      key: item.id,
      className: "az-help-card",
      "data-az-desc-id": item.id,
      onClick: () => setExpandedItemId(isExpanded ? null : item.id),
      style: {
        cursor: 'pointer'
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        fontWeight: 'bold',
        color: 'var(--md-sys-color-primary, #6200ee)',
        marginBottom: '8px'
      }
    }, titleText), infoText && /*#__PURE__*/React.createElement("div", {
      style: {
        display: '-webkit-box',
        WebkitLineClamp: isExpanded ? 'unset' : 1,
        WebkitBoxOrient: 'vertical',
        overflow: 'hidden',
        textOverflow: 'ellipsis'
      }
    }, infoText), listText && /*#__PURE__*/React.createElement("div", {
      style: {
        marginTop: infoText ? '8px' : '0px',
        display: '-webkit-box',
        WebkitLineClamp: isExpanded ? 'unset' : 1,
        WebkitBoxOrient: 'vertical',
        overflow: 'hidden',
        textOverflow: 'ellipsis'
      }
    }, listText), isExpanded && /*#__PURE__*/React.createElement("div", {
      style: {
        marginTop: '8px',
        fontSize: '0.8em',
        color: 'gray'
      }
    }, "Tap to collapse"));
  })), /*#__PURE__*/React.createElement("canvas", {
    ref: canvasRef,
    className: "az-help-canvas"
  }), /*#__PURE__*/React.createElement("button", {
    className: "az-fab-exit",
    onClick: onDismiss
  }, "\u2715"));
};
export default HelpOverlay;
//# sourceMappingURL=HelpOverlay.js.map