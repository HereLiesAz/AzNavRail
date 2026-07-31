package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzItemAlert
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.sqrt

/**
 * An upward-pointing triangle with rounded corners, inscribed in the button's box — the glyph a rail
 * item turns into while it owns a notice or warning popup.
 *
 * Each corner is cut back along both of its edges by [cornerRadius] and bridged with a quadratic
 * curve through the original vertex, which is the cheap, well-behaved way to round an arbitrary
 * polygon: no arc-centre trigonometry, and the curve stays inside the untouched triangle.
 */
internal data class AzRoundedTriangleShape(val cornerRadius: Dp = 6.dp) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        return Outline.Generic(roundedTrianglePath(size, radiusPx))
    }
}

/** The rounded-triangle [Path] for a box of [size], corners cut back by [radiusPx]. */
internal fun roundedTrianglePath(size: Size, radiusPx: Float): Path {
    val apex = Offset(size.width / 2f, 0f)
    val bottomRight = Offset(size.width, size.height)
    val bottomLeft = Offset(0f, size.height)
    val vertices = listOf(apex, bottomRight, bottomLeft)

    val path = Path()
    if (size.width <= 0f || size.height <= 0f) return path

    // Never cut back more than half an edge, or adjacent corners would overrun each other on a
    // button small enough that the radius rivals the triangle's own edges.
    fun distance(a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return sqrt(dx * dx + dy * dy)
    }

    fun towards(from: Offset, to: Offset, by: Float): Offset {
        val length = distance(from, to)
        if (length == 0f) return from
        val t = (by / length).coerceIn(0f, 0.5f)
        return Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
    }

    val shortestEdge = minOf(
        distance(vertices[0], vertices[1]),
        distance(vertices[1], vertices[2]),
        distance(vertices[2], vertices[0]),
    )
    val cut = min(radiusPx, shortestEdge / 2f)

    vertices.forEachIndexed { index, vertex ->
        val previous = vertices[(index + vertices.size - 1) % vertices.size]
        val next = vertices[(index + 1) % vertices.size]
        val entry = towards(vertex, previous, cut)
        val exit = towards(vertex, next, cut)
        if (index == 0) path.moveTo(entry.x, entry.y) else path.lineTo(entry.x, entry.y)
        path.quadraticTo(vertex.x, vertex.y, exit.x, exit.y)
    }
    path.close()
    return path
}

/**
 * The Compose [Shape] an [AzButtonShape] draws as — the single mapping every call site shares, so a
 * new member (like [AzButtonShape.TRIANGLE]) can never be silently missed by one of them.
 */
internal fun AzButtonShape.toComposeShape(): Shape = when (this) {
    AzButtonShape.CIRCLE, AzButtonShape.NONE_CIRCLE -> CircleShape
    AzButtonShape.SQUARE, AzButtonShape.NONE_SQUARE -> RectangleShape
    AzButtonShape.RECTANGLE -> RectangleShape
    AzButtonShape.TRIANGLE -> AzRoundedTriangleShape()
    AzButtonShape.NONE -> RectangleShape
}

/**
 * The stroke colour for an alerted item. Both levels are yellow — a notice sits back in amber, a
 * warning comes forward in a saturated hazard yellow — so either reads instantly as "look at me"
 * against the rail's normal accent.
 */
internal fun AzItemAlert.color(): Color = when (this) {
    AzItemAlert.NOTICE -> Color(0xFFFFC107)
    AzItemAlert.WARNING -> Color(0xFFFFD400)
}

/**
 * A rounded regular polygon with [vertices] corners, inscribed in [size], rotated so a flat edge
 * sits at the bottom. Corners are cut back by [cornerFraction] of the shortest edge and bridged with
 * a quadratic through the original vertex — the same cheap, well-behaved rounding
 * [AzRoundedTriangleShape] uses.
 */
internal fun azRoundedPolygonPoints(size: Size, vertices: Int): List<Offset> {
    val n = vertices.coerceAtLeast(3)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = min(size.width, size.height) / 2f
    // Start at the top so an odd-vertex polygon points up, which reads as "upright" while spinning.
    val start = -PI.toFloat() / 2f
    return (0 until n).map { i ->
        val a = start + 2f * PI.toFloat() * i / n
        Offset(cx + r * cos(a), cy + r * sin(a))
    }
}

/**
 * The path of a rounded polygon **part-way between** two vertex counts.
 *
 * Both polygons are resampled to the same point count (their least common multiple, capped) so the
 * two outlines can be interpolated vertex-for-vertex; [fraction] `0` is [fromVertices], `1` is
 * [toVertices]. That resampling is what lets a square become a pentagon without the outline
 * visibly tearing, which is the whole point of morphing rather than cross-fading.
 */
internal fun azMorphedPolygonPath(
    size: Size,
    fromVertices: Int,
    toVertices: Int,
    fraction: Float,
    cornerFraction: Float = 0.35f,
): Path {
    val a = azRoundedPolygonPoints(size, fromVertices)
    val b = azRoundedPolygonPoints(size, toVertices)
    // Resampling to a shared, generous point count keeps both outlines dense enough that the
    // interpolation reads as a shape changing rather than as points sliding.
    val samples = 96
    val blended = (0 until samples).map { i ->
        val t = i.toFloat() / samples
        val pa = azSampleClosedPolyline(a, t)
        val pb = azSampleClosedPolyline(b, t)
        Offset(
            pa.x + (pb.x - pa.x) * fraction,
            pa.y + (pb.y - pa.y) * fraction,
        )
    }
    return azRoundedPathThrough(blended, cornerFraction)
}

/** The point at normalised distance [t] around the closed polyline through [points]. */
private fun azSampleClosedPolyline(points: List<Offset>, t: Float): Offset {
    if (points.isEmpty()) return Offset.Zero
    val n = points.size
    val scaled = t.coerceIn(0f, 1f) * n
    val i = scaled.toInt().coerceIn(0, n - 1)
    val f = scaled - i
    val p0 = points[i]
    val p1 = points[(i + 1) % n]
    return Offset(p0.x + (p1.x - p0.x) * f, p0.y + (p1.y - p0.y) * f)
}

/** A closed path through [points], corners softened by [cornerFraction] of each adjacent edge. */
private fun azRoundedPathThrough(points: List<Offset>, cornerFraction: Float): Path {
    val path = Path()
    if (points.size < 3) return path
    val n = points.size
    fun mid(p: Offset, q: Offset, f: Float) = Offset(p.x + (q.x - p.x) * f, p.y + (q.y - p.y) * f)
    val f = cornerFraction.coerceIn(0f, 0.5f)
    points.forEachIndexed { i, v ->
        val prev = points[(i + n - 1) % n]
        val next = points[(i + 1) % n]
        val entry = mid(v, prev, f)
        val exit = mid(v, next, f)
        if (i == 0) path.moveTo(entry.x, entry.y) else path.lineTo(entry.x, entry.y)
        path.quadraticTo(v.x, v.y, exit.x, exit.y)
    }
    path.close()
    return path
}

/**
 * The shape an alerted item wears **part-way** into its warning triangle.
 *
 * [progress] `0` is the item's normal [base] shape, `1` is the rounded triangle. Everything between
 * is a real geometric blend, so the item *becomes* a warning rather than being replaced by a picture
 * of one — the transformation is the message, and a hard swap throws it away.
 *
 * A circle is approximated by a 32-gon for the blend; at `progress == 0` the exact
 * [AzButtonShape.toComposeShape] is used instead, so an un-alerted circle stays a true circle.
 */
internal data class AzAlertMorphShape(
    val base: AzButtonShape,
    val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (progress <= 0f) return base.toComposeShape().createOutline(size, layoutDirection, density)
        val from = when (base) {
            AzButtonShape.CIRCLE, AzButtonShape.NONE_CIRCLE -> 32
            AzButtonShape.SQUARE, AzButtonShape.RECTANGLE,
            AzButtonShape.NONE, AzButtonShape.NONE_SQUARE -> 4
            AzButtonShape.TRIANGLE -> 3
        }
        return Outline.Generic(
            azMorphedPolygonPath(
                size = size,
                fromVertices = from,
                toVertices = 3,
                fraction = progress.coerceIn(0f, 1f),
                cornerFraction = 0.18f,
            )
        )
    }
}
