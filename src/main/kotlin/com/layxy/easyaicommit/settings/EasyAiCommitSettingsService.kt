package com.layxy.easyaicommit.settings

import com.layxy.easyaicommit.model.ModelProtocol
import com.layxy.easyaicommit.model.OutputLanguage
import com.layxy.easyaicommit.model.ProviderPreset
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "EasyAiCommitSettings", storages = [Storage("easy-ai-commit.xml")])
class EasyAiCommitSettingsService : PersistentStateComponent<EasyAiCommitSettingsState> {
    private var settingsState = EasyAiCommitSettingsState()

    override fun getState(): EasyAiCommitSettingsState = settingsState

    override fun loadState(state: EasyAiCommitSettingsState) {
        settingsState = state
        normalize()
    }

    @Synchronized
    fun snapshot(): SettingsSnapshot {
        normalize()
        return SettingsSnapshot(
            profiles = settingsState.profiles.map { it.copyState().toModel() },
            defaultProfileId = settingsState.defaultProfileId,
            maxDiffCharacters = settingsState.maxDiffCharacters,
        )
    }

    @Synchronized
    fun replace(
        profiles: List<ModelProfileState>,
        defaultProfileId: String,
        maxDiffCharacters: Int,
    ) {
        settingsState.profiles = profiles.map { it.copyState() }.toMutableList()
        settingsState.defaultProfileId = defaultProfileId
        settingsState.maxDiffCharacters = maxDiffCharacters.coerceIn(5_000, 200_000)
        normalize()
    }

    @Synchronized
    private fun normalize() {
        if (settingsState.profiles.isEmpty()) {
            settingsState.profiles.add(ModelProfileState.defaultOpenAi())
        }
        settingsState.profiles.forEach { profile ->
            val preset = ProviderPreset.fromStored(profile.preset)
            profile.preset = preset.name
            val storedProtocol = ModelProtocol.fromStored(profile.protocol)
            if (!preset.supports(storedProtocol)) {
                profile.protocol = preset.defaultProtocol.name
            }
            profile.outputLanguage = OutputLanguage.normalize(profile.outputLanguage)
            profile.requestTimeoutSeconds = profile.requestTimeoutSeconds.coerceIn(5, 300)
        }
        if (settingsState.profiles.none { it.id == settingsState.defaultProfileId }) {
            settingsState.defaultProfileId = settingsState.profiles.first().id
        }
        settingsState.maxDiffCharacters = settingsState.maxDiffCharacters.coerceIn(5_000, 200_000)
    }

    companion object {
        fun getInstance(): EasyAiCommitSettingsService =
            ApplicationManager.getApplication().getService(EasyAiCommitSettingsService::class.java)
    }
}
