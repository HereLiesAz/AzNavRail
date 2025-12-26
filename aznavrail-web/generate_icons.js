import fs from 'fs';
import path from 'path';
import { createCanvas } from 'canvas';

// Helper to create an icon
function createIcon(filename, size, color) {
  const canvas = createCanvas(size, size);
  const ctx = canvas.getContext('2d');

  // Fill background
  ctx.fillStyle = `rgb(${color.r}, ${color.g}, ${color.b})`;
  ctx.fillRect(0, 0, size, size);

  // Draw text
  ctx.fillStyle = 'white';
  ctx.font = `${Math.floor(size / 3)}px sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText('App', size / 2, size / 2);

  const buffer = canvas.toBuffer('image/png');
  const dir = 'public';
  if (!fs.existsSync(dir)){
      fs.mkdirSync(dir);
  }
  fs.writeFileSync(path.join(dir, filename), buffer);
  console.log(`Generated ${filename}`);
}

try {
  // PWA Icons
  createIcon('pwa-192x192.png', 192, { r: 0, g: 0, b: 255 });
  createIcon('pwa-512x512.png', 512, { r: 255, g: 0, b: 0 });
  createIcon('apple-touch-icon.png', 180, { r: 128, g: 0, b: 128 });
  createIcon('favicon.png', 32, { r: 0, g: 255, b: 0 }); // png favicon is easier in node than ico without extra libs

  console.log("Icons generated successfully.");
} catch (e) {
  console.error("Error generating icons:", e);
}
