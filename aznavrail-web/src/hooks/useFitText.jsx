import { useEffect, useRef } from 'react';

/**
 * A custom React hook that dynamically adjusts the font size of a text element
 * to fit within its parent container.
 *
 * This hook uses a binary search algorithm to efficiently find the optimal font size.
 * It also uses a ResizeObserver to re-calculate the font size whenever the container's
 * dimensions change.
 *
 * @returns {React.RefObject} A React ref object that should be attached to the text
 * element you want to resize.
 */
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

    // Re-run the text fitting logic when the container is resized.
    const resizeObserver = new ResizeObserver(resizeText);
    resizeObserver.observe(container);

    // Cleanup the observer when the component unmounts.
    return () => resizeObserver.disconnect();
  }, [ref]);

  return ref;
};

export default useFitText;