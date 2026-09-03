package com.layxy.easyaicommit.client

import com.layxy.easyaicommit.EasyAiCommitBundle.message
import com.layxy.easyaicommit.model.ApiKeyAuth
import com.layxy.easyaicommit.model.ModelProfile
import com.layxy.easyaicommit.model.ModelProtocol
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.io.HttpRequests
import java.net.HttpURLConnection
import java.util.Properties

object CompatibleModelClient {
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 90_000
    private const val MAX_RESPONSE_CHARACTERS = 100_000
    private val gson = Gson()
    private val pluginVersion: String by lazy {
        val properties = Properties()
        CompatibleModelClient::class.java
            .getResourceAsStream("/easy-ai-commit-version.properties")
            ?.use(properties::load)
        properties.getProperty("version")?.takeIf(String::isNotBlank) ?: "dev"
    }

    fun generate(
        profile: ModelProfile,
        apiKey: String,
        prompt: String,
        indicator: ProgressIndicator? = null,
    ): String {
        require(profile.baseUrl.isNotBlank()) { message("client.error.baseUrlRequired") }
        require(profile.model.isNotBlank()) { message("client.error.modelRequired") }
        require(apiKey.isNotBlank()) { message("client.error.apiKeyRequired") }
        if (!profile.preset.supports(profile.protocol)) {
            throw ModelRequestException(
                message(
                    "client.error.protocolUnsupported",
                    profile.preset.displayName,
                    profile.protocol.displayName,
                ),
            )
        }

        val response = when (profile.protocol) {
            ModelProtocol.OPENAI_COMPATIBLE -> requestOpenAi(profile, apiKey, prompt, indicator)
            ModelProtocol.ANTHROPIC_COMPATIBLE -> requestAnthropic(profile, apiKey, prompt, indicator)
        }

        return cleanGeneratedMessage(response).ifBlank {
            throw ModelRequestException(message("client.error.emptyMessage"))
        }
    }

    fun endpoint(baseUrl: String, protocol: ModelProtocol): String {
        val normalized = baseUrl.trim().trimEnd('/')
        if (normalized.isEmpty()) return normalized

        return when (protocol) {
            ModelProtocol.OPENAI_COMPATIBLE ->
                if (normalized.endsWith("/chat/completions")) normalized else "$normalized/chat/completions"
            ModelProtocol.ANTHROPIC_COMPATIBLE ->
                if (normalized.endsWith("/messages")) normalized else "$normalized/messages"
        }
    }

    internal fun parseOpenAiResponse(json: String): String {
        val root = parseObject(json)
        val content = root.getAsJsonArray("choices")
            ?.firstOrNull()
            ?.asJsonObject
            ?.getAsJsonObject("message")
            ?.get("content")
            ?: throw ModelRequestException(
                errorMessage(root) ?: message("client.error.openaiMissingMessage"),
            )

        return extractText(content)
    }

    internal fun parseAnthropicResponse(json: String): String {
        val root = parseObject(json)
        val content = root.getAsJsonArray("content")
            ?: throw ModelRequestException(
                errorMessage(root) ?: message("client.error.anthropicMissingContent"),
            )

        return content
            .mapNotNull { element ->
                element.takeIf { it.isJsonObject }
                    ?.asJsonObject
                    ?.takeIf { it.get("type")?.asString == "text" }
                    ?.get("text")
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asString
            }
            .joinToString("\n")
    }

    internal fun cleanGeneratedMessage(value: String): String {
        var result = value.trim()
        if (result.startsWith("```") && result.endsWith("```")) {
            result = result.lineSequence().toList().drop(1).dropLast(1).joinToString("\n").trim()
        }
        if (result.length >= 2 && result.first() == '"' && result.last() == '"') {
            result = result.substring(1, result.length - 1).trim()
        }
        return result
    }

    internal fun apiKeyHeaders(profile: ModelProfile, apiKey: String): Map<String, String> =
        when (profile.preset.configFor(profile.protocol).apiKeyAuth) {
            ApiKeyAuth.BEARER -> mapOf("Authorization" to "Bearer $apiKey")
            ApiKeyAuth.X_API_KEY -> mapOf("x-api-key" to apiKey)
        }

    private fun requestOpenAi(
        profile: ModelProfile,
        apiKey: String,
        prompt: String,
        indicator: ProgressIndicator?,
    ): String {
        val body = gson.toJson(
            mapOf(
                "model" to profile.model,
                "messages" to listOf(
                    mapOf(
                        "role" to "system",
                        "content" to "Write a concise Git commit message. Return only the commit message.",
                    ),
                    mapOf("role" to "user", "content" to prompt),
                ),
                "temperature" to 0.2,
                "max_tokens" to 512,
                "stream" to false,
            ),
        )

        val response = postJson(
            url = endpoint(profile.baseUrl, profile.protocol),
            body = body,
            headers = buildMap {
                putAll(apiKeyHeaders(profile, apiKey))
                if (profile.preset.name == "OPENROUTER") put("X-Title", "Easy AI Commit")
                if (profile.preset.name == "GEMINI") {
                    put("x-goog-api-client", "easy-ai-commit-oai/$pluginVersion")
                }
            },
            indicator = indicator,
        )
        return parseOpenAiResponse(response)
    }

    private fun requestAnthropic(
        profile: ModelProfile,
        apiKey: String,
        prompt: String,
        indicator: ProgressIndicator?,
    ): String {
        val body = gson.toJson(
            mapOf(
                "model" to profile.model,
                "system" to "Write a concise Git commit message. Return only the commit message.",
                "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
                "temperature" to 0.2,
                "max_tokens" to 512,
                "stream" to false,
            ),
        )

        val response = postJson(
            url = endpoint(profile.baseUrl, profile.protocol),
            body = body,
            headers = buildMap {
                putAll(apiKeyHeaders(profile, apiKey))
                put("anthropic-version", "2023-06-01")
            },
            indicator = indicator,
        )
        return parseAnthropicResponse(response)
    }

    private fun postJson(
        url: String,
        body: String,
        headers: Map<String, String>,
        indicator: ProgressIndicator?,
    ): String {
        try {
            return HttpRequests.post(url, HttpRequests.JSON_CONTENT_TYPE)
                .connectTimeout(CONNECT_TIMEOUT_MS)
                .readTimeout(READ_TIMEOUT_MS)
                .gzip(true)
                .useProxy(true)
                .userAgent("EasyAICommit/$pluginVersion")
                .accept(HttpRequests.JSON_CONTENT_TYPE)
                .isReadResponseOnError(true)
                .throwStatusCodeException(false)
                .tuner { connection ->
                    headers.forEach(connection::setRequestProperty)
                }
                .connect { request ->
                    indicator?.checkCanceled()
                    request.write(body)
                    val connection = request.connection as HttpURLConnection
                    val status = connection.responseCode
                    val responseBody = if (status in 200..299) {
                        request.readString(indicator)
                    } else {
                        request.readError()
                    }.orEmpty().take(MAX_RESPONSE_CHARACTERS)

                    if (status !in 200..299) {
                        val remoteMessage = runCatching { errorMessage(parseObject(responseBody)) }.getOrNull()
                        val details = remoteMessage?.let { ": $it" }.orEmpty()
                        throw ModelRequestException(
                            message("client.error.requestHttp", status, details),
                        )
                    }
                    responseBody
                }
        } catch (error: ModelRequestException) {
            throw error
        } catch (error: Exception) {
            throw ModelRequestException(
                error.message ?: message("client.error.requestFailed"),
                error,
            )
        }
    }

    private fun parseObject(json: String): JsonObject = try {
        JsonParser.parseString(json).asJsonObject
    } catch (error: Exception) {
        throw ModelRequestException(message("client.error.invalidJson"), error)
    }

    private fun errorMessage(root: JsonObject): String? {
        val error = root.get("error") ?: return null
        return when {
            error.isJsonPrimitive -> error.asString
            error.isJsonObject -> error.asJsonObject.get("message")?.takeIf { it.isJsonPrimitive }?.asString
            else -> null
        }
    }

    private fun extractText(content: JsonElement): String = when {
        content.isJsonPrimitive -> content.asString
        content.isJsonArray -> content.asJsonArray.mapNotNull { item ->
            when {
                item.isJsonPrimitive -> item.asString
                item.isJsonObject -> item.asJsonObject.get("text")?.takeIf { it.isJsonPrimitive }?.asString
                else -> null
            }
        }.joinToString("\n")
        else -> ""
    }
}
