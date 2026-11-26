package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LedgerEntryEntity
import com.example.data.ReturningFanEntity
import com.example.data.StreamSessionEntity
import com.example.data.UserSettings
import com.example.domain.model.AudienceLanguage
import com.example.domain.model.AudienceTone
import com.example.domain.model.FamePreset
import com.example.domain.model.GiftType
import com.example.domain.model.LivePlatformMode
import com.example.domain.model.ViewerTimePoint
import com.example.ui.components.GiftArtworkIcon
import com.example.ui.components.SimulationWatermarkPill
import com.example.ui.components.ViewerAvatar
import com.example.ui.components.ViewerGrowthChart
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatDuration
import com.example.ui.theme.FameMint
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VideoLiveGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun HistoryScreen(
    sessions: List<StreamSessionEntity>,
    parseTimeline: (String) -> List<ViewerTimePoint>,
    buildExportText: (StreamSessionEntity) -> String,
    onDeleteSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expandedId by remember { mutableStateOf<String?>(null) }
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Box(modifier = modifier.fillMaxSize().background(StudioObsidian), contentAlignment = Alignment.TopCenter) {
        if (sessions.isEmpty()) {
            EmptyTabState(
                icon = Icons.Default.History,
                title = "No Stream History Yet",
                subtitle = "Start a simulated livestream from the Studio tab to record viewer graphs, retention, and virtual earnings."
            )
        } else {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live History", style = MaterialTheme.typography.headlineMedium)
                        SimulationWatermarkPill(compact = true)
                    }
                }

                items(sessions, key = { it.sessionId }) { session ->
                    val mode = LivePlatformMode.fromId(session.platformModeId)
                    val preset = FamePreset.fromId(session.famePresetId)
                    val isExpanded = expandedId == session.sessionId

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = StudioSurface,
                        border = BorderStroke(1.dp, StudioOutline),
                        modifier = Modifier.fillMaxWidth().clickable {
                            expandedId = if (isExpanded) null else session.sessionId
                        }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${mode.displayName} • ${preset.title}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${formatter.format(Date(session.startedAtMs))} • ${formatDuration(session.durationSec)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, buildExportText(session))
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Export Simulated Stream"))
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "Export stats", tint = FameMint)
                                    }
                                    IconButton(onClick = { onDeleteSession(session.sessionId) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete session", tint = TextMuted)
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Peak ${formatCompactCount(session.peakViewers)}", style = MaterialTheme.typography.labelLarge, color = FameMint)
                                Text("${session.uniqueParticipants} chatters", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                Text("${session.totalVirtualCoins} virtual coins", style = MaterialTheme.typography.labelLarge, color = VideoLiveGold)
                            }

                            if (isExpanded) {
                                ViewerGrowthChart(
                                    points = parseTimeline(session.timelineJson),
                                    accentColor = modeAccentColor(mode),
                                    modifier = Modifier.fillMaxWidth().height(130.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityScreen(
    fans: List<ReturningFanEntity>,
    fanMemoryEnabled: Boolean,
    onDeleteFan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(StudioObsidian), contentAlignment = Alignment.TopCenter) {
        if (fans.isEmpty()) {
            EmptyTabState(
                icon = Icons.Default.Groups,
                title = if (fanMemoryEnabled) "No Returning Fans Yet" else "Fan Memory Paused",
                subtitle = if (fanMemoryEnabled) {
                    "Frequent chatters and top gifters from your sessions will appear here and remember past topics."
                } else {
                    "Enable Fan Memory in Settings to let virtual viewers remember previous livestreams."
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column {
                        Text("Fan Community", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Virtual returning viewers • Deletable anytime",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                items(fans, key = { it.viewerId }) { fan ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StudioSurface,
                        border = BorderStroke(1.dp, StudioOutline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ViewerAvatar(
                                initials = fan.username.take(2).uppercase(),
                                seed = abs(fan.viewerId.hashCode()),
                                size = 42.dp,
                                hasHighlightRing = fan.loyaltyScore >= 0.8f
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(fan.username, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${fan.joinCount} streams",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FameMint
                                    )
                                }
                                if (!fan.lastTopic.isNullOrBlank()) {
                                    Text(
                                        "Remembers: \"${fan.lastTopic}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { fan.loyaltyScore.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)),
                                    color = FameMint,
                                    trackColor = StudioSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteFan(fan.viewerId) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Forget fan", tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalletScreen(
    totalCoins: Int,
    entries: List<LedgerEntryEntity>,
    modifier: Modifier = Modifier
) {
    val reconciledTotal = remember(entries) { entries.sumOf { it.virtualCoins } }

    Box(modifier = modifier.fillMaxSize().background(StudioObsidian), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = StudioSurface,
                    border = BorderStroke(1.dp, VideoLiveGold.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Virtual Wallet", style = MaterialTheme.typography.titleLarge)
                            SimulationWatermarkPill(compact = true)
                        }
                        Text(
                            text = "${formatCompactCount(totalCoins)} coins",
                            style = MaterialTheme.typography.displayLarge,
                            color = VideoLiveGold
                        )
                        Text(
                            text = "100% simulated & non-redeemable • Ledger reconciled: $reconciledTotal coins across ${entries.size} events",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                Text("Virtual Gift Catalog", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(GiftType.entries) { gift ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = StudioSurfaceVariant,
                            border = BorderStroke(1.dp, StudioOutline)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                GiftArtworkIcon(gift = gift, size = 40.dp)
                                Text(gift.displayName, style = MaterialTheme.typography.labelLarge)
                                Text("${gift.virtualCoins} • ${gift.tier.label}", style = MaterialTheme.typography.labelSmall, color = FameMint)
                            }
                        }
                    }
                }
            }

            item {
                Text("Idempotent Gift Ledger", style = MaterialTheme.typography.titleMedium)
            }

            if (entries.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StudioSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No virtual gift transactions yet", color = TextSecondary)
                        }
                    }
                }
            } else {
                items(entries.take(80), key = { it.eventId }) { entry ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StudioSurface,
                        border = BorderStroke(1.dp, StudioOutline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${entry.viewerUsername} • ${entry.giftName}", style = MaterialTheme.typography.labelLarge)
                                Text("${entry.tierName} • ID ${entry.eventId.takeLast(8)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Text("+${entry.virtualCoins} coins", style = MaterialTheme.typography.labelLarge, color = VideoLiveGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onLanguageChange: (AudienceLanguage) -> Unit,
    onToneChange: (AudienceTone) -> Unit,
    onChatSpeedChange: (Float) -> Unit,
    onGiftFreqChange: (Float) -> Unit,
    onToggleVisualAi: (Boolean) -> Unit,
    onToggleFanMemory: (Boolean) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onDeleteAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(StudioObsidian), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Studio Settings", style = MaterialTheme.typography.headlineMedium)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = StudioSurface,
                    border = BorderStroke(1.dp, StudioOutline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Audience Simulation", style = MaterialTheme.typography.titleMedium)

                        Text("Language", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AudienceLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = settings.language == lang,
                                    onClick = { onLanguageChange(lang) },
                                    label = { Text(lang.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FameMint.copy(alpha = 0.2f),
                                        selectedLabelColor = FameMint
                                    )
                                )
                            }
                        }

                        Text("Tone", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AudienceTone.entries.forEach { tone ->
                                FilterChip(
                                    selected = settings.tone == tone,
                                    onClick = { onToneChange(tone) },
                                    label = { Text(tone.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FameMint.copy(alpha = 0.2f),
                                        selectedLabelColor = FameMint
                                    )
                                )
                            }
                        }

                        Text("Chat Speed", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Slider(
                            value = settings.chatSpeedLevel,
                            onValueChange = onChatSpeedChange,
                            colors = SliderDefaults.colors(thumbColor = FameMint, activeTrackColor = FameMint)
                        )

                        Text("Gift Frequency", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Slider(
                            value = settings.giftFrequencyLevel,
                            onValueChange = onGiftFreqChange,
                            colors = SliderDefaults.colors(thumbColor = FameMint, activeTrackColor = FameMint)
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = StudioSurface,
                    border = BorderStroke(1.dp, StudioOutline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Privacy, AI & Accessibility", style = MaterialTheme.typography.titleMedium)

                        SettingSwitchRow(
                            title = "Visual Scene Understanding",
                            subtitle = "Analyze sparse camera/screen frames during live streams",
                            checked = settings.visualUnderstandingEnabled,
                            onCheckedChange = onToggleVisualAi
                        )
                        SettingSwitchRow(
                            title = "Returning Fan Memory",
                            subtitle = "Let loyal virtual fans remember past stream topics locally",
                            checked = settings.fanMemoryEnabled,
                            onCheckedChange = onToggleFanMemory
                        )
                        SettingSwitchRow(
                            title = "Reduced Motion",
                            subtitle = "Minimize floating hearts and full-screen gift animations",
                            checked = settings.reducedMotion,
                            onCheckedChange = onToggleReducedMotion
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = StudioSurface,
                    border = BorderStroke(1.dp, StudioOutline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("About FameLife", style = MaterialTheme.typography.titleMedium)
                        Text("Developed by Tebib Labs • Independent simulation studio.", style = MaterialTheme.typography.bodyMedium, color = FameMint)
                        Text(
                            "Live modes are inspired by Instagram Live, TikTok LIVE, YouTube Live, and Twitch UX conventions. FameLife is not affiliated with or endorsed by any social media platform. All viewers, chats, and gifts are simulated.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("settings_delete_all_btn")
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete All Local Data & Reset", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = StudioSurface,
            title = { Text("Delete All Local Data?") },
            text = {
                Text(
                    "This removes your local creator profile, stream history, virtual wallet ledger, and returning fan memories.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onDeleteAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson, contentColor = Color.White)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = FameMint)
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = StudioObsidian, checkedTrackColor = FameMint)
        )
    }
}

@Composable
private fun EmptyTabState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = FameMint, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
