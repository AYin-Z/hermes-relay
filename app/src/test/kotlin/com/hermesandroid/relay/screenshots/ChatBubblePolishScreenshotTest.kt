package com.hermesandroid.relay.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hermesandroid.relay.data.ChatMessage
import com.hermesandroid.relay.data.HermesCard
import com.hermesandroid.relay.data.HermesCardClarifyBatch
import com.hermesandroid.relay.data.HermesCardClarifyQuestion
import com.hermesandroid.relay.data.HermesCardInput
import com.hermesandroid.relay.data.MessageDeliveryStatus
import com.hermesandroid.relay.data.MessageRole
import com.hermesandroid.relay.ui.components.MessageBubble
import com.hermesandroid.relay.ui.theme.HermesRelayTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChatBubblePolishScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test @Config(qualifiers = "w360dp-h800dp-xhdpi")
    fun darkConversation() = capture("dark-conversation", "dark", 1f)

    @Test @Config(qualifiers = "w360dp-h800dp-xhdpi")
    fun lightConversation() = capture("light-conversation", "light", 1f)

    @Test @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun narrowLargeText() = capture("narrow-large-text", "dark", 1.5f)

    @Test @Config(qualifiers = "w720dp-h360dp-xhdpi")
    fun landscape() = capture("landscape", "dark", 1f)

    @Test @Config(qualifiers = "w360dp-h800dp-xhdpi")
    fun correctionAndTimeShareOneLine() {
        compose.setContent {
            HermesRelayTheme(themePreference = "dark") {
                MessageBubble(correction(), animationEnabled = false)
            }
        }
        val time = SimpleDateFormat("h:mm a", Locale.US).format(Date(TIMESTAMP))
        val timeBounds = compose.onNodeWithText(time, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val status = compose.onNodeWithText("Correction sent", useUnmergedTree = true)
        val statusBounds = status.fetchSemanticsNode().boundsInRoot
        assertTrue("Time and correction should share a footer row", kotlin.math.abs(timeBounds.center.y - statusBounds.center.y) <= 2f)
        val layouts = mutableListOf<TextLayoutResult>()
        status.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.single().lineCount)
    }

    private fun capture(name: String, theme: String, scale: Float) {
        val card = HermesCard(
            type = HermesCard.BuiltInTypes.ASK_CLARIFY,
            title = "Hermes needs clarification",
            id = "clarify",
            clarifyBatch = HermesCardClarifyBatch(listOf(
                HermesCardClarifyQuestion("q0", "Which accent color?", HermesCardInput(HermesCardInput.Kinds.TEXT), "Orange"),
                HermesCardClarifyQuestion("q1", "Which sections?", HermesCardInput(HermesCardInput.Kinds.CHOICE, multiSelect = true), "[\"Calendar\",\"Tasks\"]"),
            )),
        )
        val messages = listOf(
            ChatMessage(id = "ask", role = MessageRole.ASSISTANT, content = "", timestamp = TIMESTAMP, cards = listOf(card), clientOnly = true),
            ChatMessage(id = "reply", role = MessageRole.ASSISTANT, content = "Got both responses in one batch:\n\n- Accent: **Orange**\n- Sections: **Calendar and Tasks**", timestamp = TIMESTAMP),
            correction(),
        )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                HermesRelayTheme(themePreference = theme) {
                    LazyColumn(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(messages.size, key = { messages[it].id }) { index ->
                            MessageBubble(messages[index], showAgentIdentity = false, isFirstInGroup = index == 0 || index == 2,
                                isLastInGroup = index != 0, animationEnabled = false)
                        }
                    }
                }
            }
        }
        val directory = File("build/ui-evidence/bubble-polish").apply { mkdirs() }
        compose.onRoot().captureRoboImage(File(directory, "$name-top.png").path)
        repeat(3) { compose.onNode(hasScrollAction()).performTouchInput { swipeUp() } }
        compose.onRoot().captureRoboImage(File(directory, "$name-bottom.png").path)
    }

    private fun correction() = ChatMessage(id = "correction", role = MessageRole.USER, content = "Test", timestamp = TIMESTAMP,
        deliveryStatus = MessageDeliveryStatus.STEERED)

    companion object { private const val TIMESTAMP = 1_700_000_000_000L }
}
