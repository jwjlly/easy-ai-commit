package com.layxy.easyaicommit.settings

import com.layxy.easyaicommit.model.ModelProfile
import com.layxy.easyaicommit.model.ModelProtocol
import com.layxy.easyaicommit.model.OutputLanguage
import com.layxy.easyaicommit.model.ProviderPreset
import com.layxy.easyaicommit.model.ProxyConfiguration
import com.layxy.easyaicommit.model.ProxyMode
import com.layxy.easyaicommit.model.ProxyType
import com.layxy.easyaicommit.prompt.PromptTemplate
import java.util.UUID

class EasyAiCommitSettingsState {
    var defaultProfileId: String = "openai-default"
    var profiles: MutableList<ModelProfileState> = mutableListOf(ModelProfileState.defaultOpenAi())
    var maxDiffCharacters: Int = 60_000
}

class ModelProfileState {
    var id: String = UUID.randomUUID().toString()
    var name: String = "OpenAI"
    var preset: String = ProviderPreset.OPENAI.name
    var protocol: String = ModelProtocol.OPENAI_COMPATIBLE.name
    var baseUrl: String = ProviderPreset.OPENAI.defaultConfig.baseUrl
    var model: String = ProviderPreset.OPENAI.defaultConfig.defaultModel
    var outputLanguage: String = OutputLanguage.AUTO.id
    var promptTemplate: String = PromptTemplate.DEFAULT
    var proxy: ProxySettingsState = ProxySettingsState()
    var requestTimeoutSeconds: Int = 90

    fun toModel(): ModelProfile = ModelProfile(
        id = id,
        name = name,
        preset = ProviderPreset.fromStored(preset),
        protocol = ModelProtocol.fromStored(protocol),
        baseUrl = baseUrl,
        model = model,
        outputLanguage = OutputLanguage.normalize(outputLanguage),
        promptTemplate = promptTemplate,
        proxy = proxy.toModel(),
        requestTimeoutSeconds = requestTimeoutSeconds,
    )

    fun copyState(): ModelProfileState = ModelProfileState().also {
        it.id = id
        it.name = name
        it.preset = preset
        it.protocol = protocol
        it.baseUrl = baseUrl
        it.model = model
        it.outputLanguage = outputLanguage
        it.promptTemplate = promptTemplate
        it.proxy = proxy.copyState()
        it.requestTimeoutSeconds = requestTimeoutSeconds
    }

    companion object {
        fun defaultOpenAi(): ModelProfileState = ModelProfileState().also {
            it.id = "openai-default"
            it.name = "OpenAI"
            it.preset = ProviderPreset.OPENAI.name
            it.protocol = ProviderPreset.OPENAI.defaultProtocol.name
            it.baseUrl = ProviderPreset.OPENAI.defaultConfig.baseUrl
            it.model = ProviderPreset.OPENAI.defaultConfig.defaultModel
        }

        fun fromPreset(preset: ProviderPreset): ModelProfileState = ModelProfileState().also {
            it.id = UUID.randomUUID().toString()
            it.name = preset.displayName
            it.preset = preset.name
            it.protocol = preset.defaultProtocol.name
            it.baseUrl = preset.defaultConfig.baseUrl
            it.model = preset.defaultConfig.defaultModel
        }
    }
}

class ProxySettingsState {
    var mode: String = ProxyMode.DIRECT.name
    var type: String = ProxyType.HTTP.name
    var host: String = ""
    var port: Int = 7890
    var authentication: Boolean = false
    var username: String = ""

    fun toModel(): ProxyConfiguration = ProxyConfiguration(
        mode = ProxyMode.fromStored(mode),
        type = ProxyType.entries.firstOrNull { it.name == type } ?: ProxyType.HTTP,
        host = host,
        port = port,
        authentication = authentication,
        username = username,
    )

    fun copyState(): ProxySettingsState = fromModel(toModel())

    companion object {
        fun fromModel(proxy: ProxyConfiguration): ProxySettingsState = ProxySettingsState().also {
            it.mode = proxy.mode.name
            it.type = proxy.type.name
            it.host = proxy.host
            it.port = proxy.port
            it.authentication = proxy.authentication
            it.username = proxy.username
        }
    }
}

data class SettingsSnapshot(
    val profiles: List<ModelProfile>,
    val defaultProfileId: String,
    val maxDiffCharacters: Int,
) {
    val defaultProfile: ModelProfile?
        get() = profiles.firstOrNull { it.id == defaultProfileId } ?: profiles.firstOrNull()
}
