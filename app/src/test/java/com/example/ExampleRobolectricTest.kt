package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.CreatorProfileEntity
import com.example.data.StreamSessionEntity
import com.example.data.UserSettings
import com.example.domain.model.LivePlatformMode
import com.example.domain.model.StreamSourceMode
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PhotoLiveOverlay
import com.example.ui.screens.PreLivePreviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FameLifeTheme
import com.example.viewmodel.LiveSessionUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches FameLife`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FameLife", appName)
    }

    @Test
    fun `onboarding displays simulation disclosure, verified toggle, and guest flow works`() {
        var guestClicked = false
        var verifiedPassed = false
        composeTestRule.setContent {
            FameLifeTheme {
                OnboardingScreen(
                    onComplete = { _, _, _, isGuest, _, isVerified ->
                        guestClicked = isGuest
                        verifiedPassed = isVerified
                    }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Simulation & Privacy Disclosure").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_name_input").performScrollTo()
        composeTestRule.onNodeWithText("Local Creator Profile").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_verified_switch").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("onboarding_guest_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(guestClicked)
        assertTrue(verifiedPassed)
    }

    @Test
    fun `home dashboard renders platform modes, notifications, and simulation badge`() {
        var selectedMode: LivePlatformMode? = null
        composeTestRule.setContent {
            FameLifeTheme {
                HomeDashboardScreen(
                    profile = CreatorProfileEntity(
                        displayName = "Alex Verified",
                        handle = "alex_v",
                        bio = "Verified creator",
                        isGuest = false,
                        virtualFollowers = 1200,
                        totalStreamsCount = 4,
                        isVerified = true
                    ),
                    settings = UserSettings(),
                    walletCoins = 1250,
                    sessionsCount = 2,
                    notifications = listOf(
                        com.example.viewmodel.StudioNotification(
                            id = "n1",
                            title = "Voice Input Notice",
                            message = "Shown in main menu only"
                        )
                    ),
                    onSelectPlatform = { selectedMode = it },
                    onSelectFamePreset = {},
                    onUpdateCustomFame = {},
                    onSelectLanguage = {},
                    onSelectTone = {},
                    onOpenPreLive = {},
                    onOpenSettings = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SIMULATED").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Verified creator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("main_menu_notification_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("platform_card_photo_live").performClick()
        composeTestRule.waitForIdle()
        assertEquals(LivePlatformMode.PHOTO_LIVE, selectedMode)
    }

    @Test
    fun `live utterance banner hides spoken transcription caption while keeping viral spike`() {
        composeTestRule.setContent {
            FameLifeTheme {
                com.example.ui.screens.CreatorUtteranceBanner(
                    state = LiveSessionUiState(
                        creatorPartialSpeech = "Hey everyone welcome to the stream",
                        lastCreatorUtterance = "How is everyone doing today?",
                        viralBannerText = "Viral surge • +240 viewers!"
                    )
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Viral surge • +240 viewers!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Listening: \"Hey everyone welcome to the stream\"").assertDoesNotExist()
        composeTestRule.onNodeWithText("You said: \"How is everyone doing today?\"").assertDoesNotExist()
    }

    @Test
    fun `settings about section is compact and opens Tebib Labs and Privacy popups`() {
        composeTestRule.setContent {
            FameLifeTheme {
                SettingsScreen(
                    settings = UserSettings(),
                    profile = CreatorProfileEntity(
                        displayName = "Tebib Host",
                        handle = "tebib_host",
                        bio = "Host bio",
                        isGuest = false,
                        virtualFollowers = 500,
                        totalStreamsCount = 2,
                        isVerified = true
                    ),
                    onLanguageChange = {},
                    onToneChange = {},
                    onChatSpeedChange = {},
                    onGiftFreqChange = {},
                    onToggleVisualAi = {},
                    onToggleFanMemory = {},
                    onToggleReducedMotion = {},
                    onDeleteAllData = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_verified_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_about_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("FameLife app icon").assertDoesNotExist()

        // Click Tebib Labs -> opens popup with full company bio
        composeTestRule.onNodeWithTag("about_tebib_labs_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("About Tebib Labs").assertIsDisplayed()
        composeTestRule.onNodeWithText("© 2026 Tebib Labs. All rights reserved.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()

        // Click Privacy -> opens Privacy popup
        composeTestRule.onNodeWithTag("about_privacy_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("100% Private Local Simulation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").performClick()
    }

    @Test
    fun `pre-live record livestream toggle works and live overlay shows verified badge without recording indicator`() {
        var recordToggled: Boolean? = null
        composeTestRule.setContent {
            FameLifeTheme {
                PreLivePreviewScreen(
                    settings = UserSettings(recordLiveEnabled = false),
                    sourceMode = StreamSourceMode.FRONT_CAMERA,
                    onSelectSource = {},
                    onToggleVisualUnderstanding = {},
                    onToggleRecordLive = { recordToggled = it },
                    onBack = {},
                    onGoLive = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("prelive_record_switch").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, recordToggled)
    }

    @Test
    fun `live overlay shows verified badge when verified and never shows recording indicator`() {
        composeTestRule.setContent {
            FameLifeTheme {
                PhotoLiveOverlay(
                    state = LiveSessionUiState(
                        isLive = true,
                        platformMode = LivePlatformMode.PHOTO_LIVE,
                        recordLiveEnabled = true,
                        currentViewers = 420
                    ),
                    creatorHandle = "star_creator",
                    creatorVerified = true,
                    reducedMotion = false,
                    draftMessage = "",
                    onDraftChange = {},
                    onSendUtterance = {},
                    onDismissSmallGift = {},
                    onPinMessage = {},
                    onTriggerHeart = {},
                    onOpenQuestions = {},
                    onOpenControls = {},
                    onRequestEnd = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_verified_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("REC", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Recording", substring = true).assertDoesNotExist()
    }

    @Test
    fun `history shows Watch and Share only for sessions with an existing recording file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempRec = File(context.cacheDir, "test_stream.mp4").apply {
            writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03))
        }
        val recordedSession = StreamSessionEntity(
            sessionId = "ses_rec",
            platformModeId = LivePlatformMode.SHORT_LIVE.id,
            famePresetId = "rising_star",
            startedAtMs = 1_700_000_000_000L,
            durationSec = 45,
            peakViewers = 250,
            avgViewers = 180,
            uniqueParticipants = 60,
            totalMessages = 140,
            newFollowers = 25,
            totalLikes = 900,
            totalGifts = 5,
            totalVirtualCoins = 300,
            retentionPercent = 72,
            timelineJson = "[]",
            recordingPath = tempRec.absolutePath
        )
        val unrecordedSession = recordedSession.copy(
            sessionId = "ses_plain",
            recordingPath = null
        )

        composeTestRule.setContent {
            FameLifeTheme {
                HistoryScreen(
                    sessions = listOf(recordedSession, unrecordedSession),
                    parseTimeline = { emptyList() },
                    buildExportText = { "" },
                    onDeleteSession = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("history_recorded_badge_ses_rec", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("history_watch_recording_ses_rec").assertIsDisplayed()
        composeTestRule.onNodeWithTag("history_share_recording_ses_rec").assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodesWithTag("history_recorded_badge_ses_plain", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        tempRec.delete()
    }
}
