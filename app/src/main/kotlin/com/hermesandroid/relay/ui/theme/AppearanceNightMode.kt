package com.hermesandroid.relay.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.Preferences
import com.hermesandroid.relay.data.AppearancePreferences
import com.hermesandroid.relay.data.CustomThemePreset

/**
 * Maps the persisted appearance preference onto AppCompat's night mode.
 *
 * Compose paints light/dark from [themePreference] directly, but the activity
 * still uses [Theme.AppCompat.DayNight]. Without locking night mode to the
 * saved preference, cold start follows the system (and OEM force-dark) even
 * when Appearance is set to Light — settings show Light while the UI stays
 * dark until the user toggles the mode control.
 */
internal object AppearanceNightMode {
    private val VALID_PREFERENCES = setOf("auto", "light", "dark")

    fun normalizePreference(raw: String?): String =
        raw?.takeIf { it in VALID_PREFERENCES } ?: "auto"

    fun nightModeFor(
        themePreference: String,
        themeMode: ThemeMode,
        customTheme: CustomThemePreset? = null,
    ): Int {
        customTheme?.let { preset ->
            return if (preset.isDark) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        }
        return when (themeMode) {
            ThemeMode.DARK_ONLY -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.LIGHT_ONLY -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.BOTH -> when (normalizePreference(themePreference)) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        }
    }

    fun nightModeFor(preferences: Preferences): Int {
        val themePreference = normalizePreference(preferences[AppearancePreferences.themeKey])
        val requestedThemeId = preferences[AppearancePreferences.appThemeKey]
        val customThemes = AppearancePreferences.decodeCustomThemes(
            preferences[AppearancePreferences.customThemesKey],
        )
        val customTheme = CustomThemePreset.idFromAppTheme(requestedThemeId)
            ?.let { id -> customThemes.firstOrNull { it.id == id } }
        val appTheme = customTheme?.toAppTheme() ?: AppThemes.byId(requestedThemeId)
        return nightModeFor(
            themePreference = customTheme?.mode ?: themePreference,
            themeMode = appTheme.mode,
            customTheme = customTheme,
        )
    }

    /** Apply only when the mode actually changes — avoids redundant uiMode churn. */
    fun apply(nightMode: Int) {
        try {
            if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }
        } catch (_: Throwable) {
            // Robolectric / headless hosts may not support night-mode switches.
        }
    }

    fun applyFromPreferences(preferences: Preferences) {
        apply(nightModeFor(preferences))
    }

    fun applyResolved(
        themePreference: String,
        appThemeId: String,
        customTheme: CustomThemePreset? = null,
    ) {
        val appTheme = customTheme?.toAppTheme() ?: AppThemes.byId(appThemeId)
        apply(
            nightModeFor(
                themePreference = customTheme?.mode ?: themePreference,
                themeMode = appTheme.mode,
                customTheme = customTheme,
            ),
        )
    }
}
