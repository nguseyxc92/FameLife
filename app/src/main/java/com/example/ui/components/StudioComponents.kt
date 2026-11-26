package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ViewerTimePoint
import com.example.ui.theme.FameMint
import com.example.ui.theme.GamingLiveViolet
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.PhotoLiveAmber
import com.example.ui.theme.PhotoLiveRose
import com.example.ui.theme.ShortLiveCoral
import com.example.ui.theme.ShortLiveNeon
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VideoLiveGold
import java.util.Locale
import kotlin.math.abs

fun formatCompactCount(value: Int): String = when {
    value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000f)
    value >= 10_000 -> String.format(Locale.US, "%.1fK", value / 1_000f)
    value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000f)
    else -> value.toString()
}

fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

@Composable
fun SimulationWatermarkPill(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = StudioObsidian.copy(alpha = 0.78f),
        border = androidx.compose.foundation.BorderStroke(1.dp, FameMint.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(FameMint)
            )
            Text(
                text = if (compact) "SIMULATED" else "SIMULATED • FAMELIFE",
                style = MaterialTheme.typography.labelSmall,
                color = FameMint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ViewerAvatar(
    initials: String,
    seed: Int,
    size: Dp = 30.dp,
    hasHighlightRing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        Pair(FameMint, Color(0xFF0E4D3B)),
        Pair(PhotoLiveRose, PhotoLiveAmber),
        Pair(ShortLiveNeon, ShortLiveCoral),
        Pair(GamingLiveViolet, InfoCyan),
        Pair(VideoLiveGold, Color(0xFFE65100)),
        Pair(InfoCyan, Color(0xFF1D4ED8))
    )
    val pair = palette[abs(seed) % palette.size]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(pair.first, pair.second)))
            .then(
                if (hasHighlightRing) Modifier.border(1.5.dp, FameMint, CircleShape)
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.take(2),
            color = TextPrimary,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ViewerGrowthChart(
    points: List<ViewerTimePoint>,
    accentColor: Color = FameMint,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(170.dp)
) {
    if (points.size < 2) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(StudioSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Not enough timeline samples yet",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        return
    }

    val maxViewers = (points.maxOfOrNull { it.viewerCount } ?: 10).coerceAtLeast(10)
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(StudioSurfaceElevated.copy(alpha = 0.65f))
            .padding(14.dp)
    ) {
        val w = size.width
        val h = size.height

        // Subtle horizontal grid lines
        for (g in 1..3) {
            val y = h * (g / 4f)
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val linePath = Path()
        val fillPath = Path()

        points.forEachIndexed { index, pt ->
            val x = if (points.size <= 1) 0f else (index.toFloat() / (points.size - 1)) * w
            val y = h - (pt.viewerCount.toFloat() / maxViewers.toFloat()).coerceIn(0f, 1f) * (h * 0.88f)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == points.lastIndex) {
                fillPath.lineTo(x, h)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(accentColor.copy(alpha = 0.34f), accentColor.copy(alpha = 0.0f))
            )
        )
        drawPath(
            path = linePath,
            color = accentColor,
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
