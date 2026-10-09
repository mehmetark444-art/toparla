package com.toparla.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Boş durum görseli: tek renkli soyut şekil (blueprint C6). Karakter, yüz, maskot yok. */
enum class EmptyShape { LEAF, RING, PATH, WAVE }

// Şekiller kutunun oranlarıyla çizilir; sayılar kutu genişliğinin kesirleridir.
private const val STROKE_RATIO = 0.05f
private const val RING_RADIUS = 0.42f
private const val EDGE = 0.1f
private const val FAR_EDGE = 0.9f
private const val THIRD = 0.33f
private const val TWO_THIRDS = 0.66f
private const val WAVE_TOP = 0.38f
private const val WAVE_BOTTOM = 0.62f
private const val WAVE_GAP = 0.18f

internal fun DrawScope.drawEmptyShape(shape: EmptyShape, color: Color) {
    val w = size.width
    val stroke = Stroke(width = w * STROKE_RATIO, cap = StrokeCap.Round)
    when (shape) {
        EmptyShape.RING -> drawCircle(color = color, radius = w * RING_RADIUS, style = stroke)
        EmptyShape.LEAF -> drawPath(
            path = Path().apply {
                moveTo(w * EDGE, w * FAR_EDGE)
                quadraticTo(w * EDGE, w * EDGE, w * FAR_EDGE, w * EDGE)
                quadraticTo(w * FAR_EDGE, w * FAR_EDGE, w * EDGE, w * FAR_EDGE)
                lineTo(w * TWO_THIRDS, w * THIRD)
            },
            color = color,
            style = stroke,
        )
        EmptyShape.PATH -> drawPath(
            path = Path().apply {
                moveTo(w * EDGE, w * FAR_EDGE)
                cubicTo(w * FAR_EDGE, w * TWO_THIRDS, w * EDGE, w * THIRD, w * FAR_EDGE, w * EDGE)
            },
            color = color,
            style = stroke,
        )
        EmptyShape.WAVE -> listOf(WAVE_TOP, WAVE_BOTTOM).forEach { y ->
            drawPath(
                path = Path().apply {
                    moveTo(w * EDGE, w * y)
                    cubicTo(w * THIRD, w * (y - WAVE_GAP), w * TWO_THIRDS, w * (y + WAVE_GAP), w * FAR_EDGE, w * y)
                },
                color = color,
                style = stroke,
            )
        }
    }
    // Merkez noktası: şekle "dinlenme" hissi veren tek küçük işaret.
    if (shape == EmptyShape.RING) drawCircle(color = color, radius = w * STROKE_RATIO, center = Offset(w / 2, w / 2))
}
