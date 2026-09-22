package com.hermesandroid.relay.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import com.hermesandroid.relay.data.CustomThemePreset
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceNightModeTest {
    @Test
    fun dualModeHonorsExplicitLightAndDark() {
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppearanceNightMode.nightModeFor("light", ThemeMode.BOTH),
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            AppearanceNightMode.nightModeFor("dark", ThemeMode.BOTH),
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppearanceNightMode.nightModeFor("auto", ThemeMode.BOTH),
        )
    }

    @Test
    fun fixedThemesIgnorePreferenceAxis() {
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppearanceNightMode.nightModeFor("dark", ThemeMode.LIGHT_ONLY),
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            AppearanceNightMode.nightModeFor("light", ThemeMode.DARK_ONLY),
        )
    }

    @Test
    fun customThemeModeWinsOverPreference() {
        val light = CustomThemePreset(
            id = "day",
            name = "Day",
            mode = CustomThemePreset.MODE_LIGHT,
            backgroundHex = "#F5F5F5",
            surfaceHex = "#FFFFFF",
            accentHex = "#0E18D6",
            textHex = "#111111",
        )
        val dark = light.copy(id = "night", name = "Night", mode = CustomThemePreset.MODE_DARK)
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppearanceNightMode.nightModeFor("dark", ThemeMode.BOTH, light),
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            AppearanceNightMode.nightModeFor("light", ThemeMode.BOTH, dark),
        )
    }

    @Test
    fun normalizePreferenceFallsBackToAuto() {
        assertEquals("auto", AppearanceNightMode.normalizePreference(null))
        assertEquals("auto", AppearanceNightMode.normalizePreference("sepia"))
        assertEquals("light", AppearanceNightMode.normalizePreference("light"))
    }
}
