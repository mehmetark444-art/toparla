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

    // Güneş: iç içe iki halka (avatarın yüzsüz dairesi + ışık halkası). Şimdi'nin ışınlı güneşinden ayrılsın diye ışınsız.
    val Gunes: ImageVector = icon("Gunes", STROKE, "M8,12 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0", "M3.5,12 a8.5,8.5 0 1,0 17,0 a8.5,8.5 0 1,0 -17,0")
    val Mic: ImageVector = icon("Mic", STROKE_BOLD, "M12,3 a3,3 0 0,1 3,3 v5 a3,3 0 0,1 -6,0 V6 a3,3 0 0,1 3,-3 z", "M5,11 a7,7 0 0,0 14,0 M12,18 v3")
    val Keyboard: ImageVector = icon("Keyboard", STROKE_BOLD, "M5,6 h14 a2,2 0 0,1 2,2 v8 a2,2 0 0,1 -2,2 H5 a2,2 0 0,1 -2,-2 V8 a2,2 0 0,1 2,-2 z", "M7,14 h10")
    val Person: ImageVector = icon("Person", STROKE, "M8,8 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0", "M4,20 c1.6,-3.6 4.6,-5 8,-5 s6.4,1.4 8,5")
    val Send: ImageVector = icon("Send", STROKE_BOLD, "M4,12 L20,4 L14,20 L11,13 z", "M11,13 L20,4")

    // Hatırlatma ekranları (F2.35–F2.38).
    val Close: ImageVector = icon("Close", STROKE_BOLD, "M6,6 L18,18", "M18,6 L6,18")
    val Back: ImageVector = icon("Back", STROKE_BOLD, "M15,5 L8,12 L15,19")
    val Plus: ImageVector = icon("Plus", STROKE_BOLD, "M12,5 V19", "M5,12 H19")
    val CheckCircle: ImageVector = icon("CheckCircle", STROKE_BOLD, "M3,12 a9,9 0 1,0 18,0 a9,9 0 1,0 -18,0", "M8,12.5 L11,15.5 L16,9.5")
    val AlertCircle: ImageVector = icon("AlertCircle", STROKE_BOLD, "M3,12 a9,9 0 1,0 18,0 a9,9 0 1,0 -18,0", "M12,7 V13", "M12,16.5 V17")
    val Shield: ImageVector = icon("Shield", STROKE, "M12,3 L19,6 V12 C19,16 16,19 12,21 C8,19 5,16 5,12 V6 z", "M9,12 L11.5,14.5 L15,10")

    // Kurulum sihirbazı ve sınama sonuçları (F2.36, F2.37).
    val Check: ImageVector = icon("Check", STROKE_BOLD, "M5,12.5 L9.5,17 L19,7.5")
    val InfoCircle: ImageVector = icon("InfoCircle", STROKE_BOLD, "M3,12 a9,9 0 1,0 18,0 a9,9 0 1,0 -18,0", "M12,11 V17", "M12,7.5 V8")
    val Bell: ImageVector = icon("Bell", STROKE_BOLD, "M12,3 a6,6 0 0,0 -6,6 v4 l-2,3 h16 l-2,-3 V9 a6,6 0 0,0 -6,-6 z", "M10,20 a2,2 0 0,0 4,0")
    val Lock: ImageVector = icon("Lock", STROKE_BOLD, "M7,11 h10 a2,2 0 0,1 2,2 v5 a2,2 0 0,1 -2,2 H7 a2,2 0 0,1 -2,-2 v-5 a2,2 0 0,1 2,-2 z", "M8,11 V8 a4,4 0 0,1 8,0 v3")

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
