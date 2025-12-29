import React, { useRef, useEffect } from 'react';
import './HelpOverlay.css';

const HelpOverlay = ({ items, railWidth, onDismiss }) => {
  const canvasRef = useRef(null);
  const descriptionsRef = useRef(null);

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

    items.forEach(item => {
        if (!item.info) return;

        // Find rail button or menu item
        const itemEl = document.querySelector(`[data-az-nav-id="${item.id}"]`);
        const descEl = document.querySelector(`[data-az-desc-id="${item.id}"]`);

        if (itemEl && descEl) {
            const itemRect = itemEl.getBoundingClientRect();
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

  }, [items, railWidth]);

  return (
    <div className="az-help-overlay">
      <div
        className="az-help-descriptions"
        style={{ marginLeft: railWidth }}
        ref={descriptionsRef}
      >
        {items.filter(i => i.info).map(item => (
            <div key={item.id} className="az-help-card" data-az-desc-id={item.id}>
                {item.info}
            </div>
        ))}
      </div>
      <canvas ref={canvasRef} className="az-help-canvas" />
      <button className="az-fab-exit" onClick={onDismiss}>✕</button>
    </div>
  );
};

export default HelpOverlay;
