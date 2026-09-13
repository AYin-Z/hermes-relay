package com.hermesandroid.relay.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hermesandroid.relay.ui.components.InjectedContextSheet
import com.hermesandroid.relay.ui.theme.HermesRelayTheme
import com.hermesandroid.relay.viewmodel.ChatViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h720dp-xhdpi")
class InjectedContextSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun gatewayPreviewLabelsUnsupportedContextWithoutShowingPhonePreamble() {
        showPreview("gateway")
        compose.onAllNodesWithText(
            "Not sent in Gateway chat. This connection does not support extra per-turn context.",
        ).assertCountEquals(2)
        compose.onNodeWithText("Hermes-Relay Android app", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Server-side persona").assertExists()
        compose.onRoot().captureRoboImage("build/ui-regression/injected-context-gateway.png")
    }

    @Test
    fun apiOnlyPreviewShowsThePreparedMobileContext() {
        showPreview("sessions")
        compose.onNodeWithText("Hermes-Relay Android app", substring = true).assertExists()
        compose.onNodeWithText("Not sent in Gateway chat", substring = true).assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/ui-regression/injected-context-api.png")
    }

    private fun showPreview(endpoint: String) {
        val preview = ChatViewModel().apply { streamingEndpoint = endpoint }.previewInjectedContext()
        compose.setContent {
            HermesRelayTheme(appThemeId = "hermes-relay", themePreference = "dark") {
                InjectedContextSheet(context = preview, onDismiss = {})
            }
        }
    }
}
