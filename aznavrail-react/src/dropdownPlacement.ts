import { AzDropdownAlignment } from './types';

/** The vertical band a drop-down anchor sits in. */
export type DropdownVert = 'top' | 'center' | 'bottom';
/** The horizontal band a drop-down anchor sits in (start/end are LTR-relative). */
export type DropdownHoriz = 'start' | 'center' | 'end';

export interface DropdownAnchor {
  vert: DropdownVert;
  horiz: DropdownHoriz;
  /** True when anchored to the bottom — the panel then unfolds upward (above the icon). */
  isBottom: boolean;
}

/**
 * Decomposes an {@link AzDropdownAlignment} (e.g. `'bottom-end'`) into its vertical/horizontal
 * bands and the unfold direction. Shared by the web and React Native drop-down renderers so both
 * platforms place the hamburger trigger identically. Defaults to `top-start`.
 */
export function parseDropdownAnchor(alignment?: AzDropdownAlignment | string): DropdownAnchor {
  const a = (alignment || 'top-start') as string;
  const vert: DropdownVert = a.startsWith('top') ? 'top' : a.startsWith('bottom') ? 'bottom' : 'center';
  const horiz: DropdownHoriz = a.endsWith('start') ? 'start' : a.endsWith('end') ? 'end' : 'center';
  return { vert, horiz, isBottom: vert === 'bottom' };
}
