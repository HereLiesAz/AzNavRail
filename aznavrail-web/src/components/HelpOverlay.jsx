import React, { useState, useLayoutEffect, useRef } from 'react';
import './HelpOverlay.css';
import './AzTextBox.css'; // Reuse style

const HelpOverlay = ({ items }) => {
  const [layout, setLayout] = useState([]);
  const canvasRef = useRef(null);

  // Helper to find visible elements and calculate layout
  useLayoutEffect(() => {
    // 1. Find all visible elements with [data-az-nav-id]
    const elements = Array.from(document.querySelectorAll('[data-az-nav-id]'));

    // 2. Map them to item info
    // Flatten items to lookup info by id
    const itemMap = {};
    const flatten = (list) => {
        list.forEach(i => {
            itemMap[i.id] = i;
            if (i.items) flatten(i.items);
        });
    };
    flatten(items);

    const visibleItems = elements.map(el => {
        const id = el.getAttribute('data-az-nav-id');
        const item = itemMap[id];
        if (!item || !item.info) return null;

        const rect = el.getBoundingClientRect();
        return {
            id,
            info: item.info,
            targetRect: rect,
            targetY: rect.top + rect.height / 2,
            targetRight: rect.right
        };
    }).filter(Boolean);

    // 3. Layout Algorithm
    // Sort by Y position
    visibleItems.sort((a, b) => a.targetY - b.targetY);

    const layouts = [];
    const TOP_MARGIN = window.innerHeight * 0.2; // 20%
    const SPACING = 32; // Double space
    let currentY = TOP_MARGIN;

    visibleItems.forEach((item, index) => {
        // First pass rough estimate, used to seed the state
        let idealY = item.targetY - 24;
        let top = Math.max(currentY, idealY);

        layouts.push({
            ...item,
            top,
            left: item.targetRight + 100, // This is overridden by absolute positioning later if needed, but we keep it safe
            index // Add index for lane calculation
        });

        currentY = top + 80;
    });

    setLayout(layouts);

  }, [items]);

  const descriptionRefs = useRef({});

  useLayoutEffect(() => {
      // 2nd pass: Adjustment
      if (layout.length === 0) return;

      const newLayout = [...layout];
      let changed = false;
      const TOP_MARGIN = window.innerHeight * 0.2;
      const SPACING = 32;
      let currentBottom = TOP_MARGIN;

      newLayout.forEach((item) => {
          const el = descriptionRefs.current[item.id];
          if (!el) return;
          const height = el.getBoundingClientRect().height;

          let idealTop = item.targetY - (height / 2);
          let top = Math.max(currentBottom, idealTop);

          if (top !== item.top) {
              item.top = top;
              changed = true;
          }

          currentBottom = top + height + SPACING;
      });

      if (changed) {
          setLayout(newLayout);
      }

      // Draw canvas
      const canvas = canvasRef.current;
      if (!canvas) return;

      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
      const ctx = canvas.getContext('2d');
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.strokeStyle = 'red';
      ctx.fillStyle = 'red';
      ctx.lineWidth = 2;

      newLayout.forEach((item) => {
          const el = descriptionRefs.current[item.id];
          if (!el) return;
          const boxRect = el.getBoundingClientRect();

          const startX = item.targetRight;
          const startY = item.targetY;
          const endX = boxRect.left;
          const endY = boxRect.top + boxRect.height / 2;

          ctx.beginPath();
          ctx.moveTo(startX, startY);

          // Elbow logic (Lanes)
          const railRight = startX;
          const laneStep = 8; // Match Android 8dp
          const basePadding = 16; // Match Android 16dp

          const elbowX = railRight + basePadding + (item.index * laneStep);

          ctx.lineTo(elbowX, startY); // Should this be from startX?
          // Wait, path is: Description -> Elbow -> Button.
          // Or Button -> Elbow -> Description.
          // My canvas code starts at startX (Button).
          // Android code:
          /*
            path.moveTo(descPoint.x, descPoint.y) // Description
            path.lineTo(elbowX, descPoint.y)
            path.lineTo(elbowX, buttonPoint.y)
            path.lineTo(buttonPoint.x, buttonPoint.y) // Button
          */
          // Let's match Android order to be safe, although visual result is same.
          // Start from Description (endX, endY)

          ctx.beginPath();
          ctx.moveTo(endX, endY);
          ctx.lineTo(elbowX, endY);
          ctx.lineTo(elbowX, startY);
          ctx.lineTo(startX, startY);
          ctx.stroke();

          // Arrowhead at Button (startX, startY)
          // Solid triangle pointing left.
          // Tip at startX. Base at startX + size.
          const arrowSize = 12; // Match Android
          ctx.beginPath();
          ctx.moveTo(startX, startY);
          ctx.lineTo(startX + arrowSize, startY - arrowSize/2);
          ctx.lineTo(startX + arrowSize, startY + arrowSize/2);
          ctx.closePath();
          ctx.fill();
      });

  }, [layout, items]);

  return (
    <div className="az-help-overlay">
        <canvas ref={canvasRef} className="az-help-canvas" />
        {layout.map(item => (
            <div
                key={item.id}
                ref={el => descriptionRefs.current[item.id] = el}
                className="az-help-description az-textbox-wrapper multiline"
                style={{
                    position: 'absolute',
                    top: item.top,
                    // Left offset needs to be enough to clear the widest lane.
                    // Max lane offset = railRight + 16 + (N * 8).
                    // If we assume N < 10, that's ~100px from rail.
                    // We set left: item.targetRight + 100 in initial layout, but let's enforce it here.
                    left: 200, // Fixed left margin to keep boxes aligned? Or flexible?
                    // User said "info boxes should resize their width".
                    // Let's set `left` to a safe distance, e.g. 200px or dynamic based on lanes.
                    // Android used `railWidth + 64.dp`.
                    // Here we don't know rail width easily without measuring, but `item.targetRight` gives us the rail edge.
                    // Let's us `item.targetRight + 64 + (item.index * 8)`.
                    // No, boxes should probably align left? Or flow?
                    // Android code: offset x = railWidth + 64.dp. Constant.
                    // Let's stick to a constant large enough margin.
                    // item.targetRight is ~80px (collapsed rail) or 260px (expanded).
                    // But rail collapses in help mode? Not necessarily, but usually.
                    // Let's use `left: item.targetRight + 100`.
                    left: item.targetRight + 100,
                    right: 32,
                    width: 'auto',
                    maxWidth: '400px',
                }}
            >
                {item.info}
            </div>
        ))}
    </div>
  );
};

export default HelpOverlay;
