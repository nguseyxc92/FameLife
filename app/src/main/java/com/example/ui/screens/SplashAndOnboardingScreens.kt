package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FameMint
import com.example.ui.theme.FameMintContainer
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.84f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, tween(420))
        delay(550L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(FameMintContainer.copy(alpha = 0.5f), StudioObsidian)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
                .padding(24.dp)
        ) {
            FameLifeEmblem(modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                text = "FameLife",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Experience the spotlight.",
                style = MaterialTheme.typography.titleMedium,
                color = FameMint
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = "TEBIB LABS • PRIVATE AI SIMULATION",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
fun FameLifeEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val r = minOf(size.width, size.height) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawCircle(
            brush = Brush.radialGradient(listOf(FameMint.copy(alpha = 0.25f), Color.Transparent)),
            radius = r,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )
        drawCircle(
            color = FameMint,
            radius = r * 0.72f,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = Stroke(width = r * 0.14f)
        )
        val play = Path().apply {
            moveTo(cx - r * 0.22f, cy - r * 0.30f)
            lineTo(cx + r * 0.34f, cy)
            lineTo(cx - r * 0.22f, cy + r * 0.30f)
            close()
        }
        drawPath(play, color = Color.White)
    }
}

@Composable
fun OnboardingScreen(
    onComplete: (displayName: String, handle: String, bio: String, isGuest: Boolean) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FameLifeEmblem(modifier = Modifier.size(44.dp))
                Column {
                    Text("Welcome to FameLife", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Private AI Livestream Simulator • Tebib Labs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FameMint
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = StudioSurface,
                border = BorderStroke(1.dp, FameMint.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Simulation disclosure",
                            tint = FameMint
                        )
                        Text(
                            text = "Simulation & Privacy Disclosure",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    DisclosureBullet("No real social platform broadcast, public streaming, or real viewers.")
                    DisclosureBullet("All gifts, Super Chats, Bits, coins, and earnings are 100% virtual and non-redeemable.")
                    DisclosureBullet("Camera, microphone, and screen share are used strictly on-demand while you are live; nothing is retained unless you save local stats.")
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = StudioSurfaceVariant,
                border = BorderStroke(1.dp, StudioOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Optional Local Creator Profile",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Stored only on your device. You can delete it anytime or continue immediately as a Guest.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Creator Name (optional)") },
                        placeholder = { Text("e.g., Selam Studio") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input")
                    )
                    OutlinedTextField(
                        value = handle,
                        onValueChange = { handle = it },
                        label = { Text("Handle (optional)") },
                        placeholder = { Text("e.g., selam.live") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_handle_input")
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Stream Vibe / Bio (optional)") },
                        placeholder = { Text("Late night chats, gaming & music") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    val name = displayName.ifBlank { "Creator" }
                    val h = handle.ifBlank { "famelife_creator" }
                    onComplete(name, h, bio, false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_enter_studio_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = FameMint, contentColor = StudioObsidian)
            ) {
                Text("Save Profile & Enter Studio", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { onComplete("Guest Creator", "guest_live", "", true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("onboarding_guest_btn"),
                border = BorderStroke(1.dp, StudioOutline)
            ) {
                Text("Continue as Guest", color = TextPrimary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Offline-friendly • Deletable local history",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DisclosureBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = FameMint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
