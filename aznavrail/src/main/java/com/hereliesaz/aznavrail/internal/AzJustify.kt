package com.hereliesaz.aznavrail.internal

/**
 * Closed-form solver for the hybrid **kerning + font-scale** justification used by the WP7-style
 * menu drawer. The label fills the row via a mix of letter-spacing *and* a font-size scale, without
 * ever exceeding the kerning ratio allowed by the current font size.
 *
 * Given:
 *  - `naturalWidthPx` — measured width of the label at its base font size (`baseFontSizePx`),
 *    single-line, no letter-spacing.
 *  - `rowWidthPx`     — available row width the label should fill.
 *  - `charCount`      — number of characters in the label (`≥ 2`).
 *  - `maxTrackingRatio` — the largest allowed `letterSpacing / fontSize` before we grow the font.
 *  - `maxFontScale` — hard cap on how much the font can grow (protects against runaway growth
 *    when the label is very short relative to the row).
 *
 * Returns the pair (`fontScale`, `letterSpacingPx`) that lands the label exactly on
 * `rowWidthPx`, subject to the two constraints. If the label is already wider than the row
 * the result is `(1f, 0f)` — no justification, let the label render at its natural size.
 *
 * ### Math (closed form, one branch)
 *
 * With `n = charCount`, `W₀ = naturalWidthPx`, `f₀ = baseFontSizePx`, `W = rowWidthPx`,
 * `α = maxTrackingRatio`, and unknowns `s` (font scale) & `k` (letter-spacing px):
 *
 *  - Total width: `s·W₀ + (n − 1)·k = W`
 *  - Kerning cap: `k ≤ α · s · f₀`
 *
 * **Phase A** (kerning suffices) — try `s = 1`, `k = (W − W₀) / (n − 1)`. If `k ≤ α · f₀`
 * we accept it and return.
 *
 * **Phase B** (kerning saturated) — set `k = α · s · f₀`, substitute:
 * `s · W₀ + (n − 1) · α · s · f₀ = W`, so `s = W / (W₀ + (n − 1) · α · f₀)`. Then
 * `k = α · s · f₀`. Clamp `s` at [`1`, `maxFontScale`]; if it clamped high, the label ends up
 * shorter than the row (accept a small gap rather than distort further).
 */
internal fun solveHybridJustify(
    naturalWidthPx: Float,
    rowWidthPx: Float,
    charCount: Int,
    baseFontSizePx: Float,
    maxTrackingRatio: Float = 0.15f,
    maxFontScale: Float = 1.5f,
): Pair<Float, Float> {
    if (charCount < 2 || naturalWidthPx <= 0f || rowWidthPx <= 0f) return 1f to 0f
    if (naturalWidthPx >= rowWidthPx) return 1f to 0f

    val n = charCount
    val gaps = (n - 1).toFloat()

    // Phase A: try to fill with kerning alone at s = 1.
    val kAtScale1 = (rowWidthPx - naturalWidthPx) / gaps
    if (kAtScale1 <= maxTrackingRatio * baseFontSizePx) {
        return 1f to kAtScale1
    }

    // Phase B: kerning saturated → grow the font so `k = α·s·f₀` fills the row exactly.
    val denom = naturalWidthPx + gaps * maxTrackingRatio * baseFontSizePx
    val s = (rowWidthPx / denom).coerceIn(1f, maxFontScale)
    val k = maxTrackingRatio * s * baseFontSizePx
    return s to k
}
