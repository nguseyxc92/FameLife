package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.domain.model.GiftEvent
import com.example.domain.model.GiftType
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.FameMint
import com.example.ui.theme.GamingLiveViolet
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.PhotoLiveAmber
import com.example.ui.theme.PhotoLiveRose
import com.example.ui.theme.ShortLiveCoral
import com.example.ui.theme.ShortLiveNeon
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VideoLiveGold
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Original vector/Canvas artwork for all 8 FameLife virtual gifts.
 */
@Composable
fun GiftArtworkIcon(
    gift: GiftType,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) / 2f

        when (gift) {
            GiftType.FLOWER -> {
                for (i in 0 until 6) {
                    val angle = i * (PI / 3.0)
                    val px = cx + (r * 0.36f * cos(angle)).toFloat()
                    val py = cy + (r * 0.36f * sin(angle)).toFloat()
                    drawCircle(
                        brush = Brush.radialGradient(listOf(PhotoLiveRose, ShortLiveCoral)),
                        radius = r * 0.32f,
                        center = Offset(px, py)
                    )
                }
                drawCircle(color = VideoLiveGold, radius = r * 0.24f, center = Offset(cx, cy))
            }

            GiftType.HEART -> {
                val path = Path().apply {
                    moveTo(cx, cy + r * 0.65f)
                    cubicTo(cx - r * 1.0f, cy + r * 0.05f, cx - r * 0.75f, cy - r * 0.75f, cx, cy - r * 0.25f)
                    cubicTo(cx + r * 0.75f, cy - r * 0.75f, cx + r * 1.0f, cy + r * 0.05f, cx, cy + r * 0.65f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(listOf(ShortLiveCoral, PhotoLiveRose))
                )
            }

            GiftType.COFFEE -> {
                // Cup body
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFD7CCC8), Color(0xFF8D6E63))),
                    topLeft = Offset(cx - r * 0.48f, cy - r * 0.15f),
                    size = Size(r * 0.96f, r * 0.75f),
                    cornerRadius = CornerRadius(r * 0.2f, r * 0.2f)
                )
                // Steam curves
                drawLine(
                    color = VideoLiveGold.copy(alpha = 0.85f),
                    start = Offset(cx - r * 0.18f, cy - r * 0.25f),
                    end = Offset(cx - r * 0.08f, cy - r * 0.6f),
                    strokeWidth = r * 0.1f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = VideoLiveGold.copy(alpha = 0.85f),
                    start = Offset(cx + r * 0.15f, cy - r * 0.25f),
                    end = Offset(cx + r * 0.22f, cy - r * 0.62f),
                    strokeWidth = r * 0.1f,
                    cap = StrokeCap.Round
                )
            }

            GiftType.FIREWORKS -> {
                val colors = listOf(ShortLiveNeon, VideoLiveGold, PhotoLiveRose, FameMint)
                for (i in 0 until 8) {
                    val angle = i * (PI / 4.0)
                    val sx = cx + (r * 0.22f * cos(angle)).toFloat()
                    val sy = cy + (r * 0.22f * sin(angle)).toFloat()
                    val ex = cx + (r * 0.82f * cos(angle)).toFloat()
                    val ey = cy + (r * 0.82f * sin(angle)).toFloat()
                    drawLine(
                        color = colors[i % colors.size],
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = r * 0.12f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = Color.White, radius = r * 0.09f, center = Offset(ex, ey))
                }
            }

            GiftType.GALAXY -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White, ShortLiveNeon, GamingLiveViolet, Color.Transparent)
                    ),
                    radius = r * 0.85f,
                    center = Offset(cx, cy)
                )
                drawOval(
                    color = FameMint.copy(alpha = 0.8f),
                    topLeft = Offset(cx - r * 0.9f, cy - r * 0.28f),
                    size = Size(r * 1.8f, r * 0.56f),
                    style = Stroke(width = r * 0.1f)
                )
            }

            GiftType.CROWN -> {
                val crown = Path().apply {
                    moveTo(cx - r * 0.72f, cy + r * 0.48f)
                    lineTo(cx - r * 0.78f, cy - r * 0.3f)
                    lineTo(cx - r * 0.32f, cy + r * 0.05f)
                    lineTo(cx, cy - r * 0.55f)
                    lineTo(cx + r * 0.32f, cy + r * 0.05f)
                    lineTo(cx + r * 0.78f, cy - r * 0.3f)
                    lineTo(cx + r * 0.72f, cy + r * 0.48f)
                    close()
                }
                drawPath(
                    path = crown,
                    brush = Brush.verticalGradient(listOf(VideoLiveGold, PhotoLiveAmber))
                )
                drawCircle(color = PhotoLiveRose, radius = r * 0.11f, center = Offset(cx, cy + r * 0.15f))
            }

            GiftType.LION_MASCOT -> {
                // Stylized Addis Lion golden mane & emblem
                for (i in 0 until 12) {
                    val angle = i * (PI / 6.0)
                    val mx = cx + (r * 0.55f * cos(angle)).toFloat()
                    val my = cy + (r * 0.55f * sin(angle)).toFloat()
                    drawCircle(color = PhotoLiveAmber, radius = r * 0.28f, center = Offset(mx, my))
                }
                drawCircle(
                    brush = Brush.radialGradient(listOf(VideoLiveGold, Color(0xFFD97706))),
                    radius = r * 0.52f,
                    center = Offset(cx, cy)
                )
                drawCircle(color = StudioObsidian, radius = r * 0.09f, center = Offset(cx - r * 0.18f, cy - r * 0.08f))
                drawCircle(color = StudioObsidian, radius = r * 0.09f, center = Offset(cx + r * 0.18f, cy - r * 0.08f))
                drawCircle(color = StudioObsidian, radius = r * 0.12f, center = Offset(cx, cy + r * 0.12f))
            }

            GiftType.COSMIC_CELEBRATION -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White, FameMint, InfoCyan, GamingLiveViolet.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    radius = r * 0.95f,
                    center = Offset(cx, cy)
                )
                for (i in 0 until 6) {
                    val a = i * (PI / 3.0)
                    drawLine(
                        color = VideoLiveGold,
                        start = Offset(cx, cy),
                        end = Offset(cx + (r * 0.88f * cos(a)).toFloat(), cy + (r * 0.88f * sin(a)).toFloat()),
                        strokeWidth = r * 0.11f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun SmallGiftBanner(
    giftEvent: GiftEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(giftEvent?.eventId, giftEvent?.count) {
        if (giftEvent != null) {
            delay(3_200L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = giftEvent != null,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        giftEvent?.let { evt ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = StudioObsidian.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(FameMint, ShortLiveNeon))
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViewerAvatar(
                        initials = evt.initials,
                        seed = evt.avatarSeed,
                        hasProfilePicture = evt.hasProfilePicture,
                        imageUri = evt.avatarUri,
                        size = 28.dp
                    )
                    Column {
                        Text(
                            text = evt.username,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "sent ${evt.gift.displayName} (${evt.totalVirtualCoins} virtual)",
                            style = MaterialTheme.typography.labelSmall,
                            color = FameMint
                        )
                    }
                    GiftArtworkIcon(gift = evt.gift, size = 30.dp)
                    if (evt.count > 1) {
                        Text(
                            text = "x${evt.count}",
                            style = MaterialTheme.typography.titleMedium,
                            color = VideoLiveGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LargeGiftCelebrationOverlay(
    giftEvent: GiftEvent?,
    reducedMotion: Boolean,
    onDismiss: () -> Unit
) {
    LaunchedEffect(giftEvent?.eventId) {
        if (giftEvent != null) {
            delay(if (reducedMotion) 2_400L else 3_800L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = giftEvent != null,
        enter = fadeIn(tween(240)),
        exit = fadeOut(tween(240))
    ) {
        giftEvent?.let { evt ->
            val pulse = if (reducedMotion) {
                1f
            } else {
                val infinite = rememberInfiniteTransition(label = "giftPulse")
                val v by infinite.animateFloat(
                    initialValue = 0.92f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(650, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "giftScale"
                )
                v
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StudioObsidian.copy(alpha = 0.72f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(pulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        VideoLiveGold.copy(alpha = 0.35f),
                                        GamingLiveViolet.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        GiftArtworkIcon(gift = evt.gift, size = 112.dp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = StudioObsidian.copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VideoLiveGold.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${evt.gift.tier.label.uppercase()} VIRTUAL GIFT",
                                style = MaterialTheme.typography.labelSmall,
                                color = VideoLiveGold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${evt.username} sent ${evt.gift.displayName}",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "${evt.totalVirtualCoins} virtual coins (simulated • non-redeemable)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = FameMint)
                    ) {
                        Text("Tap anywhere to skip")
                    }
                }
            }
        }
    }
}

private data class HeartParticle(val id: Long, val xBias: Float, val color: Color)

@Composable
fun FloatingHeartsColumn(
    pulseTimestamp: Long,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableStateListOf<HeartParticle>() }
    val heartColors = remember {
        listOf(PhotoLiveRose, ShortLiveCoral, PhotoLiveAmber, FameMint, ShortLiveNeon)
    }

    LaunchedEffect(pulseTimestamp) {
        if (pulseTimestamp > 0L && !reducedMotion) {
            if (particles.size < 14) {
                particles.add(
                    HeartParticle(
                        id = pulseTimestamp + particles.size,
                        xBias = ((pulseTimestamp % 7) - 3) * 4f,
                        color = heartColors[(pulseTimestamp % heartColors.size).toInt()]
                    )
                )
            }
        }
    }

    Box(modifier = modifier.width(54.dp).height(190.dp)) {
        particles.forEach { particle ->
            val progress = remember(particle.id) { Animatable(0f) }
            LaunchedEffect(particle.id) {
                progress.animateTo(1f, tween(1600, easing = LinearEasing))
                particles.remove(particle)
            }
            val yOffset = ((1f - progress.value) * 160f).roundToInt()
            val alpha = (1f - progress.value).coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(particle.xBias.roundToInt(), - (160 - yOffset)) }
                    .size(22.dp)
                    .alpha(alpha)
            ) {
                val w = size.width
                val h = size.height
                val p = Path().apply {
                    moveTo(w / 2f, h * 0.85f)
                    cubicTo(0f, h * 0.48f, w * 0.1f, 0f, w / 2f, h * 0.32f)
                    cubicTo(w * 0.9f, 0f, w, h * 0.48f, w / 2f, h * 0.85f)
                    close()
                }
                drawPath(p, color = particle.color)
            }
        }
    }
}
