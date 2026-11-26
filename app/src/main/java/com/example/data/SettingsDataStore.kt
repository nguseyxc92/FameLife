package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AudienceLanguage
import com.example.domain.model.AudienceTone
import com.example.domain.model.CustomFameConfig
import com.example.domain.model.FamePreset
import com.example.domain.model.LivePlatformMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "famelife_prefs")

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val selectedPlatform: LivePlatformMode = LivePlatformMode.SHORT_LIVE,
    val selectedFamePreset: FamePreset = FamePreset.RISING_STAR,
    val customFameConfig: CustomFameConfig = CustomFameConfig(),
    val language: AudienceLanguage = AudienceLanguage.ENGLISH,
    val tone: AudienceTone = AudienceTone.BALANCED,
    val chatSpeedLevel: Float = 0.6f,
    val giftFrequencyLevel: Float = 0.6f,
    val visualUnderstandingEnabled: Boolean = false,
    val fanMemoryEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val sessionReminderMinutes: Int = 30,
    val recordLiveEnabled: Boolean = false
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PLATFORM_ID = stringPreferencesKey("platform_id")
        val FAME_PRESET_ID = stringPreferencesKey("fame_preset_id")
        val CUSTOM_TARGET = intPreferencesKey("custom_target")
        val LANGUAGE_ID = stringPreferencesKey("language_id")
        val TONE_ID = stringPreferencesKey("tone_id")
        val CHAT_SPEED = stringPreferencesKey("chat_speed")
        val GIFT_FREQ = stringPreferencesKey("gift_freq")
        val VISUAL_AI = booleanPreferencesKey("visual_ai")
        val FAN_MEMORY = booleanPreferencesKey("fan_memory")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val REMINDER_MINS = intPreferencesKey("reminder_mins")
        val RECORD_LIVE = booleanPreferencesKey("record_live_enabled")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            selectedPlatform = LivePlatformMode.fromId(prefs[Keys.PLATFORM_ID] ?: "short_live"),
            selectedFamePreset = FamePreset.fromId(prefs[Keys.FAME_PRESET_ID] ?: "rising_star"),
            customFameConfig = CustomFameConfig(
                targetAudience = prefs[Keys.CUSTOM_TARGET] ?: 2500
            ),
            language = AudienceLanguage.fromId(prefs[Keys.LANGUAGE_ID] ?: "en"),
            tone = AudienceTone.fromId(prefs[Keys.TONE_ID] ?: "balanced"),
            chatSpeedLevel = prefs[Keys.CHAT_SPEED]?.toFloatOrNull() ?: 0.6f,
            giftFrequencyLevel = prefs[Keys.GIFT_FREQ]?.toFloatOrNull() ?: 0.6f,
            visualUnderstandingEnabled = prefs[Keys.VISUAL_AI] ?: false,
            fanMemoryEnabled = prefs[Keys.FAN_MEMORY] ?: true,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            sessionReminderMinutes = prefs[Keys.REMINDER_MINS] ?: 30,
            recordLiveEnabled = prefs[Keys.RECORD_LIVE] ?: false
        )
    }

    suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        context.dataStore.edit { prefs ->
            val current = UserSettings(
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                selectedPlatform = LivePlatformMode.fromId(prefs[Keys.PLATFORM_ID] ?: "short_live"),
                selectedFamePreset = FamePreset.fromId(prefs[Keys.FAME_PRESET_ID] ?: "rising_star"),
                customFameConfig = CustomFameConfig(targetAudience = prefs[Keys.CUSTOM_TARGET] ?: 2500),
                language = AudienceLanguage.fromId(prefs[Keys.LANGUAGE_ID] ?: "en"),
                tone = AudienceTone.fromId(prefs[Keys.TONE_ID] ?: "balanced"),
                chatSpeedLevel = prefs[Keys.CHAT_SPEED]?.toFloatOrNull() ?: 0.6f,
                giftFrequencyLevel = prefs[Keys.GIFT_FREQ]?.toFloatOrNull() ?: 0.6f,
                visualUnderstandingEnabled = prefs[Keys.VISUAL_AI] ?: false,
                fanMemoryEnabled = prefs[Keys.FAN_MEMORY] ?: true,
                reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
                sessionReminderMinutes = prefs[Keys.REMINDER_MINS] ?: 30,
                recordLiveEnabled = prefs[Keys.RECORD_LIVE] ?: false
            )
            val updated = transform(current)
            prefs[Keys.ONBOARDING_COMPLETED] = updated.onboardingCompleted
            prefs[Keys.PLATFORM_ID] = updated.selectedPlatform.id
            prefs[Keys.FAME_PRESET_ID] = updated.selectedFamePreset.id
            prefs[Keys.CUSTOM_TARGET] = updated.customFameConfig.targetAudience
            prefs[Keys.LANGUAGE_ID] = updated.language.id
            prefs[Keys.TONE_ID] = updated.tone.id
            prefs[Keys.CHAT_SPEED] = updated.chatSpeedLevel.toString()
            prefs[Keys.GIFT_FREQ] = updated.giftFrequencyLevel.toString()
            prefs[Keys.VISUAL_AI] = updated.visualUnderstandingEnabled
            prefs[Keys.FAN_MEMORY] = updated.fanMemoryEnabled
            prefs[Keys.REDUCED_MOTION] = updated.reducedMotion
            prefs[Keys.REMINDER_MINS] = updated.sessionReminderMinutes
            prefs[Keys.RECORD_LIVE] = updated.recordLiveEnabled
        }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
