import { useRef, useState, useEffect } from 'react';

/**
 * A hook that resizes text to fit within its container.
 * This is a simple implementation for the AzNavRail UI kit.
 */
export default function useFitText(options: any = {}) {
  const [fontSize, setFontSize] = useState(100);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    let min = 10;
    let max = 100;
    let mid;

    const trySize = (size: number) => {
      el.style.fontSize = `${size}px`;
      return el.scrollWidth <= el.clientWidth && el.scrollHeight <= el.clientHeight;
    };

    while (min <= max) {
      mid = Math.floor((min + max) / 2);
      if (trySize(mid)) {
        min = mid + 1;
      } else {
        max = mid - 1;
      }
    }

    setFontSize(max);
    el.style.fontSize = `${max}px`;
  }, [options]);

  return { fontSize, ref };
}
