package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.UserSettings
import com.example.domain.model.StreamSourceMode
import com.example.media.LiveMediaStage
import com.example.ui.components.SimulationWatermarkPill
import com.example.ui.theme.FameMint
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PreLivePreviewScreen(
    settings: UserSettings,
    sourceMode: StreamSourceMode,
    onSelectSource: (StreamSourceMode) -> Unit,
    onToggleVisualUnderstanding: (Boolean) -> Unit,
    onBack: () -> Unit,
    onGoLive: (hasMicPermission: Boolean) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasMicPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPerm = granted
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPerm = granted
    }

    val multiPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPerm = results[Manifest.permission.CAMERA] ?: hasCameraPerm
        hasMicPerm = results[Manifest.permission.RECORD_AUDIO] ?: hasMicPerm
    }

    Box(modifier = Modifier.fillMaxSize().background(StudioObsidian)) {
        LiveMediaStage(
            sourceMode = sourceMode,
            hasCameraPermission = hasCameraPerm,
            isCameraPaused = false,
            visualUnderstandingEnabled = false,
            screenShareActive = false,
            onRequestCameraPermission = { cameraLauncher.launch(Manifest.permission.CAMERA) },
            onSceneFrameSampled = {},
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            StudioObsidian.copy(alpha = 0.72f),
                            Color.Transparent,
                            StudioObsidian.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(StudioObsidian.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Studio",
                            tint = TextPrimary
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${settings.selectedPlatform.displayName} • Pre-Live",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${settings.selectedFamePreset.title} • ${settings.language.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = FameMint
                        )
                    }
                }
                SimulationWatermarkPill(compact = true)
            }

            // Bottom setup sheet
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = StudioSurface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, StudioOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Broadcast Source", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StreamSourceMode.entries.forEach { mode ->
                            val icon = when (mode) {
                                StreamSourceMode.FRONT_CAMERA,
                                StreamSourceMode.REAR_CAMERA -> Icons.Default.CameraAlt
                                StreamSourceMode.SCREEN_SHARE -> Icons.Default.ScreenShare
                                StreamSourceMode.AUDIO_ONLY -> Icons.Default.GraphicEq
                            }
                            FilterChip(
                                selected = sourceMode == mode,
                                onClick = { onSelectSource(mode) },
                                leadingIcon = {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = {
                                    Text(
                                        text = when (mode) {
                                            StreamSourceMode.FRONT_CAMERA -> "Front"
                                            StreamSourceMode.REAR_CAMERA -> "Rear"
                                            StreamSourceMode.SCREEN_SHARE -> "Screen"
                                            StreamSourceMode.AUDIO_ONLY -> "Audio"
                                        },
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FameMint.copy(alpha = 0.2f),
                                    selectedLabelColor = FameMint
                                )
                            )
                        }
                    }

                    // Permissions & Visual AI row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = FameMint
                            )
                            Column {
                                Text("Visual Scene Understanding", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "Sparse frames sampled only while live",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = settings.visualUnderstandingEnabled,
                            onCheckedChange = onToggleVisualUnderstanding,
                            colors = SwitchDefaults.colors(checkedThumbColor = StudioObsidian, checkedTrackColor = FameMint)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (!hasMicPerm) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = StudioObsidian,
                            border = BorderStroke(1.dp, if (hasMicPerm) FameMint else StudioOutline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasMicPerm) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = "Microphone status",
                                    tint = if (hasMicPerm) FameMint else TextSecondary
                                )
                                Text(
                                    text = if (hasMicPerm) "Mic Ready" else "Enable Mic",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        if (!hasCameraPerm || !hasMicPerm) {
                            Surface(
                                onClick = {
                                    multiPermLauncher.launch(
                                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = StudioObsidian,
                                border = BorderStroke(1.dp, FameMint.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Grant Cam + Mic", style = MaterialTheme.typography.labelLarge, color = FameMint)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { onGoLive(hasMicPerm) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("prelive_go_live_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LiveCrimson,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.RadioButtonChecked, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Go Live (Simulated)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
