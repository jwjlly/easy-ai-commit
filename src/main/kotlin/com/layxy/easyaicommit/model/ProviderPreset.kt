package com.layxy.easyaicommit.model

import com.layxy.easyaicommit.EasyAiCommitBundle

enum class ApiKeyAuth {
    BEARER,
    X_API_KEY,
}

data class ProviderProtocolConfig(
    val baseUrl: String,
    val defaultModel: String,
    val apiKeyAuth: ApiKeyAuth,
)

private fun openAiConfig(
    baseUrl: String,
    defaultModel: String,
): Pair<ModelProtocol, ProviderProtocolConfig> =
    ModelProtocol.OPENAI_COMPATIBLE to ProviderProtocolConfig(
        baseUrl = baseUrl,
        defaultModel = defaultModel,
        apiKeyAuth = ApiKeyAuth.BEARER,
    )

private fun anthropicConfig(
    baseUrl: String,
    defaultModel: String,
    apiKeyAuth: ApiKeyAuth,
): Pair<ModelProtocol, ProviderProtocolConfig> =
    ModelProtocol.ANTHROPIC_COMPATIBLE to ProviderProtocolConfig(
        baseUrl = baseUrl,
        defaultModel = defaultModel,
        apiKeyAuth = apiKeyAuth,
    )

private fun protocolConfigs(
    vararg configs: Pair<ModelProtocol, ProviderProtocolConfig>,
): Map<ModelProtocol, ProviderProtocolConfig> = linkedMapOf(*configs)

enum class ProviderPreset(
    private val fallbackDisplayName: String,
    val defaultProtocol: ModelProtocol,
    val protocolConfigs: Map<ModelProtocol, ProviderProtocolConfig>,
) {
    CUSTOM(
        "Custom",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("", ""),
            anthropicConfig("", "", ApiKeyAuth.X_API_KEY),
        ),
    ),
    OPENAI(
        "OpenAI",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://api.openai.com/v1", "gpt-4.1-mini"),
        ),
    ),
    ANTHROPIC(
        "Anthropic",
        ModelProtocol.ANTHROPIC_COMPATIBLE,
        protocolConfigs(
            anthropicConfig(
                "https://api.anthropic.com/v1",
                "claude-sonnet-5",
                ApiKeyAuth.X_API_KEY,
            ),
        ),
    ),
    DEEPSEEK(
        "DeepSeek",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://api.deepseek.com", "deepseek-v4-flash"),
            anthropicConfig(
                "https://api.deepseek.com/anthropic/v1",
                "deepseek-v4-flash",
                ApiKeyAuth.X_API_KEY,
            ),
        ),
    ),
    OPENROUTER(
        "OpenRouter",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://openrouter.ai/api/v1", "~openai/gpt-latest"),
            anthropicConfig(
                "https://openrouter.ai/api/v1",
                "~openai/gpt-latest",
                ApiKeyAuth.BEARER,
            ),
        ),
    ),
    GEMINI(
        "Gemini",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig(
                "https://generativelanguage.googleapis.com/v1beta/openai",
                "gemini-3.7-flash",
            ),
        ),
    ),
    GROK(
        "Grok",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://api.x.ai/v1", "grok-4"),
        ),
    ),
    GLM(
        "GLM",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://open.bigmodel.cn/api/paas/v4", "glm-5.2"),
            anthropicConfig(
                "https://open.bigmodel.cn/api/anthropic/v1",
                "glm-5.2",
                ApiKeyAuth.X_API_KEY,
            ),
        ),
    ),
    ALIBABA_BAILIAN(
        "Alibaba Bailian",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig(
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "qwen-plus",
            ),
            anthropicConfig(
                "https://dashscope.aliyuncs.com/apps/anthropic/v1",
                "qwen-plus",
                ApiKeyAuth.X_API_KEY,
            ),
        ),
    ),
    KIMI(
        "Kimi",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://api.moonshot.cn/v1", "kimi-k3"),
            anthropicConfig(
                "https://api.moonshot.cn/anthropic/v1",
                "kimi-k3",
                ApiKeyAuth.BEARER,
            ),
        ),
    ),
    MINIMAX(
        "MiniMax",
        ModelProtocol.ANTHROPIC_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://api.minimaxi.com/v1", "MiniMax-M2.7"),
            anthropicConfig(
                "https://api.minimaxi.com/anthropic/v1",
                "MiniMax-M2.7",
                ApiKeyAuth.X_API_KEY,
            ),
        ),
    ),
    SILICONFLOW(
        "SiliconFlow",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig(
                "https://api.siliconflow.cn/v1",
                "deepseek-ai/DeepSeek-V4-Flash",
            ),
            anthropicConfig(
                "https://api.siliconflow.cn/v1",
                "deepseek-ai/DeepSeek-V4-Flash",
                ApiKeyAuth.BEARER,
            ),
        ),
    ),
    STEPFUN(
        "StepFun",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://api.stepfun.com/v1", "step-3.5-flash"),
            anthropicConfig(
                "https://api.stepfun.com/v1",
                "step-3.5-flash",
                ApiKeyAuth.BEARER,
            ),
        ),
    ),
    TENCENT_TOKENHUB(
        "Tencent TokenHub",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://tokenhub.tencentmaas.com/v1", "hy3"),
            anthropicConfig(
                "https://tokenhub.tencentmaas.com/v1",
                "hy3",
                ApiKeyAuth.X_API_KEY,
            ),
        ),
    ),
    VOLCANO_ARK(
        "Volcano Ark",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig(
                "https://ark.cn-beijing.volces.com/api/v3",
                "doubao-seed-2-0-lite-260215",
            ),
        ),
    ),
    BAIDU_QIANFAN(
        "Baidu Qianfan",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig(
                "https://qianfan.baidubce.com/v2",
                "ernie-4.5-turbo-20260402",
            ),
        ),
    ),
    XFYUN_SPARK(
        "iFlytek Spark",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig(
                "https://spark-api-open.xf-yun.com/v1",
                "generalv3.5",
            ),
        ),
    ),
    MISTRAL(
        "Mistral AI",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://api.mistral.ai/v1", "mistral-small-latest"),
        ),
    ),
    GROQ(
        "Groq",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://api.groq.com/openai/v1", "openai/gpt-oss-120b"),
        ),
    ),
    TOGETHER(
        "Together AI",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://api.together.xyz/v1", "openai/gpt-oss-120b"),
        ),
    ),
    FIREWORKS(
        "Fireworks AI",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig(
                "https://api.fireworks.ai/inference/v1",
                "accounts/fireworks/models/deepseek-v3p1",
            ),
        ),
    ),
    CEREBRAS(
        "Cerebras",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://api.cerebras.ai/v1", "gpt-oss-120b"),
        ),
    ),
    COHERE(
        "Cohere",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig(
                "https://api.cohere.ai/compatibility/v1",
                "command-a-plus-05-2026",
            ),
        ),
    ),
    PERPLEXITY(
        "Perplexity",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig("https://api.perplexity.ai", "sonar-pro"),
        ),
    ),
    HUGGING_FACE(
        "Hugging Face",
        ModelProtocol.OPENAI_COMPATIBLE,
        protocolConfigs(
            openAiConfig(
                "https://router.huggingface.co/v1",
                "openai/gpt-oss-120b:fastest",
            ),
        ),
    );

    val displayName: String
        get() = when (this) {
            CUSTOM -> EasyAiCommitBundle.message("provider.custom")
            else -> fallbackDisplayName
        }

    val isCustom: Boolean
        get() = this == CUSTOM

    val supportedProtocols: Set<ModelProtocol>
        get() = protocolConfigs.keys

    val defaultConfig: ProviderProtocolConfig
        get() = configFor(defaultProtocol)

    init {
        require(protocolConfigs.isNotEmpty()) {
            "$displayName must configure at least one protocol"
        }
        require(defaultProtocol in protocolConfigs) {
            "$displayName must configure its default protocol"
        }
    }

    fun supports(protocol: ModelProtocol): Boolean = protocol in supportedProtocols

    fun configFor(protocol: ModelProtocol): ProviderProtocolConfig =
        requireNotNull(protocolConfigs[protocol]) {
            "$displayName does not support ${protocol.displayName}"
        }

    override fun toString(): String = displayName

    companion object {
        fun fromStored(value: String): ProviderPreset =
            entries.firstOrNull { it.name == value } ?: CUSTOM
    }
}
