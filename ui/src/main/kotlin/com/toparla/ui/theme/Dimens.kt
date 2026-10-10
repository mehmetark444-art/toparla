package com.toparla.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Boşluk (blueprint C4 + karar 0016): 4 dp ızgara; kenar ve kart içi 24, bölümler arası 32. */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp

    val screenEdge = 24.dp
    val cardPadding = 24.dp
    val betweenCards = 12.dp
    val betweenSections = 32.dp
}

/** Köşe yarıçapları (blueprint C4). */
object Shapes {
    val chip: Shape = CircleShape
    val card: Shape = RoundedCornerShape(20.dp)
    val nowCard: Shape = RoundedCornerShape(28.dp)
    val sheetTop: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val button: Shape = RoundedCornerShape(16.dp)
    val buttonLarge: Shape = RoundedCornerShape(20.dp)
    val bubble: Shape = RoundedCornerShape(20.dp)
}

/** Ölçüler (blueprint C4, C6, C7). Dokunma hedefi en az 48 dp; birincil eylem 56 dp. */
object Sizes {
    val minTouch = 48.dp
    val primaryAction = 56.dp
    val settingRowMin = 64.dp
    val chipVisual = 36.dp
    val emptyShape = 72.dp
    val captureFab = 64.dp
    val fabIcon = 28.dp
    val badgeIcon = 14.dp
    val topBar = 56.dp
    val avatarSmall = 24.dp
    val avatarMedium = 40.dp
    val avatarLarge = 72.dp
    val statusDot = 10.dp
    val ringStroke = 4.dp
    val progressRing = 96.dp
    val undoRing = 28.dp
    val undoRingStroke = 2.dp

    // Hatırlatma Sağlığı, kurulum sihirbazı ve sınama (onaylı taslaklar 9 ve 10 Ekim 2026).
    val rowIcon = 24.dp
    val compactRowMin = 48.dp
    val infoValueMax = 200.dp
    val stepBadge = 32.dp
    val stepDot = 8.dp
    val stepDotCurrent = 24.dp
    val testRing = 200.dp
    val testRingStroke = 12.dp
    val heroIcon = 36.dp
    val lockBadge = 40.dp
}

/** Kart yüksekliği: yumuşak, sıcak tonlu gölge + neredeyse görünmez sınır (karar 0016). */
object Elevation {
    val none = 0.dp
    val card = 12.dp
    val small = 6.dp
    val border = 1.dp
}
