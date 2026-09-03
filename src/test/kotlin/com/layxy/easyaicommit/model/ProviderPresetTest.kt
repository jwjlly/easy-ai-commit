package com.layxy.easyaicommit.model

import com.layxy.easyaicommit.EasyAiCommitBundle.message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProviderPresetTest {
    @Test
    fun `protocol display names remain unlocalized vendor terms`() {
        assertEquals("OpenAI Compatible", ModelProtocol.OPENAI_COMPATIBLE.displayName)
        assertEquals("Anthropic Compatible", ModelProtocol.ANTHROPIC_COMPATIBLE.displayName)
        assertEquals("OpenAI Compatible", ModelProtocol.OPENAI_COMPATIBLE.toString())
        assertEquals("Anthropic Compatible", ModelProtocol.ANTHROPIC_COMPATIBLE.toString())
    }

    @Test
    fun `preset protocol matrix matches the design document`() {
        val expected = linkedMapOf(
            ProviderPreset.CUSTOM to expectedBoth(message("provider.custom")),
            ProviderPreset.OPENAI to expectedOpenAi("OpenAI"),
            ProviderPreset.ANTHROPIC to expectedAnthropic("Anthropic"),
            ProviderPreset.DEEPSEEK to expectedBoth("DeepSeek"),
            ProviderPreset.OPENROUTER to expectedBoth("OpenRouter"),
            ProviderPreset.GEMINI to expectedOpenAi("Gemini"),
            ProviderPreset.GROK to expectedOpenAi("Grok"),
            ProviderPreset.GLM to expectedBoth("GLM"),
            ProviderPreset.ALIBABA_BAILIAN to expectedBoth("Alibaba Bailian"),
            ProviderPreset.KIMI to expectedBoth("Kimi"),
            ProviderPreset.MINIMAX to expectedBoth(
                displayName = "MiniMax",
                defaultProtocol = ModelProtocol.ANTHROPIC_COMPATIBLE,
            ),
            ProviderPreset.SILICONFLOW to expectedBoth("SiliconFlow"),
            ProviderPreset.STEPFUN to expectedBoth("StepFun"),
            ProviderPreset.TENCENT_TOKENHUB to expectedBoth("Tencent TokenHub"),
            ProviderPreset.VOLCANO_ARK to expectedOpenAi("Volcano Ark"),
            ProviderPreset.BAIDU_QIANFAN to expectedOpenAi("Baidu Qianfan"),
            ProviderPreset.XFYUN_SPARK to expectedOpenAi("iFlytek Spark"),
            ProviderPreset.MISTRAL to expectedOpenAi("Mistral AI"),
            ProviderPreset.GROQ to expectedOpenAi("Groq"),
            ProviderPreset.TOGETHER to expectedOpenAi("Together AI"),
            ProviderPreset.FIREWORKS to expectedOpenAi("Fireworks AI"),
            ProviderPreset.CEREBRAS to expectedOpenAi("Cerebras"),
            ProviderPreset.COHERE to expectedOpenAi("Cohere"),
            ProviderPreset.PERPLEXITY to expectedOpenAi("Perplexity"),
            ProviderPreset.HUGGING_FACE to expectedOpenAi("Hugging Face"),
        )

        assertEquals(expected.keys.toList(), ProviderPreset.entries.toList())
        expected.forEach { (preset, expectedPreset) ->
            assertEquals(expectedPreset.displayName, preset.displayName, preset.name)
            assertEquals(expectedPreset.defaultProtocol, preset.defaultProtocol, preset.name)
            assertEquals(expectedPreset.supportedProtocols, preset.supportedProtocols, preset.name)
        }
    }

    @Test
    fun `custom preset supports both protocols with protocol specific authentication`() {
        val openAiConfig = ProviderPreset.CUSTOM.configFor(ModelProtocol.OPENAI_COMPATIBLE)
        val anthropicConfig = ProviderPreset.CUSTOM.configFor(ModelProtocol.ANTHROPIC_COMPATIBLE)

        assertEquals(ApiKeyAuth.BEARER, openAiConfig.apiKeyAuth)
        assertEquals(ApiKeyAuth.X_API_KEY, anthropicConfig.apiKeyAuth)
        assertTrue(openAiConfig.baseUrl.isBlank())
        assertTrue(openAiConfig.defaultModel.isBlank())
        assertTrue(anthropicConfig.baseUrl.isBlank())
        assertTrue(anthropicConfig.defaultModel.isBlank())
    }

    @Test
    fun `unknown preset id falls back to custom`() {
        assertEquals(ProviderPreset.CUSTOM, ProviderPreset.fromStored("UNKNOWN_PRESET"))
    }

    @Test
    fun `dual protocol presets use protocol specific endpoints and authentication`() {
        val expectedAnthropic = mapOf(
            ProviderPreset.DEEPSEEK to expectedConfig(
                "https://api.deepseek.com/anthropic/v1",
                ApiKeyAuth.X_API_KEY,
            ),
            ProviderPreset.OPENROUTER to expectedConfig(
                "https://openrouter.ai/api/v1",
                ApiKeyAuth.BEARER,
            ),
            ProviderPreset.GLM to expectedConfig(
                "https://open.bigmodel.cn/api/anthropic/v1",
                ApiKeyAuth.X_API_KEY,
            ),
            ProviderPreset.ALIBABA_BAILIAN to expectedConfig(
                "https://dashscope.aliyuncs.com/apps/anthropic/v1",
                ApiKeyAuth.X_API_KEY,
            ),
            ProviderPreset.KIMI to expectedConfig(
                "https://api.moonshot.cn/anthropic/v1",
                ApiKeyAuth.BEARER,
            ),
            ProviderPreset.MINIMAX to expectedConfig(
                "https://api.minimaxi.com/anthropic/v1",
                ApiKeyAuth.X_API_KEY,
            ),
            ProviderPreset.SILICONFLOW to expectedConfig(
                "https://api.siliconflow.cn/v1",
                ApiKeyAuth.BEARER,
            ),
            ProviderPreset.STEPFUN to expectedConfig(
                "https://api.stepfun.com/v1",
                ApiKeyAuth.BEARER,
            ),
            ProviderPreset.TENCENT_TOKENHUB to expectedConfig(
                "https://tokenhub.tencentmaas.com/v1",
                ApiKeyAuth.X_API_KEY,
            ),
        )

        expectedAnthropic.forEach { (preset, expectedConfig) ->
            val config = preset.configFor(ModelProtocol.ANTHROPIC_COMPATIBLE)
            assertEquals(expectedConfig.baseUrl, config.baseUrl, preset.name)
            assertEquals(expectedConfig.apiKeyAuth, config.apiKeyAuth, preset.name)
        }
    }

    @Test
    fun `new openai presets provide usable defaults`() {
        val expected = mapOf(
            ProviderPreset.VOLCANO_ARK to expectedTemplate(
                "https://ark.cn-beijing.volces.com/api/v3",
                "doubao-seed-2-0-lite-260215",
            ),
            ProviderPreset.BAIDU_QIANFAN to expectedTemplate(
                "https://qianfan.baidubce.com/v2",
                "ernie-4.5-turbo-20260402",
            ),
            ProviderPreset.XFYUN_SPARK to expectedTemplate(
                "https://spark-api-open.xf-yun.com/v1",
                "generalv3.5",
            ),
            ProviderPreset.MISTRAL to expectedTemplate(
                "https://api.mistral.ai/v1",
                "mistral-small-latest",
            ),
            ProviderPreset.GROQ to expectedTemplate(
                "https://api.groq.com/openai/v1",
                "openai/gpt-oss-120b",
            ),
            ProviderPreset.TOGETHER to expectedTemplate(
                "https://api.together.xyz/v1",
                "openai/gpt-oss-120b",
            ),
            ProviderPreset.FIREWORKS to expectedTemplate(
                "https://api.fireworks.ai/inference/v1",
                "accounts/fireworks/models/deepseek-v3p1",
            ),
            ProviderPreset.CEREBRAS to expectedTemplate(
                "https://api.cerebras.ai/v1",
                "gpt-oss-120b",
            ),
            ProviderPreset.COHERE to expectedTemplate(
                "https://api.cohere.ai/compatibility/v1",
                "command-a-plus-05-2026",
            ),
            ProviderPreset.PERPLEXITY to expectedTemplate(
                "https://api.perplexity.ai",
                "sonar-pro",
            ),
            ProviderPreset.HUGGING_FACE to expectedTemplate(
                "https://router.huggingface.co/v1",
                "openai/gpt-oss-120b:fastest",
            ),
        )

        expected.forEach { (preset, template) ->
            assertEquals(template.baseUrl, preset.defaultConfig.baseUrl, preset.name)
            assertEquals(template.model, preset.defaultConfig.defaultModel, preset.name)
            assertEquals(ApiKeyAuth.BEARER, preset.defaultConfig.apiKeyAuth, preset.name)
        }
    }

    @Test
    fun `all built in protocol configurations have complete defaults`() {
        ProviderPreset.entries
            .filterNot { it.isCustom }
            .forEach { preset ->
                preset.protocolConfigs.values.forEach { config ->
                    assertTrue(config.baseUrl.isNotBlank(), preset.name)
                    assertTrue(config.defaultModel.isNotBlank(), preset.name)
                }
            }
    }

    private fun expectedOpenAi(displayName: String): ExpectedPreset = ExpectedPreset(
        displayName = displayName,
        defaultProtocol = ModelProtocol.OPENAI_COMPATIBLE,
        supportedProtocols = setOf(ModelProtocol.OPENAI_COMPATIBLE),
    )

    private fun expectedAnthropic(displayName: String): ExpectedPreset = ExpectedPreset(
        displayName = displayName,
        defaultProtocol = ModelProtocol.ANTHROPIC_COMPATIBLE,
        supportedProtocols = setOf(ModelProtocol.ANTHROPIC_COMPATIBLE),
    )

    private fun expectedBoth(
        displayName: String,
        defaultProtocol: ModelProtocol = ModelProtocol.OPENAI_COMPATIBLE,
    ): ExpectedPreset = ExpectedPreset(
        displayName = displayName,
        defaultProtocol = defaultProtocol,
        supportedProtocols = setOf(
            ModelProtocol.OPENAI_COMPATIBLE,
            ModelProtocol.ANTHROPIC_COMPATIBLE,
        ),
    )

    private fun expectedConfig(
        baseUrl: String,
        apiKeyAuth: ApiKeyAuth,
    ): ExpectedConfig = ExpectedConfig(baseUrl, apiKeyAuth)

    private fun expectedTemplate(
        baseUrl: String,
        model: String,
    ): ExpectedTemplate = ExpectedTemplate(baseUrl, model)

    private data class ExpectedPreset(
        val displayName: String,
        val defaultProtocol: ModelProtocol,
        val supportedProtocols: Set<ModelProtocol>,
    )

    private data class ExpectedConfig(
        val baseUrl: String,
        val apiKeyAuth: ApiKeyAuth,
    )

    private data class ExpectedTemplate(
        val baseUrl: String,
        val model: String,
    )
}
