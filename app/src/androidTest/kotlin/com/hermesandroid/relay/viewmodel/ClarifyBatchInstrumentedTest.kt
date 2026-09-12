package com.hermesandroid.relay.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermesandroid.relay.network.upstream.ChatHandler
import com.hermesandroid.relay.network.upstream.DashboardApiClient
import com.hermesandroid.relay.network.upstream.GatewayChatClient
import com.hermesandroid.relay.network.upstream.HermesApiClient
import com.hermesandroid.relay.ui.components.MessageBubble
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Production socket, ViewModel, transcript, Compose and IME actions on a virtual device. */
class ClarifyBatchInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var fixture: AndroidGatewayContractFixture
    private lateinit var scope: CoroutineScope
    private lateinit var gateway: GatewayChatClient
    private lateinit var viewModel: ChatViewModel
    private lateinit var handler: ChatHandler
    private lateinit var socket: WebSocket

    @Before fun setUp() {
        fixture = AndroidGatewayContractFixture()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val http = OkHttpClient()
        gateway = GatewayChatClient(
            initialDashboardClient = DashboardApiClient(fixture.server.url("/").toString().trimEnd('/'), http),
            okHttpClient = http, scope = scope,
            callbackDispatcher = { Handler(Looper.getMainLooper()).post(it) },
        )
        handler = ChatHandler().also { it.setSessionId("20260821_120000_fixture") }
        viewModel = ChatViewModel().also {
            it.initialize(HermesApiClient(fixture.server.url("/").toString(), "fixture-key"), handler)
            it.streamingEndpoint = "gateway"
            it.setProfileMessageLoader { Result.success(emptyList()) }
            it.updateGatewayClient(gateway)
        }
        compose.setContent {
            val messages by handler.messages.collectAsStateWithLifecycle()
            MaterialTheme {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(messages.size, key = { messages[it].id }) { index ->
                        MessageBubble(messages[index], showTimestamps = false,
                            onCardInput = viewModel::answerAsk, animationEnabled = false)
                    }
                }
            }
        }
        assertTrue(runBlocking { gateway.prewarmAwait("20260821_120000_fixture") })
        socket = fixture.awaitServerSocket()
    }

    @After fun tearDown() {
        viewModel.updateGatewayClient(null)
        gateway.shutdown()
        scope.cancel()
        fixture.shutdown()
    }

    @Test fun confirmedProgressSurvivesLifecycleAndCustomAnswerUsesIme() {
        compose.runOnIdle { viewModel.sendMessage("Ask two questions") }
        fixture.awaitRpc("prompt.submit")
        socket.send(fixture.event("clarify.request", Json.parseToJsonElement("""
            {"request_id":"batch-device","questions":[
              {"qid":"route/a","question":"Which route?","choices":["Canary","Immediate"]},
              {"qid":"notes:b","question":"Anything else?","choices":null}
            ]}
        """) as JsonObject, "fixture-live-1"))
        compose.waitUntil(10_000) { viewModel.pendingAsk.value != null }
        compose.onNodeWithText("Canary").performClick()
        compose.waitUntil(10_000) { viewModel.pendingAsk.value?.ask?.answers?.get("route/a") == "Canary" }
        assertEquals(JsonPrimitive("route/a"), fixture.awaitRpc("clarify.respond")["question_id"])
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithText("Question 2 of 2").assertIsDisplayed()
        compose.onNodeWithContentDescription("Type an answer…").apply {
            performClick()
            performTextInput("  Keep rollback ready  ")
            performImeAction()
        }
        compose.waitUntil(10_000) { viewModel.pendingAsk.value == null }
        compose.onNodeWithText("All questions answered").assertIsDisplayed()
        assertEquals(2, fixture.rpcCount("clarify.respond"))
        assertEquals(1, fixture.rpcCount("prompt.submit"))
    }
}
