package com.layxy.easyaicommit.client

import com.google.gson.JsonParser
import com.intellij.testFramework.LightPlatformTestCase
import com.layxy.easyaicommit.EasyAiCommitBundle.message
import com.layxy.easyaicommit.model.ProviderPreset
import com.layxy.easyaicommit.settings.ModelProfileState
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

class ResponsesApiIntegrationTest : LightPlatformTestCase() {
    fun testGenerationSendsResponsesRequestAndCleansMessage() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{
                "status":"completed", "error":null,
                "output":[
                    {"type":"reasoning","summary":[]},
                    {"type":"message","status":"completed","content":[
                        {"type":"output_text","text":"```text\nfeat: support responses\n```"}
                    ]}
                ]
            }"""))
            val profile = ModelProfileState.defaultOpenAi().toModel().copy(
                baseUrl = " ${server.url("/gateway/v1/responses/")} ",
                model = "test-reasoning-model",
                requestTimeoutSeconds = 5,
            )
            assertEquals("feat: support responses", CompatibleModelClient.generate(profile, "test-key", "diff content"))
            val request = server.takeRequest(1, TimeUnit.SECONDS)!!
            assertEquals("POST", request.method)
            assertEquals("/gateway/v1/responses", request.path)
            assertEquals("Bearer test-key", request.getHeader("Authorization"))
            val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals("test-reasoning-model", body.get("model").asString)
            assertEquals("diff content", body.get("input").asString)
            assertTrue(body.get("instructions").asString.contains("commit message"))
            assertEquals(4096, body.get("max_output_tokens").asInt)
            assertFalse(body.get("stream").asBoolean)
            assertFalse(body.get("store").asBoolean)
            assertFalse(body.has("messages"))
            assertFalse(body.has("max_tokens"))
            assertFalse(body.has("temperature"))
        }
    }

    fun testExistingChatAndAnthropicRequestsKeepTheirFormats() {
        MockWebServer().use { server ->
            listOf(ProviderPreset.OPENAI, ProviderPreset.ANTHROPIC).forEach { preset ->
                val isOpenAi = preset == ProviderPreset.OPENAI
                val response = if (isOpenAi) """{"choices":[{"message":{"content":"fix: regression"}}]}"""
                    else """{"content":[{"type":"text","text":"fix: regression"}]}"""
                server.enqueue(MockResponse().setBody(response))
                val profile = ModelProfileState.fromPreset(preset).toModel().copy(
                    baseUrl = server.url("/v1").toString(), requestTimeoutSeconds = 5,
                )
                assertEquals("fix: regression", CompatibleModelClient.generate(profile, "test-key", "diff"))
                val request = server.takeRequest(1, TimeUnit.SECONDS)!!
                assertEquals(if (isOpenAi) "/v1/chat/completions" else "/v1/messages", request.path)
                val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
                assertTrue(body.has("messages"))
                assertEquals(512, body.get("max_tokens").asInt)
                assertFalse(body.has("input"))
                assertFalse(body.has("max_output_tokens"))
            }
        }
    }

    fun testResponsesFailuresDoNotReturnMessagesOrFallBack() {
        MockWebServer().use { server ->
            val profile = ModelProfileState.defaultOpenAi().toModel().copy(
                baseUrl = server.url("/v1/responses").toString(), requestTimeoutSeconds = 5,
            )
            val cases = listOf(
                MockResponse().setResponseCode(400).setBody("""{"error":{"message":"unsupported model"}}""") to
                    message("client.error.requestHttp", 400, ": unsupported model"),
                MockResponse().setBody("""{"status":"completed","output":[]}""") to
                    message("client.error.emptyMessage"),
                MockResponse().setBody("""{"status":"incomplete","incomplete_details":{"reason":"max_output_tokens"},
                    "output":[{"type":"message","content":[{"type":"output_text","text":"partial"}]}]}""") to
                    message("client.error.responsesNotCompleted", "max_output_tokens"),
            )
            cases.forEach { (response, expected) ->
                server.enqueue(response)
                val error = assertFailsWith<ModelRequestException> {
                    CompatibleModelClient.generate(profile, "test-key", "diff")
                }
                assertEquals(expected, error.message)
                assertEquals("/v1/responses", server.takeRequest(1, TimeUnit.SECONDS)!!.path)
            }
            assertEquals(cases.size, server.requestCount)
        }
    }
}
