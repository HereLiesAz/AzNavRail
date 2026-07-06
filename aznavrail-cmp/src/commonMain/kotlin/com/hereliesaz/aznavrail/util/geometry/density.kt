package com.hereliesaz.aznavrail.util.geometry

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import kotlin.math.roundToInt

// Unit-conversion extension functions for use in non-composable lambdas (e.g., DrawScope, Layout).
// Composable overloads below delegate to LocalDensity.

// DP
/** Converts [dp] to [TextUnit] (sp), returning [TextUnit.Unspecified] for unspecified input. */
fun Density.dpToSp(dp: Dp) = if (dp.isSpecified) dp.toSp() else TextUnit.Unspecified
/** Converts [dp] to a float-precision pixel value; returns `Float.NaN` for unspecified input. */
fun Density.dpToFloatPx(dp: Dp) = if (dp.isSpecified) dp.toPx() else Float.NaN
/** Converts [dp] to truncated integer pixels; returns 0 for unspecified input. */
fun Density.dpToIntPx(dp: Dp) = if (dp.isSpecified) dp.toPx().toInt() else 0
/** Converts [dp] to rounded integer pixels; returns 0 for unspecified input. */
fun Density.dpRoundToPx(dp: Dp) = if (dp.isSpecified) dp.roundToPx() else 0

/** Composable accessor for [Density.dpToSp] using [LocalDensity]. */
@Composable
fun Dp.toSp() = LocalDensity.current.dpToSp(this)
/** Composable accessor for [Density.dpToFloatPx] using [LocalDensity]. */
@Composable
fun Dp.toFloatPx() = LocalDensity.current.dpToFloatPx(this)
/** Composable accessor for [Density.dpToIntPx] using [LocalDensity]. */
@Composable
fun Dp.toIntPx() = LocalDensity.current.dpToIntPx(this)
/** Composable accessor for [Density.dpRoundToPx] using [LocalDensity]. */
@Composable
fun Dp.roundToPx() = LocalDensity.current.dpRoundToPx(this)

/** Wraps this [Dp] into a square [DpSize] (width == height == this). Returns [DpSize.Unspecified] when this is unspecified. */
fun Dp.toRecDpSize() = if (isSpecified) DpSize(this, this) else DpSize.Unspecified
/** Wraps this [Dp] into a square [DpOffset] (x == y == this). Returns [DpOffset.Unspecified] when this is unspecified. */
fun Dp.toRecDpOffset() = if (isSpecified) DpOffset(this, this) else DpOffset.Unspecified


// TEXT UNIT
/** Converts [sp] to [Dp]; returns [Dp.Unspecified] for unspecified input. */
fun Density.spToDp(sp: TextUnit) = if (sp.isSpecified) sp.toDp() else Dp.Unspecified
/** Converts [sp] to float pixels; returns `Float.NaN` for unspecified input. */
fun Density.spToFloatPx(sp: TextUnit) = if (sp.isSpecified) sp.toPx() else Float.NaN
/** Converts [sp] to truncated integer pixels; returns 0 for unspecified input. */
fun Density.spToIntPx(sp: TextUnit) = if (sp.isSpecified) sp.toPx().toInt() else 0
/** Converts [sp] to rounded integer pixels; returns 0 for unspecified input. */
fun Density.spRoundToPx(sp: TextUnit) = if (sp.isSpecified) sp.roundToPx() else 0

/** Composable accessor for [Density.spToDp] using [LocalDensity]. */
@Composable
fun TextUnit.toDp() = LocalDensity.current.spToDp(this)
/** Composable accessor for [Density.spToFloatPx] using [LocalDensity]. */
@Composable
fun TextUnit.toFloatPx() = LocalDensity.current.spToFloatPx(this)
/** Composable accessor for [Density.spToIntPx] using [LocalDensity]. */
@Composable
fun TextUnit.toIntPx() = LocalDensity.current.spToIntPx(this)
/** Composable accessor for [Density.spRoundToPx] using [LocalDensity]. */
@Composable
fun TextUnit.roundToPx() = LocalDensity.current.spRoundToPx(this)


// FLOAT
/** Converts pixel float [px] to [Dp]; returns [Dp.Unspecified] when [px] is not finite. */
fun Density.floatPxToDp(px: Float) = if (px.isFinite()) px.toDp() else Dp.Unspecified
/** Converts pixel float [px] to [TextUnit]; returns [TextUnit.Unspecified] when [px] is not finite. */
fun Density.floatPxToSp(px: Float) = if (px.isFinite()) px.toSp() else TextUnit.Unspecified

/** Composable accessor for [Density.floatPxToDp] using [LocalDensity]. */
@Composable
fun Float.toDp() = LocalDensity.current.floatPxToDp(this)
/** Composable accessor for [Density.floatPxToSp] using [LocalDensity]. */
@Composable
fun Float.toSp() = LocalDensity.current.floatPxToSp(this)

/** Truncates this finite float to integer pixels; returns 0 when not finite. */
fun Float.toIntPx() = if (isFinite()) toInt() else 0
/** Rounds this finite float to integer pixels; returns 0 when not finite. */
fun Float.roundToPx() = if (isFinite()) roundToInt() else 0

/** Wraps this float as a square [Size] (width == height == this). Returns [Size.Unspecified] when not finite. */
fun Float.toRecSize() = if (isFinite()) Size(this, this) else Size.Unspecified
/** Wraps this float as a square [Offset] (x == y == this). Returns [Offset.Unspecified] when not finite. */
fun Float.toRecOffset() = if (isFinite()) Offset(this, this) else Offset.Unspecified


// INT
/** Converts integer pixels [px] to [Dp]. */
fun Density.intPxToDp(px: Int) = px.toDp()
/** Converts integer pixels [px] to [TextUnit] (sp). */
fun Density.intPxToSp(px: Int) = px.toSp()

/** Composable accessor for [Density.intPxToDp] using [LocalDensity]. */
@Composable
fun Int.toDp() = LocalDensity.current.intPxToDp(this)
/** Composable accessor for [Density.intPxToSp] using [LocalDensity]. */
@Composable
fun Int.toSp() = LocalDensity.current.intPxToSp(this)

/** Promotes this integer pixel count to a float. */
fun Int.toFloatPx() = toFloat()

/** Wraps this int as a square [IntSize] (width == height == this). */
fun Int.toRecIntSize() = IntSize(this, this)
/** Wraps this int as a square [IntOffset] (x == y == this). */
fun Int.toRecIntOffset() = IntOffset(this, this)


// DP SIZE
/** Converts [dpSize] to truncated [IntSize]; returns [IntSize.Zero] when unspecified. */
fun Density.dpSizeToIntSize(dpSize: DpSize) =
    if (dpSize.isSpecified) IntSize(dpSize.width.toPx().toInt(), dpSize.height.toPx().toInt())
    else IntSize.Zero

/** Converts [dpSize] to rounded [IntSize]; returns [IntSize.Zero] when unspecified. */
fun Density.dpSizeRoundToIntSize(dpSize: DpSize) =
    if (dpSize.isSpecified) IntSize(dpSize.width.roundToPx(), dpSize.height.roundToPx())
    else IntSize.Zero

/** Converts [dpSize] to float-pixel [Size]; returns [Size.Unspecified] when unspecified. */
fun Density.dpSizeToSize(dpSize: DpSize) =
    if (dpSize.isSpecified) Size(dpSize.width.toPx(), dpSize.height.toPx())
    else Size.Unspecified

/** Composable accessor for [Density.dpSizeToIntSize] using [LocalDensity]. */
@Composable
fun DpSize.toIntSize() = LocalDensity.current.dpSizeToIntSize(this)
/** Composable accessor for [Density.dpSizeRoundToIntSize] using [LocalDensity]. */
@Composable
fun DpSize.roundToIntSize() = LocalDensity.current.dpSizeRoundToIntSize(this)
/** Composable accessor for [Density.dpSizeToSize] using [LocalDensity]. */
@Composable
fun DpSize.toSize() = LocalDensity.current.dpSizeToSize(this)

/** Returns true when this size is specified and both dimensions are strictly positive. */
fun DpSize.isSpaced() = isSpecified && width > 0.dp && height > 0.dp


// SIZE
/** Converts pixel [size] to [DpSize]; returns [DpSize.Unspecified] when unspecified. */
fun Density.sizeToDpSize(size: Size) =
    if (size.isSpecified) DpSize(size.width.toDp(), size.height.toDp())
    else DpSize.Unspecified

/** Composable accessor for [Density.sizeToDpSize] using [LocalDensity]. */
@Composable
fun Size.toDpSize() =
    if (isSpecified) LocalDensity.current.sizeToDpSize(this)
    else DpSize.Unspecified

/** Truncates this [Size] to [IntSize]; returns [IntSize.Zero] when unspecified. */
fun Size.toIntSize() =
    if (isSpecified) IntSize(width.toInt(), height.toInt())
    else IntSize.Zero

/** Returns true when this [Size] is specified and both dimensions are strictly positive. */
fun Size.isSpaced() = isSpecified && width > 0F && height > 0F


// INT SIZE
/** Converts integer [intSize] to [DpSize]. */
fun Density.intSizeToDpSize(intSize: IntSize) =
    DpSize(intSize.width.toDp(), intSize.height.toDp())

/** Composable accessor for [Density.intSizeToDpSize] using [LocalDensity]. */
@Composable
fun IntSize.toDpSize() = LocalDensity.current.intSizeToDpSize(this)

/** Promotes this [IntSize] to a float [Size]. */
@Composable
fun IntSize.toSize() = Size(width.toFloat(), height.toFloat())

/** Returns true when both [IntSize] dimensions are strictly positive. */
fun IntSize.isSpaced() = width > 0 && height > 0


// DP OFFSET
/** Converts [dpOffset] to truncated [IntOffset]; returns [IntOffset.Zero] when unspecified. */
fun Density.dpOffsetToIntOffset(dpOffset: DpOffset) =
    if (dpOffset.isSpecified) IntOffset(dpOffset.x.toPx().toInt(), dpOffset.y.toPx().toInt())
    else IntOffset.Zero

/** Converts [dpOffset] to rounded [IntOffset]; returns [IntOffset.Zero] when unspecified. */
fun Density.dpOffsetRoundToIntOffset(dpOffset: DpOffset) =
    if (dpOffset.isSpecified) IntOffset(dpOffset.x.roundToPx(), dpOffset.y.roundToPx())
    else IntOffset.Zero

/** Converts [dpOffset] to float-pixel [Offset]; returns [Offset.Unspecified] when unspecified. */
fun Density.dpOffsetToOffset(dpOffset: DpOffset) =
    if (dpOffset.isSpecified) Offset(dpOffset.x.toPx(), dpOffset.y.toPx())
    else Offset.Unspecified

/** Composable accessor for [Density.dpOffsetToIntOffset] using [LocalDensity]. */
@Composable
fun DpOffset.toIntOffset() = LocalDensity.current.dpOffsetToIntOffset(this)
/** Composable accessor for [Density.dpOffsetRoundToIntOffset] using [LocalDensity]. */
@Composable
fun DpOffset.roundToIntOffset() = LocalDensity.current.dpOffsetRoundToIntOffset(this)
/** Composable accessor for [Density.dpOffsetToOffset] using [LocalDensity]. */
@Composable
fun DpOffset.toOffset() = LocalDensity.current.dpOffsetToOffset(this)


// OFFSET
/** Converts pixel [offset] to [DpOffset]; returns [DpOffset.Unspecified] when unspecified. */
fun Density.offsetToDpOffset(offset: Offset) =
    if (offset.isSpecified) DpOffset(offset.x.toDp(), offset.y.toDp())
    else DpOffset.Unspecified

/** Composable accessor for [Density.offsetToDpOffset] using [LocalDensity]. */
@Composable
fun Offset.toDpOffset() = LocalDensity.current.offsetToDpOffset(this)

/** Truncates this [Offset] to [IntOffset]; returns [IntOffset.Zero] when unspecified. */
fun Offset.toIntOffset() =
    if (isSpecified) IntOffset(x.toInt(), y.toInt())
    else IntOffset.Zero


// INT OFFSET
/** Converts integer [intOffset] to [DpOffset]. */
fun Density.intOffsetToDpOffset(intOffset: IntOffset) =
    DpOffset(intOffset.x.toDp(), intOffset.y.toDp())

/** Composable accessor for [Density.intOffsetToDpOffset] using [LocalDensity]. */
@Composable
fun IntOffset.toDpOffset() = LocalDensity.current.intOffsetToDpOffset(this)

/** Promotes this [IntOffset] to a float [Offset]. */
fun IntOffset.toOffset() = Offset(x.toFloat(), y.toFloat())
