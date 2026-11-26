package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.CreatorProfileEntity
import com.example.data.UserSettings
import com.example.domain.model.ChatEventType
import com.example.domain.model.LivePlatformMode
import com.example.domain.model.StreamSourceMode
import com.example.media.LiveMediaStage
import com.example.media.ScreenCaptureBridge
import com.example.media.ScreenCaptureService
import com.example.ui.components.LargeGiftCelebrationOverlay
import com.example.ui.components.ViewerAvatar
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.FameMint
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VideoLiveGold
import com.example.viewmodel.AiStatus
import com.example.viewmodel.LiveSessionUiState
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRoomScreen(
    state: LiveSessionUiState,
    settings: UserSettings,
    profile: CreatorProfileEntity?,
    onSubmitUtterance: (String) -> Unit,
    onVisualFrameSampled: (android.graphics.Bitmap) -> Unit,
    onDismissSmallGift: () -> Unit,
    onDismissLargeGift: () -> Unit,
    onPinMessage: (com.example.domain.model.ChatEvent) -> Unit,
    onRemoveMessage: (String) -> Unit,
    onTriggerHeart: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleMic: (Boolean) -> Unit,
    onToggleCameraOff: () -> Unit,
    onSwitchSourceMode: (StreamSourceMode) -> Unit,
    onToggleChatVisible: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleVisualAi: (Boolean) -> Unit,
    onEndStreamConfirmed: () -> Unit
) {
    val context = LocalContext.current
    val screenShareActive by ScreenCaptureBridge.isCapturing.collectAsState()

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

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPerm = granted
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPerm = granted
        if (granted) onToggleMic(true)
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val svcIntent = ScreenCaptureService.createStartIntent(context, result.resultCode, result.data!!)
            ContextCompat.startForegroundService(context, svcIntent)
            onSwitchSourceMode(StreamSourceMode.SCREEN_SHARE)
        }
    }

    var draftMessage by remember { mutableStateOf("") }
    var showControlsSheet by remember { mutableStateOf(false) }
    var showSupportersSheet by remember { mutableStateOf(false) }
    var showQuestionsSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showEndDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = state.isLive) {
        showEndDialog = true
    }

    val sendUtteranceAction = {
        if (draftMessage.isNotBlank()) {
            onSubmitUtterance(draftMessage)
            draftMessage = ""
        }
    }

    val stageContent: @Composable (Modifier) -> Unit = { mod ->
        LiveMediaStage(
            sourceMode = state.sourceMode,
            hasCameraPermission = hasCameraPerm,
            isCameraPaused = state.isCameraOff,
            visualUnderstandingEnabled = state.visualUnderstandingEnabled,
            screenShareActive = screenShareActive,
            onRequestCameraPermission = { cameraLauncher.launch(Manifest.permission.CAMERA) },
            onSceneFrameSampled = onVisualFrameSampled,
            modifier = mod
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(StudioObsidian)) {
        when (state.platformMode) {
            LivePlatformMode.PHOTO_LIVE -> {
                stageContent(Modifier.fillMaxSize())
                PhotoLiveOverlay(
                    state = state,
                    creatorHandle = profile?.handle ?: "creator_live",
                    reducedMotion = settings.reducedMotion,
                    draftMessage = draftMessage,
                    onDraftChange = { draftMessage = it },
                    onSendUtterance = sendUtteranceAction,
                    onDismissSmallGift = onDismissSmallGift,
                    onPinMessage = onPinMessage,
                    onTriggerHeart = onTriggerHeart,
                    onOpenQuestions = { showQuestionsSheet = true },
                    onOpenControls = { showControlsSheet = true },
                    onRequestEnd = { showEndDialog = true }
                )
            }

            LivePlatformMode.SHORT_LIVE -> {
                stageContent(Modifier.fillMaxSize())
                ShortLiveOverlay(
                    state = state,
                    creatorName = profile?.displayName ?: "Creator",
                    draftMessage = draftMessage,
                    onDraftChange = { draftMessage = it },
                    onSendUtterance = sendUtteranceAction,
                    onDismissSmallGift = onDismissSmallGift,
                    onFlipCamera = onFlipCamera,
                    onToggleMic = {
                        if (!hasMicPerm) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        else onToggleMic(true)
                    },
                    onOpenSupporters = { showSupportersSheet = true },
                    onOpenControls = { showControlsSheet = true },
                    onRequestEnd = { showEndDialog = true }
                )
            }

            LivePlatformMode.VIDEO_LIVE -> {
                VideoLiveLayout(
                    state = state,
                    draftMessage = draftMessage,
                    onDraftChange = { draftMessage = it },
                    onSendUtterance = sendUtteranceAction,
                    onPinMessage = onPinMessage,
                    onRemoveMessage = onRemoveMessage,
                    onOpenSearch = { showSearchSheet = true },
                    onOpenControls = { showControlsSheet = true },
                    onRequestEnd = { showEndDialog = true },
                    videoStageContent = stageContent
                )
            }

            LivePlatformMode.GAMING_LIVE -> {
                GamingLiveLayout(
                    state = state,
                    draftMessage = draftMessage,
                    onDraftChange = { draftMessage = it },
                    onSendUtterance = sendUtteranceAction,
                    onRemoveMessage = onRemoveMessage,
                    onOpenControls = { showControlsSheet = true },
                    onRequestEnd = { showEndDialog = true },
                    videoStageContent = stageContent
                )
            }
        }

        // Honest degraded mode or reminder toast strip
        if (state.aiStatus is AiStatus.Degraded) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = StudioObsidian.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, AlertAmber)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${state.aiStatus.reason} (Ambient chat only)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlertAmber
                    )
                }
            }
        }

        // Large skippable full-screen gift celebration overlay
        LargeGiftCelebrationOverlay(
            giftEvent = state.activeLargeGift,
            reducedMotion = settings.reducedMotion,
            onDismiss = onDismissLargeGift
        )
    }

    // Studio Controls & Quick Creator Prompts Bottom Sheet
    if (showControlsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showControlsSheet = false },
            containerColor = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Live Studio Controls", style = MaterialTheme.typography.titleLarge)

                // Quick creator scenarios for instant testing / mic-free interaction
                Text("Quick Creator Phrases", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                val quickPrompts = listOf(
                    "Put 1 in the chat",
                    "Should I open this door?",
                    "Should I ask her out?",
                    "Thank you for supporting me"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickPrompts) { prompt ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = StudioSurfaceVariant,
                            border = BorderStroke(1.dp, FameMint.copy(alpha = 0.45f)),
                            modifier = Modifier.clickable {
                                onSubmitUtterance(prompt)
                                showControlsSheet = false
                            }
                        ) {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelLarge,
                                color = FameMint,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Grid of live controls
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioControlButton(
                        icon = Icons.Default.Cameraswitch,
                        label = "Flip Cam",
                        active = false,
                        onClick = onFlipCamera,
                        modifier = Modifier.weight(1f)
                    )
                    StudioControlButton(
                        icon = if (state.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        label = if (state.isCameraOff) "Cam Off" else "Cam On",
                        active = !state.isCameraOff,
                        onClick = onToggleCameraOff,
                        modifier = Modifier.weight(1f)
                    )
                    StudioControlButton(
                        icon = if (state.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (state.isMicMuted) "Muted" else "Mic On",
                        active = !state.isMicMuted,
                        onClick = {
                            if (!hasMicPerm) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            else onToggleMic(true)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioControlButton(
                        icon = Icons.Default.ScreenShare,
                        label = if (state.sourceMode == StreamSourceMode.SCREEN_SHARE) "Stop Share" else "Screen Share",
                        active = state.sourceMode == StreamSourceMode.SCREEN_SHARE,
                        onClick = {
                            if (state.sourceMode == StreamSourceMode.SCREEN_SHARE) {
                                onSwitchSourceMode(StreamSourceMode.FRONT_CAMERA)
                            } else {
                                val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                mediaProjectionLauncher.launch(mgr.createScreenCaptureIntent())
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StudioControlButton(
                        icon = Icons.Default.GraphicEq,
                        label = "Audio Only",
                        active = state.sourceMode == StreamSourceMode.AUDIO_ONLY,
                        onClick = {
                            val next = if (state.sourceMode == StreamSourceMode.AUDIO_ONLY) {
                                StreamSourceMode.FRONT_CAMERA
                            } else StreamSourceMode.AUDIO_ONLY
                            onSwitchSourceMode(next)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StudioControlButton(
                        icon = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        label = if (state.isPaused) "Resume" else "Pause",
                        active = state.isPaused,
                        onClick = onTogglePause,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioControlButton(
                        icon = if (state.isChatVisible) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                        label = if (state.isChatVisible) "Hide Chat" else "Show Chat",
                        active = state.isChatVisible,
                        onClick = onToggleChatVisible,
                        modifier = Modifier.weight(1f)
                    )
                    StudioControlButton(
                        icon = Icons.Default.Visibility,
                        label = if (state.visualUnderstandingEnabled) "Vision On" else "Vision Off",
                        active = state.visualUnderstandingEnabled,
                        onClick = { onToggleVisualAi(!state.visualUnderstandingEnabled) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        showControlsSheet = false
                        showEndDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("End Simulated Live", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    if (showSupportersSheet) {
        ModalBottomSheet(onDismissRequest = { showSupportersSheet = false }, containerColor = StudioSurface) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Session Virtual Supporters", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Total reconciled virtual coins: ${state.totalVirtualCoins} (non-redeemable)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FameMint
                )
                if (state.topSupporters.isEmpty()) {
                    Text("No virtual gifts sent yet in this session.", color = TextSecondary)
                } else {
                    state.topSupporters.forEachIndexed { idx, sup ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("#${idx + 1}", style = MaterialTheme.typography.titleMedium, color = VideoLiveGold)
                                ViewerAvatar(initials = sup.initials, seed = sup.avatarSeed, size = 32.dp)
                                Text(sup.username, style = MaterialTheme.typography.labelLarge)
                            }
                            Text("${sup.totalCoins} coins", style = MaterialTheme.typography.labelMedium, color = FameMint)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showQuestionsSheet) {
        val questions = state.searchableChatHistory.filter { it.type == ChatEventType.QUESTION || it.text.contains("?") }
        ModalBottomSheet(onDismissRequest = { showQuestionsSheet = false }, containerColor = StudioSurface) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Audience Questions (${questions.size})", style = MaterialTheme.typography.titleLarge)
                if (questions.isEmpty()) {
                    Text("No viewer questions yet — ask the chat what they want to know!", color = TextSecondary)
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(questions.reversed(), key = { it.eventId }) { q ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StudioSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onPinMessage(q)
                                    showQuestionsSheet = false
                                }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(q.username, style = MaterialTheme.typography.labelLarge, color = FameMint)
                                    Text(q.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showSearchSheet) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(query, state.searchableChatHistory) {
            if (query.isBlank()) state.searchableChatHistory.takeLast(50).reversed()
            else state.searchableChatHistory.filter {
                it.text.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true)
            }.takeLast(80).reversed()
        }
        ModalBottomSheet(onDismissRequest = { showSearchSheet = false }, containerColor = StudioSurface) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Search Session Chat (${state.searchableChatHistory.size} stored)", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search username or keyword...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered, key = { it.eventId }) { msg ->
                        Text("${msg.username}: ${msg.text}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            containerColor = StudioSurface,
            title = { Text("End Simulated Livestream?") },
            text = {
                Text(
                    "Camera, microphone, and screen capture will stop immediately. Your simulated stats (${formatCompactCount(state.peakViewers)} peak viewers, ${state.totalVirtualCoins} virtual coins) will be summarized.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndDialog = false
                        onEndStreamConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_end_stream_btn")
                ) {
                    Text("End Stream")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("Stay Live", color = FameMint)
                }
            }
        )
    }
}

@Composable
private fun StudioControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (active) FameMint else StudioOutline),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = if (active) FameMint else TextPrimary, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) FameMint else TextSecondary, maxLines = 1)
        }
    }
}
