package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.UserSettings
import com.example.domain.model.LivePlatformMode
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.FameLifeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
    fun `onboarding displays simulation disclosure and guest flow works`() {
        var guestClicked = false
        composeTestRule.setContent {
            FameLifeTheme {
                OnboardingScreen(
                    onComplete = { _, _, _, isGuest ->
                        guestClicked = isGuest
                    }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Simulation & Privacy Disclosure").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_guest_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(guestClicked)
    }

    @Test
    fun `home dashboard renders platform modes and simulation badge`() {
        var selectedMode: LivePlatformMode? = null
        composeTestRule.setContent {
            FameLifeTheme {
                HomeDashboardScreen(
                    profile = null,
                    settings = UserSettings(),
                    walletCoins = 1250,
                    sessionsCount = 2,
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
        composeTestRule.onNodeWithTag("platform_card_photo_live").performClick()
        composeTestRule.waitForIdle()
        assertEquals(LivePlatformMode.PHOTO_LIVE, selectedMode)
    }
}
