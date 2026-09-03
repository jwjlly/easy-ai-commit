package com.layxy.easyaicommit.settings

import com.layxy.easyaicommit.EasyAiCommitBundle.message
import com.layxy.easyaicommit.client.CompatibleModelClient
import com.layxy.easyaicommit.model.ModelProtocol
import com.layxy.easyaicommit.model.OutputLanguage
import com.layxy.easyaicommit.model.ProviderPreset
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ActionListener
import java.util.UUID
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JSpinner
import javax.swing.JSplitPane
import javax.swing.SpinnerNumberModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

internal class EasyAiCommitSettingsComponent {
    private val profilesModel = DefaultListModel<ModelProfileState>()
    private val profilesList = JBList(profilesModel)
    private val profileName = JBTextField()
    private val providerPreset = ComboBox(ProviderPreset.entries.toTypedArray()).withWidth(300)
    private val protocol = ComboBox(ModelProtocol.entries.toTypedArray()).withWidth(260)
    private val baseUrl = JBTextField()
    private val model = JBTextField()
    private val apiKey = JBPasswordField()
    private val clearApiKey = JButton(message("settings.button.clear"))
    private val testConnection = JButton(message("settings.button.testConnection"))
    private val connectionStatus = JBLabel()
    private val outputLanguage = ComboBox(OutputLanguage.options).withWidth(260)
    private val promptTemplate = JBTextArea(10, 48)
    private val maxDiffCharacters = JSpinner(SpinnerNumberModel(60_000, 5_000, 200_000, 5_000))
    private val setDefault = JButton(message("settings.button.setDefault"))

    private var defaultProfileId: String = ""
    private var currentProfileId: String? = null
    private var loadingForm = false
    private val secretEdits = mutableMapOf<String, String>()
    private val removedProfileIds = mutableSetOf<String>()

    val panel: JComponent = buildPanel()

    init {
        installListeners()
    }

    fun reset(snapshot: SettingsSnapshot) {
        loadingForm = true
        profilesModel.clear()
        snapshot.profiles.forEach { profile ->
            profilesModel.addElement(ModelProfileState().also {
                it.id = profile.id
                it.name = profile.name
                it.preset = profile.preset.name
                it.protocol = profile.protocol.name
                it.baseUrl = profile.baseUrl
                it.model = profile.model
                it.outputLanguage = profile.outputLanguage
                it.promptTemplate = profile.promptTemplate
            })
        }
        defaultProfileId = snapshot.defaultProfileId
        maxDiffCharacters.value = snapshot.maxDiffCharacters
        secretEdits.clear()
        removedProfileIds.clear()
        loadingForm = false

        if (!profilesModel.isEmpty) profilesList.selectedIndex = 0
        refreshDefaultButton()
    }

    fun isModified(snapshot: SettingsSnapshot): Boolean {
        val currentProfiles = (0 until profilesModel.size).map { profilesModel.getElementAt(it).toModel() }
        return currentProfiles != snapshot.profiles ||
            defaultProfileId != snapshot.defaultProfileId ||
            (maxDiffCharacters.value as Int) != snapshot.maxDiffCharacters ||
            secretEdits.isNotEmpty() ||
            removedProfileIds.isNotEmpty()
    }

    @Throws(ConfigurationException::class)
    fun apply(service: EasyAiCommitSettingsService) {
        val profiles = (0 until profilesModel.size).map { profilesModel.getElementAt(it).copyState() }
        profiles.forEach { profile ->
            if (profile.name.isBlank()) {
                throw ConfigurationException(message("settings.validation.profileName.empty"))
            }
            if (profile.baseUrl.isBlank()) {
                throw ConfigurationException(message("settings.validation.baseUrl.empty", profile.name))
            }
            if (profile.model.isBlank()) {
                throw ConfigurationException(message("settings.validation.model.empty", profile.name))
            }
            val preset = ProviderPreset.fromStored(profile.preset)
            val selectedProtocol = ModelProtocol.fromStored(profile.protocol)
            if (!preset.supports(selectedProtocol)) {
                throw ConfigurationException(
                    message(
                        "settings.validation.protocol.unsupported",
                        preset.displayName,
                        selectedProtocol.displayName,
                    ),
                )
            }
        }

        service.replace(
            profiles = profiles,
            defaultProfileId = defaultProfileId,
            maxDiffCharacters = maxDiffCharacters.value as Int,
        )

        val keysToSave = secretEdits.toMap()
        val keysToRemove = removedProfileIds.toSet()
        if (keysToSave.isNotEmpty() || keysToRemove.isNotEmpty()) {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    keysToRemove.forEach(ApiKeyStore::remove)
                    keysToSave.forEach { (profileId, apiKey) -> ApiKeyStore.save(profileId, apiKey) }
                },
                message("settings.progress.savingCredentials"),
                false,
                null,
            )
        }
        secretEdits.clear()
        removedProfileIds.clear()
    }

    private fun buildPanel(): JComponent {
        profilesList.cellRenderer = object : ColoredListCellRenderer<ModelProfileState>() {
            override fun customizeCellRenderer(
                list: JList<out ModelProfileState>,
                value: ModelProfileState?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean,
            ) {
                if (value == null) return
                append(
                    value.name.ifBlank { message("settings.profile.unnamed") },
                    SimpleTextAttributes.REGULAR_ATTRIBUTES,
                )
                val suffix = buildString {
                    append(ModelProtocol.fromStored(value.protocol).displayName)
                    if (value.id == defaultProfileId) {
                        append(" · ")
                        append(message("settings.profile.defaultSuffix"))
                    }
                }
                append("  $suffix", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
        }
        profilesList.emptyText.text = message("settings.profiles.empty")
        profilesList.preferredSize = Dimension(230, 420)

        val listPanel = ToolbarDecorator.createDecorator(profilesList)
            .setAddAction { addProfile() }
            .setRemoveAction { removeSelectedProfile() }
            .setMoveUpAction { moveSelectedProfile(-1) }
            .setMoveDownAction { moveSelectedProfile(1) }
            .createPanel()

        val left = JPanel(BorderLayout(JBUI.scale(0), JBUI.scale(8))).apply {
            border = JBUI.Borders.empty(8, 0, 0, 8)
            add(
                JBLabel(message("settings.section.modelProfiles")).apply {
                    font = font.deriveFont(Font.BOLD)
                },
                BorderLayout.NORTH,
            )
            add(listPanel, BorderLayout.CENTER)
            add(setDefault, BorderLayout.SOUTH)
        }

        promptTemplate.lineWrap = true
        promptTemplate.wrapStyleWord = true
        apiKey.emptyText.text = message("settings.apiKey.placeholder")

        val apiKeyPanel = JPanel(BorderLayout(JBUI.scale(6), 0)).apply {
            add(apiKey, BorderLayout.CENTER)
            add(clearApiKey, BorderLayout.EAST)
        }
        val connectionPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            add(testConnection)
            add(JBLabel("  "))
            add(connectionStatus)
        }

        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent(message("settings.label.profileName"), profileName)
            .addLabeledComponent(message("settings.label.providerPreset"), providerPreset)
            .addLabeledComponent(message("settings.label.protocol"), protocol)
            .addLabeledComponent(message("settings.label.baseUrl"), baseUrl)
            .addLabeledComponent(message("settings.label.model"), model)
            .addLabeledComponent(message("settings.label.apiKey"), apiKeyPanel)
            .addLabeledComponent(message("settings.label.connection"), connectionPanel)
            .addLabeledComponent(message("settings.label.outputLanguage"), outputLanguage)
            .addLabeledComponent(message("settings.label.maximumChangeSize"), maxDiffCharacters)
            .addLabeledComponentFillVertically(
                message("settings.label.promptTemplate"),
                JBScrollPane(promptTemplate),
            )
            .panel

        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(10)
            add(
                JBLabel(
                    "<html><b>${message("settings.header.location")}</b><br>" +
                        "${message("settings.header.description")}</html>",
                ),
            )
        }
        val right = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 8, 0, 12)
            add(header, BorderLayout.NORTH)
            add(JBScrollPane(form).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
        }

        return JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right).apply {
            border = JBUI.Borders.empty()
            dividerLocation = JBUI.scale(245)
            resizeWeight = 0.0
        }
    }

    private fun installListeners() {
        profilesList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) loadSelectedProfile()
        }

        profileName.onTextChanged { updateCurrentProfile { it.name = profileName.text } }
        baseUrl.onTextChanged { updateCurrentProfile { it.baseUrl = baseUrl.text } }
        model.onTextChanged { updateCurrentProfile { it.model = model.text } }
        promptTemplate.onTextChanged { updateCurrentProfile { it.promptTemplate = promptTemplate.text } }
        apiKey.onTextChanged {
            if (!loadingForm) currentProfileId?.let { secretEdits[it] = String(apiKey.password) }
        }

        providerPreset.addActionListener {
            if (loadingForm) return@addActionListener
            val selected = providerPreset.selectedItem as? ProviderPreset ?: return@addActionListener
            val config = selected.defaultConfig
            updateCurrentProfile { profile ->
                profile.preset = selected.name
                profile.protocol = selected.defaultProtocol.name
                profile.baseUrl = config.baseUrl
                profile.model = config.defaultModel
            }
            loadSelectedProfile()
        }
        protocol.addActionListener {
            if (loadingForm) return@addActionListener
            val selectedPreset = providerPreset.selectedItem as? ProviderPreset ?: return@addActionListener
            val selectedProtocol = protocol.selectedItem as? ModelProtocol ?: return@addActionListener
            if (!selectedPreset.supports(selectedProtocol)) return@addActionListener
            val config = selectedPreset.configFor(selectedProtocol)
            updateCurrentProfile { profile ->
                profile.protocol = selectedProtocol.name
                if (!selectedPreset.isCustom) {
                    profile.baseUrl = config.baseUrl
                    profile.model = config.defaultModel
                }
            }
            loadSelectedProfile()
        }
        outputLanguage.addActionListener {
            if (!loadingForm) {
                val selected = outputLanguage.selectedItem as? OutputLanguage ?: return@addActionListener
                updateCurrentProfile { it.outputLanguage = selected.id }
            }
        }

        clearApiKey.addActionListener {
            currentProfileId?.let {
                secretEdits[it] = ""
                loadingForm = true
                apiKey.text = ""
                loadingForm = false
            }
        }
        setDefault.addActionListener {
            val selected = profilesList.selectedValue ?: return@addActionListener
            defaultProfileId = selected.id
            profilesList.repaint()
            refreshDefaultButton()
        }
        testConnection.addActionListener(ActionListener { testCurrentConnection() })
    }

    private fun loadSelectedProfile() {
        val profile = profilesList.selectedValue
        currentProfileId = profile?.id
        loadingForm = true
        setFormEnabled(profile != null)
        if (profile != null) {
            profileName.text = profile.name
            val selectedPreset = ProviderPreset.fromStored(profile.preset)
            val storedProtocol = ModelProtocol.fromStored(profile.protocol)
            val selectedProtocol = storedProtocol.takeIf(selectedPreset::supports)
                ?: selectedPreset.defaultProtocol
            profile.protocol = selectedProtocol.name
            providerPreset.selectedItem = selectedPreset
            configureProtocolSelector(selectedPreset, selectedProtocol)
            baseUrl.text = profile.baseUrl
            model.text = profile.model
            outputLanguage.selectedItem = OutputLanguage.fromStored(profile.outputLanguage)
            promptTemplate.text = profile.promptTemplate
            apiKey.text = secretEdits[profile.id].orEmpty()
        }
        connectionStatus.text = ""
        loadingForm = false
        refreshDefaultButton()
    }

    private fun configureProtocolSelector(
        preset: ProviderPreset,
        selectedProtocol: ModelProtocol,
    ) {
        val options = ModelProtocol.entries.filter(preset::supports)
        protocol.model = DefaultComboBoxModel(options.toTypedArray())
        protocol.selectedItem = selectedProtocol
        protocol.isEnabled = profilesList.selectedValue != null && options.size > 1
        protocol.toolTipText = if (options.size == 1) {
            message("settings.protocol.fixed.tooltip")
        } else {
            null
        }
    }

    private fun addProfile() {
        val profile = ModelProfileState.fromPreset(ProviderPreset.CUSTOM).also {
            it.id = UUID.randomUUID().toString()
            it.name = message("settings.profile.newName", profilesModel.size + 1)
        }
        profilesModel.addElement(profile)
        profilesList.selectedIndex = profilesModel.size - 1
        if (defaultProfileId.isBlank()) defaultProfileId = profile.id
    }

    private fun removeSelectedProfile() {
        if (profilesModel.size <= 1) return
        val index = profilesList.selectedIndex.takeIf { it >= 0 } ?: return
        val profile = profilesModel.getElementAt(index)
        removedProfileIds += profile.id
        secretEdits.remove(profile.id)
        profilesModel.remove(index)
        if (profile.id == defaultProfileId) defaultProfileId = profilesModel.getElementAt(0).id
        profilesList.selectedIndex = index.coerceAtMost(profilesModel.size - 1)
        profilesList.repaint()
    }

    private fun moveSelectedProfile(offset: Int) {
        val index = profilesList.selectedIndex
        val target = index + offset
        if (index < 0 || target !in 0 until profilesModel.size) return
        val profile = profilesModel.remove(index)
        profilesModel.add(target, profile)
        profilesList.selectedIndex = target
    }

    private fun testCurrentConnection() {
        val profile = profilesList.selectedValue?.copyState() ?: return
        val editedSecret = secretEdits[profile.id]
        testConnection.isEnabled = false
        connectionStatus.text = message("settings.connection.testing")

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching {
                val key = editedSecret ?: ApiKeyStore.load(profile.id)
                if (key.isBlank()) error(message("settings.connection.apiKeyRequired"))
                CompatibleModelClient.generate(
                    profile = profile.toModel(),
                    apiKey = key,
                    prompt = "Return exactly: OK",
                )
            }
            ApplicationManager.getApplication().invokeLater(
                {
                    testConnection.isEnabled = true
                    connectionStatus.text = result.fold(
                        onSuccess = { message("settings.connection.success") },
                        onFailure = {
                            message("settings.connection.failed", it.message.orEmpty())
                        },
                    )
                },
                ModalityState.any(),
            )
        }
    }

    private fun updateCurrentProfile(update: (ModelProfileState) -> Unit) {
        if (loadingForm) return
        val profile = profilesList.selectedValue ?: return
        update(profile)
        profilesList.repaint()
    }

    private fun refreshDefaultButton() {
        val selected = profilesList.selectedValue
        setDefault.isEnabled = selected != null && selected.id != defaultProfileId
        setDefault.text = if (selected?.id == defaultProfileId) {
            message("settings.button.defaultProfile")
        } else {
            message("settings.button.setDefault")
        }
    }

    private fun setFormEnabled(enabled: Boolean) {
        listOf<JComponent>(
            profileName,
            providerPreset,
            protocol,
            baseUrl,
            model,
            apiKey,
            clearApiKey,
            testConnection,
            outputLanguage,
            promptTemplate,
        ).forEach { it.isEnabled = enabled }
        val selectedPreset = providerPreset.selectedItem as? ProviderPreset
        protocol.isEnabled = enabled && (selectedPreset?.supportedProtocols?.size ?: 0) > 1
    }

    private fun JPasswordField.onTextChanged(listener: () -> Unit) {
        document.addDocumentListener(SimpleDocumentListener(listener))
    }

    private fun JBTextField.onTextChanged(listener: () -> Unit) {
        document.addDocumentListener(SimpleDocumentListener(listener))
    }

    private fun JBTextArea.onTextChanged(listener: () -> Unit) {
        document.addDocumentListener(SimpleDocumentListener(listener))
    }

    private fun <T> ComboBox<T>.withWidth(width: Int): ComboBox<T> = apply {
        val scaledWidth = JBUI.scale(width)
        preferredSize = Dimension(scaledWidth, preferredSize.height)
        minimumSize = Dimension(scaledWidth, minimumSize.height)
    }

    private class SimpleDocumentListener(private val listener: () -> Unit) : DocumentListener {
        override fun insertUpdate(event: DocumentEvent) = listener()
        override fun removeUpdate(event: DocumentEvent) = listener()
        override fun changedUpdate(event: DocumentEvent) = listener()
    }
}
