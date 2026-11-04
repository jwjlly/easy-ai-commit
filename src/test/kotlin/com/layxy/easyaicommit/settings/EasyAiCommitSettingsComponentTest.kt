package com.layxy.easyaicommit.settings

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.openapi.options.ConfigurationException
import com.intellij.util.ui.UIUtil
import com.layxy.easyaicommit.EasyAiCommitBundle.message
import com.layxy.easyaicommit.model.ProxyMode
import com.layxy.easyaicommit.model.ProxyType
import com.layxy.easyaicommit.model.ProviderPreset
import com.layxy.easyaicommit.model.ProxyConfiguration
import java.awt.Component
import java.awt.Container
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JComboBox
import javax.swing.JCheckBox
import javax.swing.JList
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.JButton
import javax.swing.JTextArea
import javax.swing.JLabel
import com.intellij.icons.AllIcons
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import javax.swing.SwingUtilities
import javax.swing.JComponent
import kotlin.math.abs

class EasyAiCommitSettingsComponentTest : LightPlatformTestCase() {
    fun testNetworkFieldsSwitchWithProfilesWithoutModifyingSavedState() = runInEdtAndWait {
        val service = EasyAiCommitSettingsService()
        val first = ModelProfileState.defaultOpenAi().apply {
            proxy = ProxySettingsState.fromModel(ProxyConfiguration(ProxyMode.CUSTOM, host = "localhost", port = 7890))
            requestTimeoutSeconds = 30
        }
        val second = ModelProfileState.fromPreset(ProviderPreset.CUSTOM)
        service.replace(listOf(first, second), first.id, 60000)
        val snapshot = service.snapshot()
        val component = EasyAiCommitSettingsComponent()
        try {
            component.reset(snapshot)
            assertFalse(component.isModified(snapshot))
            assertTrue(component.find("proxyDetails").isVisible)
            val timeout = component.find("requestTimeout") as JSpinner
            timeout.value = 15
            val presets = component.find("providerPreset") as JComboBox<*>
            presets.selectedItem = ProviderPreset.DEEPSEEK
            assertEquals(15, timeout.value)
            assertEquals("localhost", (component.find("proxyHost") as JTextField).text)
            val profiles = component.find("profiles") as JList<*>
            profiles.selectedIndex = 1
            assertFalse(component.find("proxyDetails").isVisible)
            assertEquals(90, timeout.value)
            profiles.selectedIndex = 0
            assertEquals(15, timeout.value)
            assertTrue(component.isModified(snapshot))
            assertEquals(30, service.snapshot().profiles.first().requestTimeoutSeconds)

            val type = component.find("proxyType") as JComboBox<*>
            val auth = component.find("proxyAuthentication") as JCheckBox
            auth.doClick()
            assertTrue(component.find("proxyAuthenticationFields").isVisible)
            type.selectedItem = ProxyType.SOCKS5
            assertFalse(auth.isSelected)
            assertFalse(component.find("proxyAuthenticationFields").isVisible)
            type.selectedItem = ProxyType.HTTP
            auth.doClick()
            (component.find("proxyUsername") as JTextField).text = "proxy-user"
            render(component, "custom-proxy")

            component.reset(snapshot)
            assertFalse(component.isModified(snapshot))
            profiles.selectedIndex = 1
            assertEquals(ProxyMode.DIRECT, (component.find("proxyMode") as JComboBox<*>).selectedItem)
            render(component, "direct")
        } finally { component.dispose() }
    }

    fun testTimeoutEditorIsCommittedWhenApplying() = runInEdtAndWait {
        val service = EasyAiCommitSettingsService()
        val component = EasyAiCommitSettingsComponent()
        try {
            component.reset(service.snapshot())
            val spinner = component.find("requestTimeout") as JSpinner
            (spinner.editor as JSpinner.DefaultEditor).textField.text = "25"
            component.apply(service)
            assertEquals(25, service.snapshot().defaultProfile!!.requestTimeoutSeconds)
            assertFalse(component.isModified(service.snapshot()))
        } finally { component.dispose() }
    }

    fun testNetworkLayoutStaysCompactAndAlignedAtDifferentWidths() = runInEdtAndWait {
        val component = EasyAiCommitSettingsComponent()
        try {
            val service = EasyAiCommitSettingsService()
            component.reset(service.snapshot())
            (component.find("proxyMode") as JComboBox<*>).selectedItem = ProxyMode.CUSTOM
            (component.find("proxyAuthentication") as JCheckBox).doClick()
            val status = component.find("connectionStatus") as JTextArea
            val button = component.find("testConnection") as JButton
            val help = component.find("timeoutHelp") as JButton
            val timeout = component.find("requestTimeout") as JSpinner
            for (width in listOf(900, 1150, 1400)) {
                for (text in listOf("Connection successful", "Connection failed: " + "Check the proxy settings and network. ".repeat(8))) {
                    status.text = text
                    component.panel.setSize(width, 1000)
                    repeat(6) { descendants(component.panel).filterIsInstance<Container>().forEach { it.doLayout() } }
                    fun origin(item: Component) = SwingUtilities.convertPoint(item, 0, 0, component.panel)
                    val gap = origin(help).x - (origin(timeout).x + timeout.width)
                    assertTrue("Help gap=$gap at width=$width", gap in 0..com.intellij.util.ui.JBUI.scale(6))
                    assertTrue(help.width <= com.intellij.util.ui.JBUI.scale(24))
                    val buttonBaseline = origin(button).y + button.getBaseline(button.width, button.height)
                    val textBaseline = origin(status).y + status.getBaseline(status.width, status.height)
                    assertTrue("Button/text baselines: $buttonBaseline/$textBaseline at width=$width", abs(buttonBaseline - textBaseline) <= 1)
                    val fieldXs = listOf("proxyType", "proxyHost", "proxyUsername", "proxyPassword").map { origin(component.find(it)).x }
                    assertEquals("Proxy fields must share one left edge", 1, fieldXs.distinct().size)
                    val host = component.find("proxyHost") as JComponent
                    val port = component.find("proxyPort") as JComponent
                    assertEquals(origin(host).y + host.getBaseline(host.width, host.height), origin(port).y + port.getBaseline(port.width, port.height))
                }
            }
        } finally { component.dispose() }
    }

    fun testSwitchingProfilesCancelsTestAndIgnoresItsLateResult() = runInEdtAndWait {
        MockWebServer().use { slow ->
            MockWebServer().use { fast ->
                val body = """{"choices":[{"message":{"content":"OK"}}]}"""
                slow.enqueue(MockResponse().setBody(body).setBodyDelay(500, TimeUnit.MILLISECONDS))
                fast.enqueue(MockResponse().setBody("""{
                    "status":"completed", "output":[{"type":"message","content":[{"type":"output_text","text":"OK"}]}]
                }"""))
                val profiles = listOf(slow, fast).map { server ->
                    ModelProfileState.defaultOpenAi().apply {
                        id = "ui-test-${server.port}"
                        baseUrl = server.url(if (server === fast) "/v1/responses" else "/v1").toString()
                        proxy.mode = ProxyMode.DIRECT.name
                        requestTimeoutSeconds = 5
                    }
                }
                val service = EasyAiCommitSettingsService()
                service.replace(profiles, profiles.first().id, 60000)
                val component = EasyAiCommitSettingsComponent()
                try {
                    component.reset(service.snapshot())
                    val button = component.find("testConnection") as JButton
                    val status = component.find("connectionStatus") as JTextArea
                    val icon = component.find("connectionStatusIcon") as JLabel
                    val idleColor = status.foreground
                    (component.find("apiKey") as JTextField).text = "local-test-key"
                    button.doClick()
                    assertFalse(button.isEnabled)
                    dispatchUntil { slow.requestCount == 1 }

                    (component.find("profiles") as JList<*>).selectedIndex = 1
                    assertTrue(button.isEnabled)
                    assertEquals("", status.text)
                    (component.find("apiKey") as JTextField).text = "another-local-test-key"
                    button.doClick()
                    dispatchUntil { button.isEnabled }
                    val latestStatus = status.text
                    assertTrue(latestStatus, latestStatus.contains(message("proxy.mode.direct")))
                    assertTrue(latestStatus, latestStatus.contains(message("network.connection.unsaved")))
                    assertEquals(1, fast.requestCount)
                    assertEquals("/v1/responses", fast.takeRequest(1, TimeUnit.SECONDS)!!.path)
                    assertEquals(AllIcons.General.InspectionsOK, icon.icon)
                    val successColor = status.foreground
                    assertFalse(idleColor == successColor)
                    render(component, "connection-success")

                    val afterSlowResponse = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(700)
                    dispatchUntil { System.nanoTime() >= afterSlowResponse }
                    assertEquals(latestStatus, status.text)
                    assertTrue(button.isEnabled)
                    fast.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"Invalid test API key"}}"""))
                    button.doClick()
                    dispatchUntil { button.isEnabled }
                    assertEquals(AllIcons.General.Error, icon.icon)
                    assertFalse(successColor == status.foreground)
                    assertTrue(status.text, status.text.contains("401"))
                    render(component, "connection-failure")
                    (component.find("requestTimeout") as JSpinner).value = 30
                    assertEquals("", status.text)
                    assertFalse(icon.isVisible)
                    assertEquals(idleColor, status.foreground)
                } finally { component.dispose() }
            }
        }
    }

    fun testTimeoutHelpReplacesInlineDescriptionAndValidationFailureIsHighlighted() = runInEdtAndWait {
        val component = EasyAiCommitSettingsComponent()
        try {
            component.reset(EasyAiCommitSettingsService().snapshot())
            val help = component.find("timeoutHelp") as JButton
            assertEquals(AllIcons.General.ContextHelp, help.icon)
            assertEquals(message("network.timeout.help"), help.accessibleContext.accessibleName)
            assertTrue(help.isFocusable)
            assertTrue(help.actionListeners.isNotEmpty())
            assertFalse(descendants(component.panel).filterIsInstance<JLabel>().any { it.text == message("network.timeout.hint") })
            (component.find("proxyMode") as JComboBox<*>).selectedItem = ProxyMode.CUSTOM
            (component.find("testConnection") as JButton).doClick()
            assertEquals(AllIcons.General.Error, (component.find("connectionStatusIcon") as JLabel).icon)
            assertTrue((component.find("connectionStatus") as JTextArea).text.contains(message("proxy.validation.host")))
        } finally { component.dispose() }
    }

    fun testMissingProxyPasswordKeepsDraftAndDoesNotSaveSettings() = runInEdtAndWait {
        val service = EasyAiCommitSettingsService()
        val saved = service.snapshot()
        val component = EasyAiCommitSettingsComponent()
        try {
            component.reset(saved)
            (component.find("proxyMode") as JComboBox<*>).selectedItem = ProxyMode.CUSTOM
            (component.find("proxyHost") as JTextField).text = "localhost"
            (component.find("proxyAuthentication") as JCheckBox).doClick()
            (component.find("proxyUsername") as JTextField).text = "test-user"
            val password = component.find("proxyPassword") as JTextField
            password.text = "temporary-test-password"
            password.text = "" // Explicit empty draft: no Password Safe access is needed.
            val error = assertFailsWith<ConfigurationException> { component.apply(service) }
            assertEquals(message("proxy.error.passwordMissing"), error.message)
            assertEquals(saved, service.snapshot())
            assertTrue(component.isModified(saved))
            assertEquals("test-user", (component.find("proxyUsername") as JTextField).text)
        } finally { component.dispose() }
    }

    private fun dispatchUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!condition()) {
            assertTrue("Timed out waiting for UI/network state", System.nanoTime() < deadline)
            UIUtil.dispatchAllInvocationEvents()
            Thread.sleep(5)
        }
    }

    private fun EasyAiCommitSettingsComponent.find(name: String): Component =
        descendants(panel).first { it.name == name }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private fun render(component: EasyAiCommitSettingsComponent, name: String) {
        component.panel.setSize(1150, 1000)
        repeat(3) { descendants(component.panel).filterIsInstance<Container>().forEach { it.doLayout() } }
        val image = BufferedImage(1150, 1000, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try { component.panel.printAll(graphics) } finally { graphics.dispose() }
        val directory = File("build/reports/settings-preview").apply { mkdirs() }
        ImageIO.write(image, "png", File(directory, "$name.png"))
    }
}
