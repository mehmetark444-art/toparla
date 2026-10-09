package com.toparla.ui.theme

/** Tema kipi (blueprint C2). Varsayılan koyu. */
enum class ThemeMode { DARK, LIGHT, AMOLED }

/** Bir vurgu renginin ana tonu ve türevleri. `0xAARRGGBB`. */
data class AccentTones(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
)

/**
 * Vurgu aileleri (blueprint C2). Koyu tonlar blueprint'teki gibidir. Açık tonlar karar 0016 gereği
 * beyaz, zemin ve ikincil kart üzerinde ≥ 4,5:1 olacak kadar koyulaştırıldı; türevler aynı kuralla
 * hesaplandı ve `ContrastTest` her birini sınar.
 */
enum class Accent(val light: AccentTones, val dark: AccentTones) {
    SAGE(
        light = AccentTones(primary = 0xFF47705F, onPrimary = 0xFFFFFFFF, primaryContainer = 0xFFD7E8E0, onPrimaryContainer = 0xFF1F3B31),
        dark = AccentTones(primary = 0xFF8DBBA9, onPrimary = 0xFF12261E, primaryContainer = 0xFF2F4A3F, onPrimaryContainer = 0xFFD7E8E0),
    ),
    SKY(
        light = AccentTones(primary = 0xFF466D8E, onPrimary = 0xFFFFFFFF, primaryContainer = 0xFFD6E2ED, onPrimaryContainer = 0xFF233646),
        dark = AccentTones(primary = 0xFF93B9DD, onPrimary = 0xFF1D252C, primaryContainer = 0xFF3F4952, onPrimaryContainer = 0xFFDFEAF5),
    ),
    CLAY(
        light = AccentTones(primary = 0xFF876148, onPrimary = 0xFFFFFFFF, primaryContainer = 0xFFEEE0D8, onPrimaryContainer = 0xFF463325),
        dark = AccentTones(primary = 0xFFDDAB86, onPrimary = 0xFF2C221B, primaryContainer = 0xFF554538, onPrimaryContainer = 0xFFF5E6DB),
    ),
    LAVENDER(
        light = AccentTones(primary = 0xFF6B6393, onPrimary = 0xFFFFFFFF, primaryContainer = 0xFFE0DEEC, onPrimaryContainer = 0xFF312E44),
        dark = AccentTones(primary = 0xFFB1A9DA, onPrimary = 0xFF23222C, primaryContainer = 0xFF484452, onPrimaryContainer = 0xFFE8E5F4),
    ),
    DUSTY_ROSE(
        light = AccentTones(primary = 0xFF895C6A, onPrimary = 0xFFFFFFFF, primaryContainer = 0xFFECDDE2, onPrimaryContainer = 0xFF442E34),
        dark = AccentTones(primary = 0xFFD9A6B5, onPrimary = 0xFF2B2124, primaryContainer = 0xFF544346, onPrimaryContainer = 0xFFF4E4E9),
    ),
    SAND(
        light = AccentTones(primary = 0xFF776846, onPrimary = 0xFFFFFFFF, primaryContainer = 0xFFE9E4D8, onPrimaryContainer = 0xFF403826),
        dark = AccentTones(primary = 0xFFD2C08F, onPrimary = 0xFF2A261D, primaryContainer = 0xFF524B3B, onPrimaryContainer = 0xFFF2ECDD),
    ),
}

/**
 * Bir temanın bütün renk jetonları (blueprint C2 + karar 0016). Bileşenler bu değerleri doğrudan değil,
 * `ToparlaTheme` üzerinden (`MaterialTheme.colorScheme` ve `ToparlaTheme.extended`) okur.
 *
 * `cardBorder` ve `cardShadow` saydamlık taşır (kartın neredeyse görünmez sınırı ve sıcak tonlu gölgesi);
 * metin rengi olarak kullanılmaz.
 */
data class ToparlaPalette(
    val background: Long,
    val surface: Long,
    val surfaceVariant: Long,
    val surfaceDim: Long,
    val onSurface: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
    val accent: AccentTones,
    val secondary: Long,
    val secondaryContainer: Long,
    val tertiary: Long,
    val tertiaryContainer: Long,
    val sun: Long,
    val carried: Long,
    val carriedContainer: Long,
    val onCarriedContainer: Long,
    val success: Long,
    val info: Long,
    val infoContainer: Long,
    val urge: Long,
    val urgeContainer: Long,
    val critical: Long,
    val onCritical: Long,
    val cardBorder: Long,
    val cardShadow: Long,
)

/**
 * `primarySoft` (metin eyleminin zemini): vurgunun bu saydamlıkta yüzeye bindirilmiş hâli (karar 0016).
 * Kontrastı `ContrastTest` bindirilmiş rengiyle ölçer.
 */
internal const val PRIMARY_SOFT_ALPHA_DARK = 0.10f
internal const val PRIMARY_SOFT_ALPHA_LIGHT = 0.08f

/** Kip ve vurguya göre paleti kurar. */
object Palettes {
    private val dark = ToparlaPalette(
        background = 0xFF1B1917,
        surface = 0xFF242220,
        surfaceVariant = 0xFF2E2B28,
        surfaceDim = 0xFF151311,
        onSurface = 0xFFECE7DF,
        onSurfaceVariant = 0xFFAFA79B,
        outline = 0xFF4A453F,
        accent = Accent.SAGE.dark,
        secondary = 0xFFDDAB86,
        secondaryContainer = 0xFF4A3626,
        tertiary = 0xFFB1A9DA,
        tertiaryContainer = 0xFF3B3756,
        sun = 0xFFF2B661,
        carried = 0xFFE0BC6A,
        carriedContainer = 0xFF4A3E1E,
        onCarriedContainer = 0xFFE0BC6A,
        success = 0xFF8CC2A0,
        info = 0xFF8FB2D6,
        infoContainer = 0xFF2A3A4A,
        urge = 0xFFC79BC0,
        urgeContainer = 0xFF43303F,
        critical = 0xFFE5645A,
        onCritical = 0xFF1B0E0D,
        cardBorder = 0x0DFFFFFF,
        cardShadow = 0x73060402,
    )

    // Karar 0016: koyu tema aynı, AMOLED'de gölge görünmediği için kart sınırı biraz daha belirgin.
    private val amoled = dark.copy(
        background = 0xFF000000,
        surface = 0xFF0C0C0C,
        surfaceVariant = 0xFF171615,
        surfaceDim = 0xFF000000,
        outline = 0xFF2F2C29,
        cardBorder = 0x1AFFFFFF,
        cardShadow = 0x00000000,
    )

    // Karar 0016: açık temada metin ve ikon olarak kullanılan renkler ≥ 4,5:1 olacak kadar koyu.
    private val light = ToparlaPalette(
        background = 0xFFFAF7F2,
        surface = 0xFFFFFFFF,
        surfaceVariant = 0xFFF1ECE4,
        surfaceDim = 0xFFE9E3D9,
        onSurface = 0xFF2B2825,
        onSurfaceVariant = 0xFF625B52,
        outline = 0xFFD8D0C4,
        accent = Accent.SAGE.light,
        secondary = 0xFF876148,
        secondaryContainer = 0xFFF1DFD1,
        tertiary = 0xFF675E9C,
        tertiaryContainer = 0xFFE6E2F3,
        sun = 0xFFE8A23A,
        carried = 0xFFC9A04A,
        carriedContainer = 0xFFF6EBCF,
        onCarriedContainer = 0xFF7C5B12,
        success = 0xFF3E7553,
        info = 0xFF506B88,
        infoContainer = 0xFFDCE7F1,
        urge = 0xFF845D7E,
        urgeContainer = 0xFFF1E3EF,
        critical = 0xFFB3362A,
        onCritical = 0xFFFFFFFF,
        cardBorder = 0x1247705F,
        cardShadow = 0x1A5C401E,
    )

    fun of(mode: ThemeMode, accent: Accent = Accent.SAGE): ToparlaPalette = when (mode) {
        ThemeMode.DARK -> dark.copy(accent = accent.dark)
        ThemeMode.AMOLED -> amoled.copy(accent = accent.dark)
        ThemeMode.LIGHT -> light.copy(accent = accent.light)
    }
}
