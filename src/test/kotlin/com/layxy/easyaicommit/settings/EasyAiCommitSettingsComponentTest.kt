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
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

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
            render(component, "ide-proxy")
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

    fun testSwitchingProfilesCancelsTestAndIgnoresItsLateResult() = runInEdtAndWait {
        MockWebServer().use { slow ->
            MockWebServer().use { fast ->
                val body = """{"choices":[{"message":{"content":"OK"}}]}"""
                slow.enqueue(MockResponse().setBody(body).setBodyDelay(500, TimeUnit.MILLISECONDS))
                fast.enqueue(MockResponse().setBody(body))
                val profiles = listOf(slow, fast).map { server ->
                    ModelProfileState.defaultOpenAi().apply {
                        id = "ui-test-${server.port}"
                        baseUrl = server.url("/v1").toString()
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

                    val afterSlowResponse = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(700)
                    dispatchUntil { System.nanoTime() >= afterSlowResponse }
                    assertEquals(latestStatus, status.text)
                    assertTrue(button.isEnabled)
                } finally { component.dispose() }
            }
        }
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
