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
    AzButtonShape.CIRCLE -> CircleShape
    AzButtonShape.SQUARE -> RectangleShape
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
