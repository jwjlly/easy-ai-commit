package com.layxy.easyaicommit.icons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EasyAiCommitIconAssetsTest {
    @Test
    fun `toolbar logo uses a padded vector canvas`() {
        val svg = resourceText("/icons/easyAiCommit.svg")

        assertTrue(svg.contains("width=\"16\""))
        assertTrue(svg.contains("height=\"16\""))
        assertTrue(svg.contains("viewBox=\"0 0 40 40\""))
        assertTrue(svg.contains("translate(2.5 2.5) scale(.875)"))
        assertTrue(svg.toByteArray().size < 8_192)
        assertNull(javaClass.getResource("/icons/easyAiCommit.png"))
        assertNull(javaClass.getResource("/icons/easyAiCommit@2x.png"))
    }

    @Test
    fun `toolbar logo loads at the JetBrains action size`() {
        assertEquals(16, EasyAiCommitIcons.Logo.iconWidth)
        assertEquals(16, EasyAiCommitIcons.Logo.iconHeight)
    }

    @Test
    fun `plugin manager logo uses the required vector canvas`() {
        val svg = resourceText("/META-INF/pluginIcon.svg")

        assertTrue(svg.contains("width=\"40\""))
        assertTrue(svg.contains("height=\"40\""))
        assertFalse(svg.contains("scale(.875)"))
        assertTrue(svg.toByteArray().size < 8_192)
    }

    @Test
    fun `toolbar and plugin logos share colors and geometry`() {
        val toolbar = resourceText("/icons/easyAiCommit.svg")
        val plugin = resourceText("/META-INF/pluginIcon.svg")
        val expectedGradient = listOf("#4FADE3", "#7187E2", "#9668D9")

        assertEquals(expectedGradient, gradientColors(toolbar))
        assertEquals(expectedGradient, gradientColors(plugin))
        assertEquals(foregroundPath(toolbar), foregroundPath(plugin))
        assertTrue(toolbar.contains("gradientUnits=\"userSpaceOnUse\""))
        assertTrue(plugin.contains("gradientUnits=\"userSpaceOnUse\""))
        assertFalse(toolbar.contains("data:image"))
        assertFalse(plugin.contains("data:image"))
    }

    private fun resourceText(path: String): String =
        assertNotNull(javaClass.getResource(path), path).readText()

    private fun gradientColors(svg: String): List<String> =
        Regex("""stop-color=\"(#[0-9A-F]{6})\"""")
            .findAll(svg)
            .map { it.groupValues[1] }
            .toList()

    private fun foregroundPath(svg: String): String = assertNotNull(
        Regex("""<path fill=\"#FFFFFF\" d=\"([^\"]+)\"/>""").find(svg),
    ).groupValues[1]
}
