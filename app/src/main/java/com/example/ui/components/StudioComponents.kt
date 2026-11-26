package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
fun VerifiedBadgeIcon(
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.56f)
                .clip(CircleShape)
                .background(Color.White)
        )
        Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = "Verified creator",
            tint = Color(0xFF1D9BF0),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ViewerAvatar(
    initials: String,
    seed: Int,
    size: Dp = 30.dp,
    hasHighlightRing: Boolean = false,
    hasProfilePicture: Boolean = true,
    imageUri: String? = null,
    modifier: Modifier = Modifier
) {
    val ringModifier = if (hasHighlightRing) {
        Modifier.border(1.5.dp, FameMint, CircleShape)
    } else {
        Modifier.border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
    }

    if (!imageUri.isNullOrBlank()) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Profile picture",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF2B2F3B))
                .then(ringModifier)
        )
        return
    }

    if (!hasProfilePicture) {
        // ~10% of audience: clean default no-profile-picture silhouette
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF2B2F3B))
                .then(ringModifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Default avatar",
                tint = Color(0xFF8C95A6),
                modifier = Modifier.size(size * 0.62f)
            )
        }
        return
    }

    // ~90% of audience: rich varied procedural portrait / lifestyle profile picture
    val s = abs(seed)
    val bgPalettes = listOf(
        Pair(Color(0xFF2B4C7E), Color(0xFF14213D)),
        Pair(PhotoLiveRose, Color(0xFF6B1D3B)),
        Pair(Color(0xFF1F6E5C), Color(0xFF0D2E27)),
        Pair(GamingLiveViolet, Color(0xFF2D1B69)),
        Pair(PhotoLiveAmber, Color(0xFF8A3800)),
        Pair(InfoCyan, Color(0xFF1E3A8A)),
        Pair(ShortLiveCoral, Color(0xFF5D1D48)),
        Pair(Color(0xFF475569), Color(0xFF1E293B))
    )
    val bg = bgPalettes[s % bgPalettes.size]
    val skinTones = listOf(
        Color(0xFFF3C6A5),
        Color(0xFFE0A97E),
        Color(0xFFC68652),
        Color(0xFF9E6238),
        Color(0xFF6F3E21),
        Color(0xFFF6D7C3)
    )
    val hairColors = listOf(
        Color(0xFF181414),
        Color(0xFF2E1E16),
        Color(0xFF4A2B18),
        Color(0xFF8D5524),
        Color(0xFFD99B38),
        Color(0xFF7E22CE)
    )
    val shirtColors = listOf(
        FameMint,
        PhotoLiveRose,
        Color(0xFFF8FAFC),
        Color(0xFF111827),
        VideoLiveGold,
        ShortLiveNeon,
        GamingLiveViolet,
        Color(0xFFEF4444)
    )

    val skin = skinTones[(s / 7) % skinTones.size]
    val hair = hairColors[(s / 13) % hairColors.size]
    val shirt = shirtColors[(s / 19) % shirtColors.size]
    val pfpMode = (s / 29) % 10 // 0..6 portrait (70%), 7..9 lifestyle/scenic PFP (30%)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(ringModifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Scenic / studio backdrop
            drawRect(brush = Brush.linearGradient(listOf(bg.first, bg.second)))

            // Soft bokeh circle accent
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = w * 0.28f,
                center = Offset(w * 0.78f, h * 0.22f)
            )

            when (pfpMode) {
                7 -> {
                    // Sunset & mountain landscape PFP
                    drawCircle(
                        color = VideoLiveGold,
                        radius = w * 0.16f,
                        center = Offset(w * 0.62f, h * 0.34f)
                    )
                    val mountain = Path().apply {
                        moveTo(0f, h)
                        lineTo(w * 0.32f, h * 0.48f)
                        lineTo(w * 0.58f, h * 0.72f)
                        lineTo(w * 0.78f, h * 0.52f)
                        lineTo(w, h)
                        close()
                    }
                    drawPath(mountain, color = Color(0xFF0F172A).copy(alpha = 0.85f))
                }
                8 -> {
                    // Cat / pet PFP
                    val earL = Path().apply {
                        moveTo(w * 0.26f, h * 0.46f)
                        lineTo(w * 0.30f, h * 0.22f)
                        lineTo(w * 0.46f, h * 0.36f)
                        close()
                    }
                    val earR = Path().apply {
                        moveTo(w * 0.74f, h * 0.46f)
                        lineTo(w * 0.70f, h * 0.22f)
                        lineTo(w * 0.54f, h * 0.36f)
                        close()
                    }
                    val fur = if (s % 2 == 0) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                    drawPath(earL, color = fur)
                    drawPath(earR, color = fur)
                    drawCircle(color = fur, radius = w * 0.25f, center = Offset(w * 0.5f, h * 0.55f))
                    drawCircle(color = Color(0xFF1E293B), radius = w * 0.035f, center = Offset(w * 0.41f, h * 0.52f))
                    drawCircle(color = Color(0xFF1E293B), radius = w * 0.035f, center = Offset(w * 0.59f, h * 0.52f))
                }
                9 -> {
                    // Night crescent + star aesthetic PFP
                    drawCircle(color = Color(0xFFFDE68A), radius = w * 0.22f, center = Offset(w * 0.52f, h * 0.46f))
                    drawCircle(color = bg.first, radius = w * 0.19f, center = Offset(w * 0.60f, h * 0.40f))
                    drawCircle(color = Color.White, radius = w * 0.035f, center = Offset(w * 0.28f, h * 0.32f))
                    drawCircle(color = Color.White, radius = w * 0.025f, center = Offset(w * 0.74f, h * 0.68f))
                }
                else -> {
                    // Human portrait PFP with shoulders, hair variation, glasses/hat
                    val hairStyle = (s / 3) % 5
                    val hasGlasses = (s / 11) % 4 == 0

                    // Long back hair if style == 2 or 3
                    if (hairStyle == 2 || hairStyle == 3) {
                        drawCircle(
                            color = hair,
                            radius = w * 0.27f,
                            center = Offset(w * 0.5f, h * 0.44f)
                        )
                    }

                    // Shoulders / hoodie
                    drawOval(
                        color = shirt,
                        topLeft = Offset(w * 0.16f, h * 0.68f),
                        size = Size(w * 0.68f, h * 0.50f)
                    )

                    // Neck
                    drawRect(
                        color = skin,
                        topLeft = Offset(w * 0.42f, h * 0.55f),
                        size = Size(w * 0.16f, h * 0.18f)
                    )

                    // Head
                    drawCircle(
                        color = skin,
                        radius = w * 0.21f,
                        center = Offset(w * 0.5f, h * 0.43f)
                    )

                    // Hair crown / cap / afro
                    when (hairStyle) {
                        0 -> {
                            // Short fade
                            drawArc(
                                color = hair,
                                startAngle = 185f,
                                sweepAngle = 170f,
                                useCenter = true,
                                topLeft = Offset(w * 0.28f, h * 0.20f),
                                size = Size(w * 0.44f, h * 0.28f)
                            )
                        }
                        1 -> {
                            // Curly / afro volume
                            drawCircle(color = hair, radius = w * 0.19f, center = Offset(w * 0.37f, h * 0.27f))
                            drawCircle(color = hair, radius = w * 0.19f, center = Offset(w * 0.63f, h * 0.27f))
                            drawCircle(color = hair, radius = w * 0.20f, center = Offset(w * 0.50f, h * 0.23f))
                        }
                        2 -> {
                            // Parted top hair
                            drawArc(
                                color = hair,
                                startAngle = 175f,
                                sweepAngle = 190f,
                                useCenter = true,
                                topLeft = Offset(w * 0.26f, h * 0.20f),
                                size = Size(w * 0.48f, h * 0.30f)
                            )
                        }
                        3 -> {
                            // Beanie / cap accent
                            drawArc(
                                color = shirt.copy(alpha = 0.9f),
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = true,
                                topLeft = Offset(w * 0.27f, h * 0.19f),
                                size = Size(w * 0.46f, h * 0.30f)
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.85f),
                                topLeft = Offset(w * 0.28f, h * 0.30f),
                                size = Size(w * 0.44f, h * 0.055f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                        else -> {
                            // High puff / bun
                            drawCircle(color = hair, radius = w * 0.14f, center = Offset(w * 0.50f, h * 0.17f))
                            drawArc(
                                color = hair,
                                startAngle = 185f,
                                sweepAngle = 170f,
                                useCenter = true,
                                topLeft = Offset(w * 0.29f, h * 0.21f),
                                size = Size(w * 0.42f, h * 0.26f)
                            )
                        }
                    }

                    // Eyes & optional sunglasses
                    if (hasGlasses) {
                        drawRoundRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(w * 0.34f, h * 0.40f),
                            size = Size(w * 0.13f, h * 0.08f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(w * 0.53f, h * 0.40f),
                            size = Size(w * 0.13f, h * 0.08f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawLine(
                            color = Color(0xFF0F172A),
                            start = Offset(w * 0.47f, h * 0.43f),
                            end = Offset(w * 0.53f, h * 0.43f),
                            strokeWidth = 1.5f
                        )
                    } else {
                        drawCircle(color = Color(0xFF1F2937), radius = w * 0.025f, center = Offset(w * 0.42f, h * 0.44f))
                        drawCircle(color = Color(0xFF1F2937), radius = w * 0.025f, center = Offset(w * 0.58f, h * 0.44f))
                    }
                }
            }
        }
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
