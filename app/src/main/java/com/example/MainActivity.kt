package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.EndStreamSummaryScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LiveRoomScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PreLivePreviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.FameLifeTheme
import com.example.ui.theme.FameMint
import com.example.ui.theme.FameMintContainer
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.FameLifeViewModel

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    DASHBOARD,
    PRE_LIVE,
    LIVE_ROOM,
    END_SUMMARY
}

enum class DashboardTab(val label: String, val icon: ImageVector) {
    STUDIO("Studio", Icons.Default.Videocam),
    HISTORY("History", Icons.Default.History),
    FANS("Fans", Icons.Default.Groups),
    WALLET("Wallet", Icons.Default.AccountBalanceWallet),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure App Check with launch intent before Compose UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            FameLifeTheme {
                FameLifeAppRoot()
            }
        }
    }
}

@Composable
fun FameLifeAppRoot(
    vm: FameLifeViewModel = viewModel()
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.creatorProfile.collectAsStateWithLifecycle()
    val sessions by vm.streamHistory.collectAsStateWithLifecycle()
    val ledgerEntries by vm.ledgerEntries.collectAsStateWithLifecycle()
    val walletCoins by vm.walletTotalCoins.collectAsStateWithLifecycle()
    val returningFans by vm.returningFans.collectAsStateWithLifecycle()
    val liveState by vm.liveState.collectAsStateWithLifecycle()
    val lastCompleted by vm.lastCompletedSession.collectAsStateWithLifecycle()
    val preLiveSource by vm.preLiveSource.collectAsStateWithLifecycle()
    val studioNotifications by vm.studioNotifications.collectAsStateWithLifecycle()

    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.SPLASH) }
    var currentTab by rememberSaveable { mutableStateOf(DashboardTab.STUDIO) }

    when (currentScreen) {
        AppScreen.SPLASH -> {
            SplashScreen(
                onFinished = {
                    currentScreen = if (settings.onboardingCompleted) {
                        AppScreen.DASHBOARD
                    } else {
                        AppScreen.ONBOARDING
                    }
                }
            )
        }

        AppScreen.ONBOARDING -> {
            OnboardingScreen(
                onComplete = { name, handle, bio, isGuest, avatarUri ->
                    vm.completeOnboarding(name, handle, bio, isGuest, avatarUri)
                    currentScreen = AppScreen.DASHBOARD
                }
            )
        }

        AppScreen.DASHBOARD -> {
            BackHandler(enabled = currentTab != DashboardTab.STUDIO) {
                currentTab = DashboardTab.STUDIO
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StudioObsidian)
            ) {
                val useNavRail = maxWidth >= 600.dp

                if (useNavRail) {
                    Scaffold(
                        containerColor = StudioObsidian,
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavigationRail(
                                containerColor = StudioSurface,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                DashboardTab.entries.forEach { tab ->
                                    NavigationRailItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = FameMint,
                                            selectedTextColor = FameMint,
                                            indicatorColor = FameMintContainer,
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted
                                        )
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                DashboardTabContent(
                                    tab = currentTab,
                                    vm = vm,
                                    settings = settings,
                                    profile = profile,
                                    sessions = sessions,
                                    ledgerEntries = ledgerEntries,
                                    walletCoins = walletCoins,
                                    returningFans = returningFans,
                                    notifications = studioNotifications,
                                    onOpenPreLive = { currentScreen = AppScreen.PRE_LIVE },
                                    onSelectTab = { currentTab = it },
                                    onResetDone = { currentScreen = AppScreen.ONBOARDING }
                                )
                            }
                        }
                    }
                } else {
                    Scaffold(
                        containerColor = StudioObsidian,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            NavigationBar(containerColor = StudioSurface) {
                                DashboardTab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label, maxLines = 1) },
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = FameMint,
                                            selectedTextColor = FameMint,
                                            indicatorColor = FameMintContainer,
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            DashboardTabContent(
                                tab = currentTab,
                                vm = vm,
                                settings = settings,
                                profile = profile,
                                sessions = sessions,
                                ledgerEntries = ledgerEntries,
                                walletCoins = walletCoins,
                                returningFans = returningFans,
                                notifications = studioNotifications,
                                onOpenPreLive = { currentScreen = AppScreen.PRE_LIVE },
                                onSelectTab = { currentTab = it },
                                onResetDone = { currentScreen = AppScreen.ONBOARDING }
                            )
                        }
                    }
                }
            }
        }

        AppScreen.PRE_LIVE -> {
            BackHandler { currentScreen = AppScreen.DASHBOARD }
            PreLivePreviewScreen(
                settings = settings,
                sourceMode = preLiveSource,
                onSelectSource = vm::setPreLiveSource,
                onToggleVisualUnderstanding = vm::toggleVisualUnderstanding,
                onBack = { currentScreen = AppScreen.DASHBOARD },
                onGoLive = { hasMicPerm ->
                    vm.startLiveStream(hasMicPermission = hasMicPerm)
                    currentScreen = AppScreen.LIVE_ROOM
                }
            )
        }

        AppScreen.LIVE_ROOM -> {
            LiveRoomScreen(
                state = liveState,
                settings = settings,
                profile = profile,
                onSubmitUtterance = vm::submitCreatorUtterance,
                onVisualFrameSampled = vm::onVisualFrameSampled,
                onDismissSmallGift = vm::dismissSmallGift,
                onDismissLargeGift = vm::dismissLargeGift,
                onPinMessage = vm::togglePinMessage,
                onRemoveMessage = vm::moderateRemoveMessage,
                onTriggerHeart = vm::triggerFloatingHeart,
                onFlipCamera = vm::switchCameraFacing,
                onToggleMic = vm::toggleMicMute,
                onToggleCameraOff = vm::toggleCameraOff,
                onSwitchSourceMode = vm::switchSourceMode,
                onToggleChatVisible = vm::toggleChatVisibility,
                onTogglePause = vm::togglePauseLive,
                onToggleVisualAi = vm::toggleVisualUnderstanding,
                onEndStreamConfirmed = {
                    vm.endLiveStream {
                        currentScreen = AppScreen.END_SUMMARY
                    }
                }
            )
        }

        AppScreen.END_SUMMARY -> {
            BackHandler { currentScreen = AppScreen.DASHBOARD }
            val session = lastCompleted
            if (session == null) {
                currentScreen = AppScreen.DASHBOARD
            } else {
                EndStreamSummaryScreen(
                    session = session,
                    timeline = vm.parseTimeline(session.timelineJson),
                    exportText = vm.buildExportReport(session),
                    onDone = { currentScreen = AppScreen.DASHBOARD }
                )
            }
        }
    }
}

@Composable
private fun DashboardTabContent(
    tab: DashboardTab,
    vm: FameLifeViewModel,
    settings: com.example.data.UserSettings,
    profile: com.example.data.CreatorProfileEntity?,
    sessions: List<com.example.data.StreamSessionEntity>,
    ledgerEntries: List<com.example.data.LedgerEntryEntity>,
    walletCoins: Int,
    returningFans: List<com.example.data.ReturningFanEntity>,
    notifications: List<com.example.viewmodel.StudioNotification>,
    onOpenPreLive: () -> Unit,
    onSelectTab: (DashboardTab) -> Unit,
    onResetDone: () -> Unit
) {
    when (tab) {
        DashboardTab.STUDIO -> {
            HomeDashboardScreen(
                profile = profile,
                settings = settings,
                walletCoins = walletCoins,
                sessionsCount = sessions.size,
                notifications = notifications,
                onDismissNotification = vm::dismissNotification,
                onClearNotifications = vm::clearAllNotifications,
                onUpdateProfile = vm::updateCreatorProfile,
                onSelectPlatform = vm::selectPlatform,
                onSelectFamePreset = vm::selectFamePreset,
                onUpdateCustomFame = vm::updateCustomFame,
                onSelectLanguage = vm::updateLanguage,
                onSelectTone = vm::updateTone,
                onOpenPreLive = onOpenPreLive,
                onOpenSettings = { onSelectTab(DashboardTab.SETTINGS) }
            )
        }
        DashboardTab.HISTORY -> {
            HistoryScreen(
                sessions = sessions,
                parseTimeline = vm::parseTimeline,
                buildExportText = vm::buildExportReport,
                onDeleteSession = vm::deleteSession
            )
        }
        DashboardTab.FANS -> {
            CommunityScreen(
                fans = returningFans,
                fanMemoryEnabled = settings.fanMemoryEnabled,
                onDeleteFan = vm::deleteReturningFan
            )
        }
        DashboardTab.WALLET -> {
            WalletScreen(
                totalCoins = walletCoins,
                entries = ledgerEntries
            )
        }
        DashboardTab.SETTINGS -> {
            SettingsScreen(
                settings = settings,
                profile = profile,
                onUpdateProfile = vm::updateCreatorProfile,
                onLanguageChange = vm::updateLanguage,
                onToneChange = vm::updateTone,
                onChatSpeedChange = vm::updateChatSpeed,
                onGiftFreqChange = vm::updateGiftFrequency,
                onToggleVisualAi = vm::toggleVisualUnderstanding,
                onToggleFanMemory = vm::toggleFanMemory,
                onToggleReducedMotion = vm::toggleReducedMotion,
                onDeleteAllData = {
                    vm.deleteAllDataAndReset()
                    onResetDone()
                }
            )
        }
    }
}
