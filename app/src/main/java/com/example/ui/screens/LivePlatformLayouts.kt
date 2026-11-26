package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ChatEvent
import com.example.domain.model.ChatEventType
import com.example.domain.model.ViewerArchetype
import com.example.ui.components.FloatingHeartsColumn
import com.example.ui.components.SimulationWatermarkPill
import com.example.ui.components.SmallGiftBanner
import com.example.ui.components.ViewerAvatar
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatDuration
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.FameMint
import com.example.ui.theme.GamingLiveEmerald
import com.example.ui.theme.GamingLiveViolet
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.PhotoLiveRose
import com.example.ui.theme.ShortLiveCoral
import com.example.ui.theme.ShortLiveNeon
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VideoLiveGold
import com.example.ui.theme.VideoLiveRed
import com.example.viewmodel.AiStatus
import com.example.viewmodel.LiveSessionUiState

/**
 * 1. PHOTO LIVE (Instagram-inspired):
 * Full-bleed video, compact creator header + LIVE + eye viewer count + X,
 * bottom-left translucent comments/badges/joins, bottom-right floating hearts column,
 * pinned comment strip, Q&A pill, and clean creator input bar.
 */
@Composable
fun PhotoLiveOverlay(
    state: LiveSessionUiState,
    creatorHandle: String,
    reducedMotion: Boolean,
    draftMessage: String,
    onDraftChange: (String) -> Unit,
    onSendUtterance: () -> Unit,
    onDismissSmallGift: () -> Unit,
    onPinMessage: (ChatEvent) -> Unit,
    onTriggerHeart: () -> Unit,
    onOpenQuestions: () -> Unit,
    onOpenControls: () -> Unit,
    onRequestEnd: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.visibleChat.size) {
        if (state.visibleChat.isNotEmpty()) {
            listState.animateScrollToItem(state.visibleChat.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Bottom readability scrim over full-bleed camera
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, StudioObsidian.copy(alpha = 0.88f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: creator pill on left, LIVE + eye + X on right
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ViewerAvatar(initials = creatorHandle.take(2).uppercase(), seed = 9, size = 34.dp)
                        Column {
                            Text(
                                text = creatorHandle,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = formatDuration(state.elapsedSec),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Brush.horizontalGradient(listOf(PhotoLiveRose, ShortLiveCoral)).let { PhotoLiveRose }
                        ) {
                            Text(
                                text = if (state.isPaused) "PAUSED" else "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StudioObsidian.copy(alpha = 0.68f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = "Viewers", tint = Color.White, modifier = Modifier.size(13.dp))
                                Text(
                                    text = formatCompactCount(state.currentViewers),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.testTag("live_viewer_count")
                                )
                            }
                        }
                        IconButton(
                            onClick = onRequestEnd,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("live_end_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "End live", tint = Color.White)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimulationWatermarkPill(compact = true)
                    AiLatencyChip(state.aiStatus)
                }

                CreatorUtteranceBanner(state)
            }

            // Bottom chat + floating hearts + input bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallGiftBanner(
                    giftEvent = state.activeSmallGift,
                    onDismiss = onDismissSmallGift
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (state.isChatVisible) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .height(210.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.visibleChat, key = { it.eventId }) { msg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPinMessage(msg) },
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ViewerAvatar(initials = msg.initials, seed = msg.avatarSeed, size = 26.dp)
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(
                                                text = msg.username,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.White
                                            )
                                            if (msg.type == ChatEventType.GIFT_REACTION) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = PhotoLiveRose.copy(alpha = 0.85f)) {
                                                    Text(
                                                        text = "BADGE",
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.92f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }

                    FloatingHeartsColumn(
                        pulseTimestamp = state.floatingHeartPulse,
                        reducedMotion = reducedMotion
                    )
                }

                state.pinnedMessage?.let { pinned ->
                    PinnedBanner(pinned = pinned, onUnpin = { onPinMessage(pinned) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LivePromptInputPill(
                        value = draftMessage,
                        onValueChange = onDraftChange,
                        onSend = onSendUtterance,
                        placeholder = "Say or type to audience...",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onOpenQuestions,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(StudioObsidian.copy(alpha = 0.72f))
                    ) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = "Audience Questions", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onOpenControls,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(StudioObsidian.copy(alpha = 0.72f))
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Studio Controls", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onTriggerHeart,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PhotoLiveRose.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Send heart", tint = PhotoLiveRose, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

/**
 * 2. SHORT LIVE (TikTok LIVE-inspired):
 * Vertical full-screen video, creator card + likes count, top supporter ranks,
 * rapid badge-tagged comments, animated small gift banner, and host control tray.
 */
@Composable
fun ShortLiveOverlay(
    state: LiveSessionUiState,
    creatorName: String,
    draftMessage: String,
    onDraftChange: (String) -> Unit,
    onSendUtterance: () -> Unit,
    onDismissSmallGift: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onOpenSupporters: () -> Unit,
    onOpenControls: () -> Unit,
    onRequestEnd: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.visibleChat.size) {
        if (state.visibleChat.isNotEmpty()) {
            listState.animateScrollToItem(state.visibleChat.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, StudioObsidian.copy(alpha = 0.9f)))
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Creator pill with likes
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = StudioObsidian.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ViewerAvatar(initials = creatorName.take(2).uppercase(), seed = 5, size = 32.dp, hasHighlightRing = true)
                            Column {
                                Text(creatorName, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1)
                                Text(
                                    "${formatCompactCount(state.totalLikes)} likes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShortLiveNeon
                                )
                            }
                        }
                    }

                    // Top supporters + viewer count + X
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.topSupporters.take(2).forEach { sup ->
                            ViewerAvatar(
                                initials = sup.initials,
                                seed = sup.avatarSeed,
                                size = 26.dp,
                                modifier = Modifier.clickable { onOpenSupporters() }
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = StudioObsidian.copy(alpha = 0.72f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(ShortLiveCoral))
                                Text(
                                    text = formatCompactCount(state.currentViewers),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.testTag("live_viewer_count")
                                )
                            }
                        }
                        IconButton(
                            onClick = onRequestEnd,
                            modifier = Modifier.size(34.dp).testTag("live_end_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "End live", tint = Color.White)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimulationWatermarkPill(compact = true)
                    AiLatencyChip(state.aiStatus)
                }

                CreatorUtteranceBanner(state)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallGiftBanner(
                    giftEvent = state.activeSmallGift,
                    onDismiss = onDismissSmallGift
                )

                if (state.isChatVisible) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .height(225.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.visibleChat, key = { it.eventId }) { msg ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StudioObsidian.copy(alpha = 0.56f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    msg.archetype.badgeLabel?.let { b ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (msg.archetype == ViewerArchetype.SUPPORTER) VideoLiveGold else ShortLiveCoral
                                        ) {
                                            Text(
                                                text = b,
                                                fontSize = 9.sp,
                                                color = StudioObsidian,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(SpanStyle(color = ShortLiveNeon, fontWeight = FontWeight.SemiBold)) {
                                                append("${msg.username} ")
                                            }
                                            withStyle(SpanStyle(color = Color.White)) {
                                                append(msg.text)
                                            }
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom host action bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LivePromptInputPill(
                        value = draftMessage,
                        onValueChange = onDraftChange,
                        onSend = onSendUtterance,
                        placeholder = "Add comment / speak...",
                        modifier = Modifier.weight(1f)
                    )
                    HostCircleAction(
                        icon = if (state.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = "Mic",
                        tint = if (state.isMicMuted) LiveCrimson else Color.White,
                        onClick = onToggleMic
                    )
                    HostCircleAction(
                        icon = Icons.Default.Cameraswitch,
                        label = "Flip",
                        tint = Color.White,
                        onClick = onFlipCamera
                    )
                    HostCircleAction(
                        icon = Icons.Default.CardGiftcard,
                        label = "Ranks",
                        tint = VideoLiveGold,
                        onClick = onOpenSupporters
                    )
                    HostCircleAction(
                        icon = Icons.Default.Tune,
                        label = "Studio",
                        tint = FameMint,
                        onClick = onOpenControls
                    )
                }
            }
        }
    }
}

/**
 * 3. VIDEO LIVE (YouTube Live-inspired):
 * Adaptive portrait (16:9 top stage + dedicated live chat panel below) and landscape split theater.
 * Features Super Chat ticker pills, highlighted Super Chat banner, moderator controls, and search.
 */
@Composable
fun VideoLiveLayout(
    state: LiveSessionUiState,
    draftMessage: String,
    onDraftChange: (String) -> Unit,
    onSendUtterance: () -> Unit,
    onPinMessage: (ChatEvent) -> Unit,
    onRemoveMessage: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenControls: () -> Unit,
    onRequestEnd: () -> Unit,
    videoStageContent: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .safeDrawingPadding()
    ) {
        val isLandscape = maxWidth > maxHeight
        val stageHeight = maxHeight * 0.36f
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.60f).fillMaxHeight()) {
                    videoStageContent(Modifier.fillMaxSize())
                    VideoStageOverlayControls(state, onOpenControls, onRequestEnd)
                }
                VideoChatPanel(
                    state = state,
                    draftMessage = draftMessage,
                    onDraftChange = onDraftChange,
                    onSendUtterance = onSendUtterance,
                    onPinMessage = onPinMessage,
                    onRemoveMessage = onRemoveMessage,
                    onOpenSearch = onOpenSearch,
                    modifier = Modifier.weight(0.40f).fillMaxHeight()
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().height(stageHeight)) {
                    videoStageContent(Modifier.fillMaxSize())
                    VideoStageOverlayControls(state, onOpenControls, onRequestEnd)
                }
                VideoChatPanel(
                    state = state,
                    draftMessage = draftMessage,
                    onDraftChange = onDraftChange,
                    onSendUtterance = onSendUtterance,
                    onPinMessage = onPinMessage,
                    onRemoveMessage = onRemoveMessage,
                    onOpenSearch = onOpenSearch,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}

@Composable
private fun VideoStageOverlayControls(
    state: LiveSessionUiState,
    onOpenControls: () -> Unit,
    onRequestEnd: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = VideoLiveRed) {
                    Text(
                        text = if (state.isPaused) "PAUSED" else "LIVE • ${formatDuration(state.elapsedSec)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(6.dp), color = StudioObsidian.copy(alpha = 0.78f)) {
                    Text(
                        text = "${formatCompactCount(state.currentViewers)} watching",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("live_viewer_count")
                    )
                }
                SimulationWatermarkPill(compact = true)
            }
            Row {
                IconButton(onClick = onOpenControls, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = "Controls", tint = Color.White)
                }
                IconButton(onClick = onRequestEnd, modifier = Modifier.size(34.dp).testTag("live_end_btn")) {
                    Icon(Icons.Default.Close, contentDescription = "End live", tint = Color.White)
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            CreatorUtteranceBanner(state)
        }
    }
}

@Composable
private fun VideoChatPanel(
    state: LiveSessionUiState,
    draftMessage: String,
    onDraftChange: (String) -> Unit,
    onSendUtterance: () -> Unit,
    onPinMessage: (ChatEvent) -> Unit,
    onRemoveMessage: (String) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.visibleChat.size) {
        if (state.visibleChat.isNotEmpty()) {
            listState.animateScrollToItem(state.visibleChat.lastIndex)
        }
    }

    Surface(modifier = modifier, color = StudioSurface) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Live chat", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${formatCompactCount(state.uniqueParticipants)} participants • ${state.totalVirtualCoins} virtual coins",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AiLatencyChip(state.aiStatus)
                    IconButton(onClick = onOpenSearch, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Search, contentDescription = "Search chat", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Super Chat horizontal pill ticker
            if (state.superChatTicker.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.superChatTicker, key = { it.eventId }) { sc ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = FameMint
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ViewerAvatar(initials = sc.initials, seed = sc.avatarSeed, size = 20.dp)
                                Text(
                                    text = "${sc.virtualCoins} coins",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioObsidian,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Highlighted latest Super Chat card
            state.latestSuperChat?.let { sc ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F8E65)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${sc.username} • ${sc.virtualCoins} virtual coins",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            Text("SUPER CHAT (SIM)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(sc.text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
            }

            state.pinnedMessage?.let { pinned ->
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    PinnedBanner(pinned = pinned, onUnpin = { onPinMessage(pinned) })
                }
            }

            // Dedicated chat feed with creator moderation actions
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.visibleChat, key = { it.eventId }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ViewerAvatar(initials = msg.initials, seed = msg.avatarSeed, size = 26.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = TextSecondary, fontWeight = FontWeight.SemiBold)) {
                                        append("${msg.username}  ")
                                    }
                                    withStyle(SpanStyle(color = TextPrimary)) {
                                        append(msg.text)
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(onClick = { onPinMessage(msg) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.PushPin, contentDescription = "Pin comment", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                        IconButton(onClick = { onRemoveMessage(msg.eventId) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove comment", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Creator reply bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LivePromptInputPill(
                    value = draftMessage,
                    onValueChange = onDraftChange,
                    onSend = onSendUtterance,
                    placeholder = "Speak or chat as creator...",
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.AttachMoney, contentDescription = "Virtual Super Chat active", tint = VideoLiveGold)
            }
        }
    }
}

/**
 * 4. GAMING LIVE (Twitch-inspired):
 * Landscape-first stage + Bits/Subs supporter leaderboard + dense badge-colored chat +
 * raid alert banner + Stream Summary card.
 */
@Composable
fun GamingLiveLayout(
    state: LiveSessionUiState,
    draftMessage: String,
    onDraftChange: (String) -> Unit,
    onSendUtterance: () -> Unit,
    onRemoveMessage: (String) -> Unit,
    onOpenControls: () -> Unit,
    onRequestEnd: () -> Unit,
    videoStageContent: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E10))
            .safeDrawingPadding()
    ) {
        val isLandscape = maxWidth > maxHeight
        val stageHeight = maxHeight * 0.34f
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
                    videoStageContent(Modifier.fillMaxSize())
                    GamingStageHeader(state, onOpenControls, onRequestEnd)
                }
                GamingChatColumn(
                    state = state,
                    draftMessage = draftMessage,
                    onDraftChange = onDraftChange,
                    onSendUtterance = onSendUtterance,
                    onRemoveMessage = onRemoveMessage,
                    modifier = Modifier.weight(0.42f).fillMaxHeight()
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().height(stageHeight)) {
                    videoStageContent(Modifier.fillMaxSize())
                    GamingStageHeader(state, onOpenControls, onRequestEnd)
                }
                GamingChatColumn(
                    state = state,
                    draftMessage = draftMessage,
                    onDraftChange = onDraftChange,
                    onSendUtterance = onSendUtterance,
                    onRemoveMessage = onRemoveMessage,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GamingStageHeader(
    state: LiveSessionUiState,
    onOpenControls: () -> Unit,
    onRequestEnd: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = GamingLiveViolet) {
                    Text(
                        text = "LIVE • ${formatCompactCount(state.currentViewers)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("live_viewer_count")
                    )
                }
                SimulationWatermarkPill(compact = true)
            }
            Row {
                IconButton(onClick = onOpenControls, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = "Studio controls", tint = Color.White)
                }
                IconButton(onClick = onRequestEnd, modifier = Modifier.size(34.dp).testTag("live_end_btn")) {
                    Icon(Icons.Default.Close, contentDescription = "End live", tint = Color.White)
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            CreatorUtteranceBanner(state)
        }
    }
}

@Composable
private fun GamingChatColumn(
    state: LiveSessionUiState,
    draftMessage: String,
    onDraftChange: (String) -> Unit,
    onSendUtterance: () -> Unit,
    onRemoveMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.visibleChat.size) {
        if (state.visibleChat.isNotEmpty()) {
            listState.animateScrollToItem(state.visibleChat.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFF18181B))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Leaderboard strip (Bits / Gift Subs)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TOP BITS (VIRTUAL)", style = MaterialTheme.typography.labelSmall, color = GamingLiveViolet)
            AiLatencyChip(state.aiStatus)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val leaders = state.topSupporters.take(3)
            if (leaders.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF26262C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No virtual Bits cheered yet • Chat active",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                leaders.forEachIndexed { i, sup ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF26262C),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("#${i + 1}", style = MaterialTheme.typography.labelSmall, color = VideoLiveGold)
                            Text(
                                text = sup.username,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatCompactCount(sup.totalCoins),
                                style = MaterialTheme.typography.labelSmall,
                                color = GamingLiveEmerald
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Stream Summary live context card (inspired by Twitch Stream Summary block)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF26262C),
            border = BorderStroke(1.dp, GamingLiveViolet.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Stream Summary (Simulated)", style = MaterialTheme.typography.labelSmall, color = GamingLiveViolet)
                Text(
                    text = state.viralBannerText
                        ?: state.lastCreatorUtterance.ifBlank { "Live session ${formatDuration(state.elapsedSec)} • ${state.uniqueParticipants} active chatters" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2
                )
            }
        }

        // Dense gaming chat
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(state.visibleChat, key = { it.eventId }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeColor = when (msg.archetype) {
                        ViewerArchetype.MODERATOR -> GamingLiveEmerald
                        ViewerArchetype.SUPPORTER -> GamingLiveViolet
                        ViewerArchetype.LOYAL_FAN -> PhotoLiveRose
                        else -> TextMuted
                    }
                    if (msg.archetype.badgeLabel != null) {
                        Icon(
                            imageVector = if (msg.archetype == ViewerArchetype.MODERATOR) Icons.Default.Shield else Icons.Default.Star,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.padding(top = 3.dp).size(13.dp)
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = badgeColor, fontWeight = FontWeight.Bold)) {
                                append("${msg.username}: ")
                            }
                            withStyle(SpanStyle(color = Color.White)) {
                                append(msg.text)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemoveMessage(msg.eventId) }, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Mod delete", tint = TextMuted, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }

        LivePromptInputPill(
            value = draftMessage,
            onValueChange = onDraftChange,
            onSend = onSendUtterance,
            placeholder = "Send a message / speak...",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CreatorUtteranceBanner(state: LiveSessionUiState) {
    val activeSpeech = state.creatorPartialSpeech.ifBlank { state.lastCreatorUtterance }
    if (activeSpeech.isBlank() && state.viralBannerText == null) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        state.viralBannerText?.let { viral ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = FameMint.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, FameMint)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = FameMint, modifier = Modifier.size(14.dp))
                    Text(viral, style = MaterialTheme.typography.labelSmall, color = FameMint)
                }
            }
        }
        if (activeSpeech.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StudioObsidian.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, StudioOutline)
            ) {
                Text(
                    text = if (state.creatorPartialSpeech.isNotBlank()) "Listening: \"$activeSpeech\"" else "You said: \"$activeSpeech\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun AiLatencyChip(aiStatus: AiStatus) {
    val (label, color) = when (aiStatus) {
        AiStatus.Ready -> "AI Ready" to FameMint
        AiStatus.Thinking -> "AI Thinking..." to ShortLiveNeon
        is AiStatus.Active -> "${aiStatus.source.take(6)} • ${aiStatus.latencyMs}ms" to FameMint
        is AiStatus.Degraded -> "Degraded (Offline)" to AlertAmber
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = StudioObsidian.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PinnedBanner(pinned: ChatEvent, onUnpin: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = StudioSurfaceElevated.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, FameMint.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = FameMint, modifier = Modifier.size(14.dp))
            Text(
                text = "${pinned.username}: ${pinned.text}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onUnpin, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Unpin", tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun LivePromptInputPill(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(999.dp),
        color = StudioObsidian.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, StudioOutline)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f).testTag("live_utterance_input")
            )
            IconButton(
                onClick = onSend,
                modifier = Modifier.size(38.dp).testTag("live_send_utterance_btn")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send utterance", tint = FameMint, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun HostCircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(StudioObsidian.copy(alpha = 0.78f))
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
    }
}
