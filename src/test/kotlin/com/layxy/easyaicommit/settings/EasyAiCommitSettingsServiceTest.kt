package com.layxy.easyaicommit.settings

import com.layxy.easyaicommit.model.ModelProtocol
import com.layxy.easyaicommit.model.OutputLanguage
import com.layxy.easyaicommit.model.ProviderPreset
import kotlin.test.Test
import kotlin.test.assertEquals

class EasyAiCommitSettingsServiceTest {
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
