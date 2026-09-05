package com.layxy.easyaicommit.settings

import com.layxy.easyaicommit.model.ModelProtocol
import com.layxy.easyaicommit.model.OutputLanguage
import com.layxy.easyaicommit.model.ProviderPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.layxy.easyaicommit.model.ProxyMode
import com.layxy.easyaicommit.model.ProxyType
import com.layxy.easyaicommit.model.ProxyConfiguration
import com.intellij.util.xmlb.XmlSerializer

class EasyAiCommitSettingsServiceTest {
    @Test
    fun `existing profiles default to IDE proxy and ninety second deadline`() {
        val service = EasyAiCommitSettingsService()
        service.loadState(stateWith(ProviderPreset.OPENAI, ModelProtocol.OPENAI_COMPATIBLE))
        val profile = service.snapshot().defaultProfile!!
        assertEquals(ProxyMode.IDE, profile.proxy.mode)
        assertEquals(90, profile.requestTimeoutSeconds)
    }

    @Test
    fun `proxy settings survive persistence and snapshots are independent`() {
        val original = stateWith(ProviderPreset.CUSTOM, ModelProtocol.ANTHROPIC_COMPATIBLE)
        original.profiles.single().apply {
            requestTimeoutSeconds = 30
            proxy = ProxySettingsState.fromModel(ProxyConfiguration(ProxyMode.CUSTOM, ProxyType.HTTP, "localhost", 8080, true, "alice"))
        }
        val restored = XmlSerializer.deserialize(XmlSerializer.serialize(original), EasyAiCommitSettingsState::class.java)
        val service = EasyAiCommitSettingsService()
        service.loadState(restored)
        val snapshot = service.snapshot()
        assertEquals(original.profiles.single().toModel(), snapshot.profiles.single())
        restored.profiles.single().proxy.host = "changed"
        assertEquals("localhost", snapshot.profiles.single().proxy.host)
        assertEquals(30, snapshot.profiles.single().requestTimeoutSeconds)
    }

    @Test
    fun `proxy input rejects URLs paths invalid ports and SOCKS authentication`() {
        listOf("", "http://localhost", "localhost/path", "user@localhost", "localhost:7890", "host name").forEach { host ->
            assertFailsWith<IllegalArgumentException> { ProxyConfiguration(ProxyMode.CUSTOM, host = host).validate() }
        }
        listOf("localhost", "127.0.0.1", "::1", "[::1]").forEach { host ->
            ProxyConfiguration(ProxyMode.CUSTOM, host = host).validate()
        }
        assertFailsWith<IllegalArgumentException> { ProxyConfiguration(ProxyMode.CUSTOM, host = "localhost", port = 0).validate() }
        assertFailsWith<IllegalArgumentException> { ProxyConfiguration(ProxyMode.CUSTOM, ProxyType.SOCKS5, "localhost", authentication = true).validate() }
    }

    @Test
    fun `new profiles default to automatic output language`() {
        assertEquals(OutputLanguage.AUTO.id, ModelProfileState.defaultOpenAi().outputLanguage)
        assertEquals(
            OutputLanguage.AUTO.id,
            ModelProfileState.fromPreset(ProviderPreset.DEEPSEEK).outputLanguage,
        )
    }

    @Test
    fun `normalizes stored output language ids`() {
        val state = stateWith(ProviderPreset.OPENAI, ModelProtocol.OPENAI_COMPATIBLE)
        state.profiles.single().outputLanguage = " ZH-cn "
        val service = EasyAiCommitSettingsService()

        service.loadState(state)

        assertEquals(
            OutputLanguage.SIMPLIFIED_CHINESE.id,
            service.snapshot().profiles.single().outputLanguage,
        )
    }

    @Test
    fun `normalizes a protocol unsupported by the stored preset`() {
        val service = EasyAiCommitSettingsService()
        service.loadState(stateWith(ProviderPreset.OPENAI, ModelProtocol.ANTHROPIC_COMPATIBLE))

        assertEquals(
            ModelProtocol.OPENAI_COMPATIBLE,
            service.snapshot().profiles.single().protocol,
        )
    }

    @Test
    fun `preserves anthropic profiles for every dual protocol preset`() {
        val dualProtocolPresets = ProviderPreset.entries.filter {
            it.supports(ModelProtocol.OPENAI_COMPATIBLE) &&
                it.supports(ModelProtocol.ANTHROPIC_COMPATIBLE)
        }

        dualProtocolPresets.forEach { preset ->
            val service = EasyAiCommitSettingsService()
            service.loadState(stateWith(preset, ModelProtocol.ANTHROPIC_COMPATIBLE))

            val profile = service.snapshot().profiles.single()
            assertEquals(ModelProtocol.ANTHROPIC_COMPATIBLE, profile.protocol, preset.name)
            assertEquals(
                preset.configFor(ModelProtocol.ANTHROPIC_COMPATIBLE).baseUrl,
                profile.baseUrl,
                preset.name,
            )
        }
    }

    private fun stateWith(
        preset: ProviderPreset,
        protocol: ModelProtocol,
    ): EasyAiCommitSettingsState {
        val config = preset.protocolConfigs[protocol] ?: preset.defaultConfig
        return EasyAiCommitSettingsState().also { state ->
            state.defaultProfileId = "test-profile"
            state.profiles = mutableListOf(
                ModelProfileState().also { profile ->
                    profile.id = "test-profile"
                    profile.name = "Test Profile"
                    profile.preset = preset.name
                    profile.protocol = protocol.name
                    profile.baseUrl = config.baseUrl
                    profile.model = config.defaultModel
                },
            )
        }
    }
}
