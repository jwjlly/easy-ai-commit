package com.layxy.easyaicommit.prompt

import com.layxy.easyaicommit.model.ModelProfile
import com.layxy.easyaicommit.model.ModelProtocol
import com.layxy.easyaicommit.model.OutputLanguage
import com.layxy.easyaicommit.model.ProviderPreset
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertContains

class PromptTemplateTest {
    @Test
    fun `renders files language and changes`() {
        val profile = ModelProfile(
            id = "test",
            name = "Test",
            preset = ProviderPreset.CUSTOM,
            protocol = ModelProtocol.OPENAI_COMPATIBLE,
            baseUrl = "https://example.test/v1",
            model = "test-model",
            outputLanguage = OutputLanguage.SIMPLIFIED_CHINESE.id,
            promptTemplate = "Language: {language}\nFiles: {files}\nDiff: {changes}",
        )

        val prompt = PromptTemplate.render(
            profile,
            listOf("a.kt", "b.kt"),
            "+ changed",
            Locale.ENGLISH,
        )
        assertContains(prompt, "Language: Simplified Chinese (zh-CN)")
        assertContains(prompt, "a.kt\nb.kt")
        assertContains(prompt, "+ changed")
    }

    @Test
    fun `resolves auto output language from the ide locale`() {
        val profile = ModelProfile(
            id = "test",
            name = "Test",
            preset = ProviderPreset.CUSTOM,
            protocol = ModelProtocol.OPENAI_COMPATIBLE,
            baseUrl = "https://example.test/v1",
            model = "test-model",
            outputLanguage = "auto",
            promptTemplate = "Language: {language}\n{changes}",
        )

        val prompt = PromptTemplate.render(profile, emptyList(), "+ changed", Locale.JAPAN)

        assertContains(prompt, "Language: Japanese (ja)")
    }
}
