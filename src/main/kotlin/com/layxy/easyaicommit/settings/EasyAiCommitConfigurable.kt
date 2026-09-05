package com.layxy.easyaicommit.settings

import com.layxy.easyaicommit.EasyAiCommitBundle.message
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

class EasyAiCommitConfigurable : SearchableConfigurable {
    private var component: EasyAiCommitSettingsComponent? = null

    override fun getId(): String = "com.layxy.easyaicommit.settings"

    override fun getDisplayName(): String = message("settings.display.name")

    override fun createComponent(): JComponent {
        return EasyAiCommitSettingsComponent().also {
            component = it
            it.reset(EasyAiCommitSettingsService.getInstance().snapshot())
        }.panel
    }

    override fun isModified(): Boolean =
        component?.isModified(EasyAiCommitSettingsService.getInstance().snapshot()) == true

    @Throws(ConfigurationException::class)
    override fun apply() {
        component?.apply(EasyAiCommitSettingsService.getInstance())
    }

    override fun reset() {
        component?.reset(EasyAiCommitSettingsService.getInstance().snapshot())
    }

    override fun disposeUIResources() {
        component?.dispose()
        component = null
    }
}
