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
