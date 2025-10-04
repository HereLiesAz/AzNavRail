import { useEffect, useRef } from 'react';

const useFitText = () => {
  const ref = useRef(null);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    const container = element.parentElement;
    if (!container) return;

    const resizeText = () => {
      let min = 1, max = 20; // Min and max font size in px
      let fontSize;

      const isOverflowing = () => element.scrollWidth > container.clientWidth || element.scrollHeight > container.clientHeight;

      // Binary search for the best font size
      while (min <= max) {
        fontSize = Math.floor((min + max) / 2);
        element.style.fontSize = `${fontSize}px`;

        if (isOverflowing()) {
          max = fontSize - 1;
        } else {
          min = fontSize + 1;
        }
      }
      // After the loop, max is the largest size that fits.
      element.style.fontSize = `${max}px`;
    };

    resizeText();

    // Optional: Add a resize observer to handle container resize
    const resizeObserver = new ResizeObserver(resizeText);
    resizeObserver.observe(container);

    return () => resizeObserver.disconnect();
  }, [ref]);

  return ref;
};

export default useFitText;