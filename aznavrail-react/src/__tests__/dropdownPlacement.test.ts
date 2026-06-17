import { parseDropdownAnchor } from '../dropdownPlacement';
import { AzDropdownAlignment } from '../types';

describe('parseDropdownAnchor', () => {
  it('defaults to top-start when no alignment is given', () => {
    expect(parseDropdownAnchor()).toEqual({ vert: 'top', horiz: 'start', isBottom: false });
  });

  it('decomposes each anchor into its bands', () => {
    expect(parseDropdownAnchor(AzDropdownAlignment.TOP_END)).toEqual({
      vert: 'top', horiz: 'end', isBottom: false,
    });
    expect(parseDropdownAnchor(AzDropdownAlignment.CENTER)).toEqual({
      vert: 'center', horiz: 'center', isBottom: false,
    });
    expect(parseDropdownAnchor(AzDropdownAlignment.CENTER_START)).toEqual({
      vert: 'center', horiz: 'start', isBottom: false,
    });
  });

  it('flags only bottom anchors for upward unfolding', () => {
    expect(parseDropdownAnchor(AzDropdownAlignment.BOTTOM_START).isBottom).toBe(true);
    expect(parseDropdownAnchor(AzDropdownAlignment.BOTTOM_CENTER).isBottom).toBe(true);
    expect(parseDropdownAnchor(AzDropdownAlignment.BOTTOM_END).isBottom).toBe(true);
    expect(parseDropdownAnchor(AzDropdownAlignment.TOP_START).isBottom).toBe(false);
    expect(parseDropdownAnchor(AzDropdownAlignment.CENTER_END).isBottom).toBe(false);
  });

  it('accepts raw string alignment values', () => {
    expect(parseDropdownAnchor('bottom-end')).toEqual({ vert: 'bottom', horiz: 'end', isBottom: true });
  });
});
