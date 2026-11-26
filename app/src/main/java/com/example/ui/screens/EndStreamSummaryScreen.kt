package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.StreamSessionEntity
import com.example.domain.model.FamePreset
import com.example.domain.model.LivePlatformMode
import com.example.domain.model.ViewerTimePoint
import com.example.ui.components.SimulationWatermarkPill
import com.example.ui.components.ViewerGrowthChart
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatDuration
import com.example.ui.theme.FameMint
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VideoLiveGold

@Composable
fun EndStreamSummaryScreen(
    session: StreamSessionEntity,
    timeline: List<ViewerTimePoint>,
    exportText: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val mode = LivePlatformMode.fromId(session.platformModeId)
    val preset = FamePreset.fromId(session.famePresetId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Stream Complete", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${mode.displayName} • ${preset.title} • ${formatDuration(session.durationSec)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    SimulationWatermarkPill()
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = StudioSurface,
                    border = BorderStroke(1.dp, StudioOutline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Simulated Audience Trajectory", style = MaterialTheme.typography.titleMedium)
                        ViewerGrowthChart(points = timeline, accentColor = modeAccentColor(mode))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("00:00", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("Peak ${formatCompactCount(session.peakViewers)}", style = MaterialTheme.typography.labelSmall, color = FameMint)
                            Text(formatDuration(session.durationSec), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryMetricCard("Peak Viewers", formatCompactCount(session.peakViewers), Modifier.weight(1f))
                        SummaryMetricCard("Avg Viewers", formatCompactCount(session.avgViewers), Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryMetricCard("Participants", formatCompactCount(session.uniqueParticipants), Modifier.weight(1f))
                        SummaryMetricCard("Messages", formatCompactCount(session.totalMessages), Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryMetricCard("New Followers", "+${formatCompactCount(session.newFollowers)}", Modifier.weight(1f))
                        SummaryMetricCard("Retention", "${session.retentionPercent}%", Modifier.weight(1f))
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = StudioSurfaceVariant,
                        border = BorderStroke(1.dp, VideoLiveGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Simulated Virtual Earnings", style = MaterialTheme.typography.labelLarge, color = VideoLiveGold)
                                Text("${session.totalGifts} virtual gifts • Non-redeemable", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Text(
                                text = "${formatCompactCount(session.totalVirtualCoins)} coins",
                                style = MaterialTheme.typography.headlineMedium,
                                color = VideoLiveGold
                            )
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "FameLife Simulated Stream Summary")
                                putExtra(Intent.EXTRA_TEXT, exportText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Simulated Stats"))
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        border = BorderStroke(1.dp, StudioOutline)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Stats", color = TextPrimary)
                    }

                    Button(
                        onClick = onDone,
                        modifier = Modifier.weight(1f).height(52.dp).testTag("summary_done_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = FameMint, contentColor = StudioObsidian)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Back to Studio", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioOutline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = FameMint)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
