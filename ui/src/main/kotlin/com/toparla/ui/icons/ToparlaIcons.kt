package com.toparla.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Uygulama içi çizgi ikonları (blueprint C6: yuvarlak uçlu, ince çizgi). Material Symbols kütüphanesi eklenmeden,
 * onaylı taslaktaki (`docs/tasarim/2026-10-09-ikon-iskelet/`) çizimlerle aynı yollar. Renk `Icon(tint = …)` ile verilir.
 */
object ToparlaIcons {
    private const val VIEWPORT = 24f
    private const val STROKE = 1.75f
    private const val STROKE_BOLD = 2f

    val Now: ImageVector = icon("Now", STROKE_BOLD, "M8,12 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0", "M12,3 v2 M12,19 v2 M3,12 h2 M19,12 h2")
    val Inbox: ImageVector = icon("Inbox", STROKE, "M3,13 l3,-8 h12 l3,8 v6 H3 z", "M3,13 h5 l1,2 h6 l1,-2 h5")
    val Plan: ImageVector = icon("Plan", STROKE, "M6,5 h12 a2,2 0 0,1 2,2 v11 a2,2 0 0,1 -2,2 H6 a2,2 0 0,1 -2,-2 V7 a2,2 0 0,1 2,-2 z", "M4,10 h16 M9,3 v4 M15,3 v4")
    val Flow: ImageVector = icon("Flow", STROKE, "M3,9 c3,-3 6,3 9,0 s6,3 9,0", "M3,15 c3,-3 6,3 9,0 s6,3 9,0")
    val Gunes: ImageVector = icon("Gunes", STROKE, "M7,12 a5,5 0 1,0 10,0 a5,5 0 1,0 -10,0", "M12,3.5 v1.5 M12,19 v1.5 M3.5,12 h1.5 M19,12 h1.5")
    val Mic: ImageVector = icon("Mic", STROKE_BOLD, "M12,3 a3,3 0 0,1 3,3 v5 a3,3 0 0,1 -6,0 V6 a3,3 0 0,1 3,-3 z", "M5,11 a7,7 0 0,0 14,0 M12,18 v3")
    val Keyboard: ImageVector = icon("Keyboard", STROKE_BOLD, "M5,6 h14 a2,2 0 0,1 2,2 v8 a2,2 0 0,1 -2,2 H5 a2,2 0 0,1 -2,-2 V8 a2,2 0 0,1 2,-2 z", "M7,14 h10")
    val Person: ImageVector = icon("Person", STROKE, "M8,8 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0", "M4,20 c1.6,-3.6 4.6,-5 8,-5 s6.4,1.4 8,5")
    val Send: ImageVector = icon("Send", STROKE_BOLD, "M4,12 L20,4 L14,20 L11,13 z", "M11,13 L20,4")

    private fun icon(name: String, stroke: Float, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = VIEWPORT.dp,
            defaultHeight = VIEWPORT.dp,
            viewportWidth = VIEWPORT,
            viewportHeight = VIEWPORT,
        )
        paths.forEach { data ->
            builder.addPath(
                pathData = addPathNodes(data),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
