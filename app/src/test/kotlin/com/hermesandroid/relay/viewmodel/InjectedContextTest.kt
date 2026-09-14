package com.hermesandroid.relay.viewmodel

import com.hermesandroid.relay.data.Profile
import com.hermesandroid.relay.util.AppContextSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InjectedContextTest {
    @Test
    fun gatewayPreviewExcludesPhoneContextEvenWhenSharingIsEnabled() {
        val viewModel = ChatViewModel().apply {
            streamingEndpoint = "gateway"
            appContextSettings = AppContextSettings(master = true, battery = true, currentApp = true)
            setSelectedProfileProvider { Profile(name = "writer", model = "test-model", systemMessage = "Profile persona") }
        }

        val preview = viewModel.previewInjectedContext()

        assertFalse(preview.perTurnContextSupported)
        assertTrue(preview.personaOwnedServerSide)
        assertNull(preview.personaPrompt)
        assertNull(preview.appContext)
        assertNull(preview.interfaceContext)
        assertNull(preview.mediaCapability)
        assertNull(preview.combinedSystemMessage)
    }

    @Test
    fun apiOnlyTransportsRetainOptedInPhoneContextAndMasterOffRemovesIt() {
        for (endpoint in listOf("sessions", "runs", "completions")) {
            val viewModel = ChatViewModel().apply { streamingEndpoint = endpoint }
            val enabled = viewModel.previewInjectedContext()
            assertTrue(enabled.perTurnContextSupported)
            assertFalse(enabled.personaOwnedServerSide)
            assertTrue(enabled.appContext!!.contains("Hermes-Relay Android app"))
            assertEquals(enabled.appContext, enabled.combinedSystemMessage)

            viewModel.appContextSettings = AppContextSettings(master = false, battery = true, currentApp = true)
            val disabled = viewModel.previewInjectedContext()
            assertNull(disabled.appContext)
            assertNull(disabled.combinedSystemMessage)
        }
    }

    @Test
    fun changingTransportRebuildsPreviewWithoutChangingSharingPreference() {
        val viewModel = ChatViewModel().apply { streamingEndpoint = "sessions" }
        val apiPreview = viewModel.previewInjectedContext()
        viewModel.streamingEndpoint = "gateway"
        assertNull(viewModel.previewInjectedContext().appContext)
        assertTrue(viewModel.appContextSettings.master)
        viewModel.streamingEndpoint = "sessions"
        assertEquals(apiPreview.appContext, viewModel.previewInjectedContext().appContext)
    }
}
