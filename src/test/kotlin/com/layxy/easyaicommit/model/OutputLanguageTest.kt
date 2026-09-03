package com.layxy.easyaicommit.model

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutputLanguageTest {
    @Test
    fun `reads stable ids and falls back to automatic`() {
        val expected = mapOf(
            "zh-CN" to OutputLanguage.SIMPLIFIED_CHINESE,
            " EN " to OutputLanguage.ENGLISH,
            "auto" to OutputLanguage.AUTO,
            "unknown" to OutputLanguage.AUTO,
        )

        expected.forEach { (storedValue, language) ->
            assertEquals(language, OutputLanguage.fromStored(storedValue), storedValue)
            assertEquals(language.id, OutputLanguage.normalize(storedValue), storedValue)
        }
    }

    @Test
    fun `uses native language names for manual presets`() {
        val expected = mapOf(
            OutputLanguage.ENGLISH to "English",
            OutputLanguage.SIMPLIFIED_CHINESE to "简体中文",
            OutputLanguage.TRADITIONAL_CHINESE to "繁體中文",
            OutputLanguage.JAPANESE to "日本語",
            OutputLanguage.KOREAN to "한국어",
            OutputLanguage.ARABIC to "العربية",
            OutputLanguage.HINDI to "हिन्दी",
        )

        expected.forEach { (language, displayName) ->
            assertEquals(displayName, language.displayName(Locale.ENGLISH), language.id)
        }
        assertEquals(19, OutputLanguage.entries.size)
        assertEquals(OutputLanguage.entries.size, OutputLanguage.entries.map { it.id }.toSet().size)
    }

    @Test
    fun `auto follows common ide locales`() {
        val expected = mapOf(
            Locale.forLanguageTag("zh-CN") to "zh-CN",
            Locale.forLanguageTag("zh-HK") to "zh-TW",
            Locale.forLanguageTag("zh-Hant") to "zh-TW",
            Locale.forLanguageTag("zh-TW") to "zh-TW",
            Locale.JAPAN to "ja",
            Locale.KOREA to "ko",
            Locale.GERMANY to "de",
            Locale.forLanguageTag("pt-BR") to "pt-BR",
        )

        expected.forEach { (locale, localeTag) ->
            assertEquals(localeTag, OutputLanguage.AUTO.resolve(locale).localeTag, locale.toLanguageTag())
        }
    }

    @Test
    fun `auto preserves an unsupported ide locale and falls back for root`() {
        val dutch = OutputLanguage.AUTO.resolve(Locale.forLanguageTag("nl-NL"))
        assertEquals("nl-NL", dutch.localeTag)
        assertTrue(dutch.promptName.contains("Dutch"))

        assertEquals("en", OutputLanguage.AUTO.resolve(Locale.ROOT).localeTag)
    }
}
