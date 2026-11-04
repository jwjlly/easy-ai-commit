package com.layxy.easyaicommit.client

import com.layxy.easyaicommit.EasyAiCommitBundle.message
import com.layxy.easyaicommit.model.ModelProfile
import com.layxy.easyaicommit.model.ModelProtocol
import com.layxy.easyaicommit.model.OutputLanguage
import com.layxy.easyaicommit.model.ProviderPreset
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CompatibleModelClientTest {
    @Test
    fun `builds compatible endpoints without duplicating paths`() {
        assertEquals(
            "https://example.test/v1/chat/completions",
            CompatibleModelClient.endpoint("https://example.test/v1/", ModelProtocol.OPENAI_COMPATIBLE),
        )
        assertEquals(
            "https://example.test/v1/chat/completions",
            CompatibleModelClient.endpoint(
                "https://example.test/v1/chat/completions",
                ModelProtocol.OPENAI_COMPATIBLE,
            ),
        )
        assertEquals(
            "https://example.test/v1/messages",
            CompatibleModelClient.endpoint("https://example.test/v1", ModelProtocol.ANTHROPIC_COMPATIBLE),
        )
    }

    @Test
    fun `parses openai compatible message content`() {
        val json = """{"choices":[{"message":{"content":"feat: add provider profiles"}}]}"""
        assertEquals("feat: add provider profiles", CompatibleModelClient.parseOpenAiResponse(json))
    }

    @Test
    fun `parses anthropic compatible text blocks`() {
        val json = """{"content":[{"type":"text","text":"fix: handle empty selection"}]}"""
        assertEquals("fix: handle empty selection", CompatibleModelClient.parseAnthropicResponse(json))
    }

    @Test
    fun `preserves full responses endpoints and gateway prefixes`() {
        listOf("/v1/responses", "/gateway/openai/v1/responses", "/responses").forEach { path ->
            assertEquals(
                "https://example.test$path",
                CompatibleModelClient.endpoint("  https://example.test$path///  ", ModelProtocol.OPENAI_COMPATIBLE),
            )
        }
        assertEquals(
            "https://example.test/responses-proxy/chat/completions",
            CompatibleModelClient.endpoint("https://example.test/responses-proxy", ModelProtocol.OPENAI_COMPATIBLE),
        )
    }

    @Test
    fun `responses parser collects all output text and ignores reasoning and tools`() {
        val json = """{
            "status":"completed", "error":null,
            "output":[
                {"type":"reasoning","summary":[{"type":"summary_text","text":"private reasoning"}]},
                {"type":"function_call","arguments":"ignored"},
                {"type":"message","status":"completed","content":[
                    {"type":"output_text","text":"feat: add "},
                    {"type":"output_text","text":"responses"}
                ]},
                {"type":"message","content":[{"type":"output_text","text":"\n\nSupport gateways."}]}
            ]
        }"""
        assertEquals("feat: add responses\n\nSupport gateways.", CompatibleModelClient.parseResponsesResponse(json))
    }

    @Test
    fun `responses parser rejects unfinished generation even with partial output`() {
        listOf("incomplete", "failed", "cancelled", "queued", "in_progress").forEach { status ->
            val error = assertFailsWith<ModelRequestException> {
                CompatibleModelClient.parseResponsesResponse("""{
                    "status":"$status", "incomplete_details":null,
                    "output":[{"type":"message","content":[{"type":"output_text","text":"partial"}]}]
                }""")
            }
            assertEquals(message("client.error.responsesNotCompleted", status), error.message)
        }
        val error = assertFailsWith<ModelRequestException> {
            CompatibleModelClient.parseResponsesResponse("""{
                "status":"incomplete", "incomplete_details":{"reason":"max_output_tokens"}, "output":[]
            }""")
        }
        assertEquals(message("client.error.responsesNotCompleted", "max_output_tokens"), error.message)
        val itemError = assertFailsWith<ModelRequestException> {
            CompatibleModelClient.parseResponsesResponse("""{
                "output":[{"type":"message","status":"incomplete","content":[{"type":"output_text","text":"partial"}]}]
            }""")
        }
        assertEquals(message("client.error.responsesNotCompleted", "incomplete"), itemError.message)
    }

    @Test
    fun `responses parser surfaces errors and refusals`() {
        val remoteError = assertFailsWith<ModelRequestException> {
            CompatibleModelClient.parseResponsesResponse("""{"status":"failed","error":{"message":"invalid model"},"output":[]}""")
        }
        assertEquals("invalid model", remoteError.message)
        val refusal = assertFailsWith<ModelRequestException> {
            CompatibleModelClient.parseResponsesResponse("""{
                "status":"completed", "output":[{"type":"message","content":[
                    {"type":"output_text","text":"partial"}, {"type":"refusal","refusal":"Cannot comply"}
                ]}]
            }""")
        }
        assertEquals(message("client.error.responsesRefused"), refusal.message)
    }

    @Test
    fun `responses parser handles missing empty and malformed content`() {
        listOf("{}", "{\"output\":null}", "{\"output\":{}}").forEach { json ->
            val error = assertFailsWith<ModelRequestException> { CompatibleModelClient.parseResponsesResponse(json) }
            assertEquals(message("client.error.openaiMissingMessage"), error.message)
        }
        listOf(
            """{"output":[]}""",
            """{"output":[null,{},42,{"type":"message","content":null}]}""",
            """{"output":[{"type":"message","content":[null,{"type":"output_text","text":null},{"type":"output_text","text":42}]}]}""",
        ).forEach { json -> assertEquals("", CompatibleModelClient.parseResponsesResponse(json)) }
        val invalid = assertFailsWith<ModelRequestException> { CompatibleModelClient.parseResponsesResponse("not json") }
        assertEquals(message("client.error.invalidJson"), invalid.message)
    }

    @Test
    fun `surfaces compatible api errors`() {
        val error = assertFailsWith<ModelRequestException> {
            CompatibleModelClient.parseOpenAiResponse("""{"error":{"message":"invalid model"}}""")
        }
        assertEquals("invalid model", error.message)
    }

    @Test
    fun `removes markdown fences from generated message`() {
        assertEquals(
            "feat: add settings",
            CompatibleModelClient.cleanGeneratedMessage("```text\nfeat: add settings\n```"),
        )
    }

    @Test
    fun `uses provider and protocol specific authentication`() {
        val expectedAnthropicAuth = mapOf(
            ProviderPreset.CUSTOM to mapOf("x-api-key" to "secret"),
            ProviderPreset.DEEPSEEK to mapOf("x-api-key" to "secret"),
            ProviderPreset.OPENROUTER to mapOf("Authorization" to "Bearer secret"),
            ProviderPreset.GLM to mapOf("x-api-key" to "secret"),
            ProviderPreset.ALIBABA_BAILIAN to mapOf("x-api-key" to "secret"),
            ProviderPreset.KIMI to mapOf("Authorization" to "Bearer secret"),
            ProviderPreset.MINIMAX to mapOf("x-api-key" to "secret"),
            ProviderPreset.SILICONFLOW to mapOf("Authorization" to "Bearer secret"),
            ProviderPreset.STEPFUN to mapOf("Authorization" to "Bearer secret"),
            ProviderPreset.TENCENT_TOKENHUB to mapOf("x-api-key" to "secret"),
        )

        expectedAnthropicAuth.forEach { (preset, expectedHeaders) ->
            assertEquals(
                mapOf("Authorization" to "Bearer secret"),
                CompatibleModelClient.apiKeyHeaders(
                    profile(preset, ModelProtocol.OPENAI_COMPATIBLE),
                    "secret",
                ),
                preset.name,
            )
            assertEquals(
                expectedHeaders,
                CompatibleModelClient.apiKeyHeaders(
                    profile(preset, ModelProtocol.ANTHROPIC_COMPATIBLE),
                    "secret",
                ),
                preset.name,
            )
        }
    }

    @Test
    fun `rejects a protocol unsupported by the provider preset`() {
        val profile = ModelProfile(
            id = "invalid",
            name = "Invalid OpenAI",
            preset = ProviderPreset.OPENAI,
            protocol = ModelProtocol.ANTHROPIC_COMPATIBLE,
            baseUrl = "https://example.test/v1",
            model = "test-model",
            outputLanguage = OutputLanguage.ENGLISH.id,
            promptTemplate = "{changes}",
        )

        val error = assertFailsWith<ModelRequestException> {
            CompatibleModelClient.generate(profile, "secret", "test")
        }
        assertContains(
            error.message.orEmpty(),
            message(
                "client.error.protocolUnsupported",
                ProviderPreset.OPENAI.displayName,
                ModelProtocol.ANTHROPIC_COMPATIBLE.displayName,
            ),
        )
    }

    private fun profile(
        preset: ProviderPreset,
        protocol: ModelProtocol,
    ): ModelProfile {
        val config = preset.configFor(protocol)
        return ModelProfile(
            id = "${preset.name}-${protocol.name}",
            name = preset.displayName,
            preset = preset,
            protocol = protocol,
            baseUrl = config.baseUrl,
            model = config.defaultModel,
            outputLanguage = OutputLanguage.ENGLISH.id,
            promptTemplate = "{changes}",
        )
    }
}
