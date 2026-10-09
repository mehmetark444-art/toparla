package com.toparla.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Yazı ölçeği (blueprint C3 + karar 0016). Sistem yazı tipi; özel font gömülmez.
 * Bir ekranda en çok dört boyut (32 / 20 / 16 / 13) ve iki ağırlık (Regular, SemiBold).
 * Rakamlar eşit genişliklidir (`tnum`): saat ve sayaçlar değişirken metin kaymaz.
 */
@Immutable
data class ToparlaTextStyles(
    /** Şimdi kartı başlığı, büyük sayı (dürtü sayacı, kritik hatırlatma saati değil: o `displayLarge`). */
    val displayNow: TextStyle,
    /** Ekran başlığı. */
    val headline: TextStyle,
    /** Kart ve bölüm başlığı. */
    val title: TextStyle,
    /** Ana metin, Güneş mesajı. */
    val bodyL: TextStyle,
    /** İkincil metin. */
    val body: TextStyle,
    /** Düğme, chip. */
    val label: TextStyle,
    /** Kaynak, zaman, rozet. */
    val caption: TextStyle,
    /** Yalnız Geliştirici menüsü; dört boyut sınırından muaf. */
    val mono: TextStyle,
)

private const val NUMERIC = "tnum"

private fun style(size: Int, line: Int, weight: FontWeight) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    fontFeatureSettings = NUMERIC,
)

val ToparlaTypeScale = ToparlaTextStyles(
    displayNow = style(size = 32, line = 40, weight = FontWeight.SemiBold),
    headline = style(size = 20, line = 28, weight = FontWeight.SemiBold),
    title = style(size = 20, line = 28, weight = FontWeight.SemiBold),
    bodyL = style(size = 16, line = 24, weight = FontWeight.Normal),
    body = style(size = 16, line = 24, weight = FontWeight.Normal),
    label = style(size = 16, line = 24, weight = FontWeight.SemiBold),
    caption = style(size = 13, line = 18, weight = FontWeight.Normal),
    mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp),
)

/** Material bileşenleri de aynı ölçeği kullansın diye M3 rollerine eşleme. */
fun ToparlaTextStyles.toTypography(): Typography = Typography(
    displayLarge = displayNow,
    displayMedium = displayNow,
    displaySmall = displayNow,
    headlineLarge = displayNow,
    headlineMedium = headline,
    headlineSmall = headline,
    titleLarge = title,
    titleMedium = label,
    titleSmall = label,
    bodyLarge = bodyL,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = label,
    labelMedium = caption,
    labelSmall = caption,
)
