package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CreatorProfileEntity
import com.example.data.UserSettings
import com.example.ui.components.EditCreatorAccountDialog
import com.example.viewmodel.StudioNotification
import com.example.domain.model.AudienceLanguage
import com.example.domain.model.AudienceTone
import com.example.domain.model.CustomFameConfig
import com.example.domain.model.FamePreset
import com.example.domain.model.LivePlatformMode
import com.example.ui.components.SimulationWatermarkPill
import com.example.ui.components.VerifiedBadgeIcon
import com.example.ui.components.ViewerAvatar
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.FameMint
import com.example.ui.theme.FameMintContainer
import com.example.ui.theme.GamingLiveViolet
import com.example.ui.theme.PhotoLiveRose
import com.example.ui.theme.ShortLiveNeon
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VideoLiveRed
import kotlin.math.roundToInt

@Composable
fun HomeDashboardScreen(
    profile: CreatorProfileEntity?,
    settings: UserSettings,
    walletCoins: Int,
    sessionsCount: Int,
    notifications: List<StudioNotification> = emptyList(),
    onDismissNotification: (String) -> Unit = {},
    onClearNotifications: () -> Unit = {},
    onUpdateProfile: (displayName: String, handle: String, bio: String, avatarUri: String?, isVerified: Boolean) -> Unit = { _, _, _, _, _ -> },
    onSelectPlatform: (LivePlatformMode) -> Unit,
    onSelectFamePreset: (FamePreset) -> Unit,
    onUpdateCustomFame: (CustomFameConfig) -> Unit,
    onSelectLanguage: (AudienceLanguage) -> Unit,
    onSelectTone: (AudienceTone) -> Unit,
    onOpenPreLive: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAccountDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                CreatorStudioHeader(
                    profile = profile,
                    walletCoins = walletCoins,
                    sessionsCount = sessionsCount,
                    unreadNotificationCount = notifications.size,
                    onEditAccount = { showAccountDialog = true },
                    onOpenNotifications = { showNotificationsDialog = true }
                )
            }

            if (notifications.isNotEmpty()) {
                item {
                    val latest = notifications.first()
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StudioSurface,
                        border = BorderStroke(1.dp, FameMint.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNotificationsDialog = true }
                            .testTag("main_menu_notification_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Studio notification",
                                tint = FameMint,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(latest.title, style = MaterialTheme.typography.labelLarge, color = FameMint)
                                Text(latest.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 2)
                            }
                            IconButton(
                                onClick = { onDismissNotification(latest.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss notification", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "1. Choose Studio Mode",
                    subtitle = "Four distinct layouts adapted for creator controls"
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(LivePlatformMode.entries) { mode ->
                        PlatformModeCard(
                            mode = mode,
                            selected = settings.selectedPlatform == mode,
                            onClick = { onSelectPlatform(mode) }
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "2. Fame Level",
                    subtitle = "Target concurrent audience range (natural stochastic flow)"
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = StudioSurface,
                    border = BorderStroke(1.dp, StudioOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FamePreset.entries.forEach { preset ->
                            FamePresetRow(
                                preset = preset,
                                selected = settings.selectedFamePreset == preset,
                                customTarget = settings.customFameConfig.targetAudience,
                                onClick = { onSelectFamePreset(preset) }
                            )
                        }

                        if (settings.selectedFamePreset == FamePreset.CUSTOM) {
                            CustomFameConfigurator(
                                config = settings.customFameConfig,
                                onChange = onUpdateCustomFame
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "3. Audience Language & Vibe",
                    subtitle = "100,000+ virtual identities • Smart audience engine"
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = StudioSurface,
                    border = BorderStroke(1.dp, StudioOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Chat Language", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AudienceLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = settings.language == lang,
                                    onClick = { onSelectLanguage(lang) },
                                    label = { Text(lang.label, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FameMint.copy(alpha = 0.2f),
                                        selectedLabelColor = FameMint
                                    )
                                )
                            }
                        }

                        Text("Audience Tone", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AudienceTone.entries.forEach { tone ->
                                FilterChip(
                                    selected = settings.tone == tone,
                                    onClick = { onSelectTone(tone) },
                                    label = { Text(tone.label, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FameMint.copy(alpha = 0.2f),
                                        selectedLabelColor = FameMint
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onOpenPreLive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("home_open_prelive_btn"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FameMint,
                        contentColor = StudioObsidian
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Enter ${settings.selectedPlatform.displayName} Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showAccountDialog) {
        EditCreatorAccountDialog(
            profile = profile,
            onDismiss = { showAccountDialog = false },
            onSave = onUpdateProfile
        )
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            containerColor = StudioSurface,
            title = { Text("Studio Notifications") },
            text = {
                if (notifications.isEmpty()) {
                    Text("You're all caught up — no notices right now.", color = TextSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        notifications.forEach { note ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StudioSurfaceVariant,
                                border = BorderStroke(1.dp, StudioOutline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(note.title, style = MaterialTheme.typography.labelLarge, color = FameMint)
                                        Text(note.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    IconButton(
                                        onClick = { onDismissNotification(note.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Close", color = FameMint)
                }
            },
            dismissButton = if (notifications.isNotEmpty()) {
                {
                    TextButton(onClick = {
                        onClearNotifications()
                        showNotificationsDialog = false
                    }) {
                        Text("Clear All", color = TextSecondary)
                    }
                }
            } else null
        )
    }
}

@Composable
private fun CreatorStudioHeader(
    profile: CreatorProfileEntity?,
    walletCoins: Int,
    sessionsCount: Int,
    unreadNotificationCount: Int,
    onEditAccount: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioOutline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onEditAccount)
                        .testTag("header_account_row")
                ) {
                    ViewerAvatar(
                        initials = (profile?.displayName ?: "CR").take(2).uppercase(),
                        seed = 17,
                        size = 48.dp,
                        hasHighlightRing = true,
                        hasProfilePicture = !profile?.avatarUri.isNullOrBlank(),
                        imageUri = profile?.avatarUri
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = profile?.displayName ?: "Creator Studio",
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (profile?.isVerified == true) {
                                VerifiedBadgeIcon(size = 17.dp)
                            }
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Account",
                                tint = FameMint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "@${profile?.handle ?: "guest_live"}" + if (profile?.isGuest == true) " • Guest" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.size(38.dp).testTag("notifications_bell_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (unreadNotificationCount > 0) FameMint else TextSecondary
                            )
                        }
                        if (unreadNotificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(FameMint)
                            )
                        }
                    }
                    SimulationWatermarkPill(compact = true)
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChip(
                    label = "Virtual Followers",
                    value = formatCompactCount(profile?.virtualFollowers ?: 0),
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Live Sessions",
                    value = sessionsCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Virtual Coins",
                    value = formatCompactCount(walletCoins),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = StudioSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = FameMint)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun PlatformModeCard(
    mode: LivePlatformMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = modeAccentColor(mode)
    val icon = modeIcon(mode)

    Surface(
        modifier = Modifier
            .width(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("platform_card_${mode.id}"),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) StudioSurfaceElevated else StudioSurface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accent else StudioOutline
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = mode.displayName, tint = accent)
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(mode.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(mode.inspirationLabel, style = MaterialTheme.typography.labelSmall, color = accent)
            Spacer(Modifier.height(6.dp))
            Text(
                text = mode.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun FamePresetRow(
    preset: FamePreset,
    selected: Boolean,
    customTarget: Int,
    onClick: () -> Unit
) {
    val fraction = when (preset) {
        FamePreset.NEW_CREATOR -> 0.12f
        FamePreset.RISING_STAR -> 0.28f
        FamePreset.INFLUENCER -> 0.44f
        FamePreset.INTERNET_CELEBRITY -> 0.62f
        FamePreset.SUPERSTAR -> 0.80f
        FamePreset.GLOBAL_ICON -> 1.0f
        FamePreset.CUSTOM -> (customTarget / 150_000f).coerceIn(0.08f, 1.0f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) FameMintContainer.copy(alpha = 0.55f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("fame_preset_${preset.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (preset == FamePreset.CUSTOM) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = FameMint, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = preset.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) FameMint else TextPrimary
                )
            }
            Text(
                text = if (preset == FamePreset.CUSTOM) "Target ~${formatCompactCount(customTarget)}" else preset.rangeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = if (selected) FameMint else FameMint.copy(alpha = 0.45f),
            trackColor = StudioSurfaceVariant
        )
    }
}

@Composable
private fun CustomFameConfigurator(
    config: CustomFameConfig,
    onChange: (CustomFameConfig) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = StudioSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Custom Fame Tuning", style = MaterialTheme.typography.labelLarge, color = FameMint)
            Text("Target Audience: ${formatCompactCount(config.targetAudience)}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = config.targetAudience.toFloat(),
                onValueChange = { onChange(config.copy(targetAudience = it.roundToInt())) },
                valueRange = 50f..150_000f,
                colors = SliderDefaults.colors(thumbColor = FameMint, activeTrackColor = FameMint)
            )
            Text("Growth Speed: ${(config.growthSpeed * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = config.growthSpeed,
                onValueChange = { onChange(config.copy(growthSpeed = it)) },
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(thumbColor = FameMint, activeTrackColor = FameMint)
            )
            Text("Gift Generosity: ${(config.generosity * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = config.generosity,
                onValueChange = { onChange(config.copy(generosity = it)) },
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(thumbColor = FameMint, activeTrackColor = FameMint)
            )
        }
    }
}

fun modeAccentColor(mode: LivePlatformMode): Color = when (mode) {
    LivePlatformMode.PHOTO_LIVE -> PhotoLiveRose
    LivePlatformMode.SHORT_LIVE -> ShortLiveNeon
    LivePlatformMode.VIDEO_LIVE -> VideoLiveRed
    LivePlatformMode.GAMING_LIVE -> GamingLiveViolet
}

fun modeIcon(mode: LivePlatformMode): ImageVector = when (mode) {
    LivePlatformMode.PHOTO_LIVE -> Icons.Default.PhotoCamera
    LivePlatformMode.SHORT_LIVE -> Icons.Default.VideoCameraFront
    LivePlatformMode.VIDEO_LIVE -> Icons.Default.LiveTv
    LivePlatformMode.GAMING_LIVE -> Icons.Default.SportsEsports
}
