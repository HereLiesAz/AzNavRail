/**
 * Closed-form solver for the hybrid **kerning + font-scale** justification used by the WP7-style
 * menu drawer. Mirrors `internal/AzJustify.kt` on Compose.
 *
 * Given the natural width of a label at its base font size, the row width to fill, the character
 * count, and the base font size, returns `{ scale, letterSpacing }` such that
 * `scale · naturalWidth + (n − 1) · letterSpacing === rowWidth` while never letting
 * `letterSpacing / (scale · baseFontSize)` exceed `maxTrackingRatio`.
 *
 * ### Math
 *
 * With `n = charCount`, `W₀ = naturalWidth`, `f₀ = baseFontSize`, `W = rowWidth`,
 * `α = maxTrackingRatio`, unknowns `s` (font scale) & `k` (letter-spacing px):
 *
 *  - Total width: `s·W₀ + (n − 1)·k = W`
 *  - Kerning cap: `k ≤ α · s · f₀`
 *
 * Phase A tries `s = 1`, `k = (W − W₀) / (n − 1)`. If `k ≤ α · f₀`, accept.
 *
 * Phase B saturates the kerning: set `k = α · s · f₀` in the equation, solve
 * `s = W / (W₀ + (n − 1) · α · f₀)`, then `k = α · s · f₀`. Clamp `s` at `[1, maxFontScale]`
 * (if `s` clamps high the label ends up slightly shorter than the row — accept a small gap over
 * further distortion).
 */
export function solveHybridJustify(
  naturalWidth: number,
  rowWidth: number,
  charCount: number,
  baseFontSize: number,
  maxTrackingRatio = 0.15,
  maxFontScale = 1.5,
  minFontScale = 0.5,
): { scale: number; letterSpacing: number } {
  if (charCount < 1 || naturalWidth <= 0 || rowWidth <= 0) {
    return { scale: 1, letterSpacing: 0 };
  }
  // Shrink branch — natural width overflows the row. Scale DOWN so the label fits on one line
  // (no kerning). Callers should also disable auto-wrap so an imperfect fit clips instead of
  // pushing a single letter onto a new line ("Generat\ne", "Projec\nt").
  if (naturalWidth >= rowWidth) {
    const scale = Math.max(minFontScale, Math.min(1, rowWidth / naturalWidth));
    return { scale, letterSpacing: 0 };
  }
  if (charCount < 2) return { scale: 1, letterSpacing: 0 };
  const gaps = charCount - 1;
  const kAtScale1 = (rowWidth - naturalWidth) / gaps;
  if (kAtScale1 <= maxTrackingRatio * baseFontSize) {
    return { scale: 1, letterSpacing: kAtScale1 };
  }
  const denom = naturalWidth + gaps * maxTrackingRatio * baseFontSize;
  const scale = Math.max(1, Math.min(maxFontScale, rowWidth / denom));
  const letterSpacing = maxTrackingRatio * scale * baseFontSize;
  return { scale, letterSpacing };
}
