package com.layxy.easyaicommit.model

import com.layxy.easyaicommit.EasyAiCommitBundle
import com.intellij.DynamicBundle
import java.util.Locale

data class ResolvedOutputLanguage(
    val localeTag: String,
    val nativeName: String,
    val promptName: String,
)

enum class OutputLanguage(
    val id: String,
    private val localeTag: String?,
    private val nativeName: String,
    private val promptName: String,
) {
    AUTO("auto", null, "", ""),
    ENGLISH("en", "en", "English", "English (en)"),
    SIMPLIFIED_CHINESE("zh-CN", "zh-CN", "简体中文", "Simplified Chinese (zh-CN)"),
    TRADITIONAL_CHINESE("zh-TW", "zh-TW", "繁體中文", "Traditional Chinese (zh-TW)"),
    JAPANESE("ja", "ja", "日本語", "Japanese (ja)"),
    KOREAN("ko", "ko", "한국어", "Korean (ko)"),
    FRENCH("fr", "fr", "Français", "French (fr)"),
    GERMAN("de", "de", "Deutsch", "German (de)"),
    SPANISH("es", "es", "Español", "Spanish (es)"),
    PORTUGUESE_BRAZIL("pt-BR", "pt-BR", "Português (Brasil)", "Brazilian Portuguese (pt-BR)"),
    ITALIAN("it", "it", "Italiano", "Italian (it)"),
    RUSSIAN("ru", "ru", "Русский", "Russian (ru)"),
    UKRAINIAN("uk", "uk", "Українська", "Ukrainian (uk)"),
    POLISH("pl", "pl", "Polski", "Polish (pl)"),
    TURKISH("tr", "tr", "Türkçe", "Turkish (tr)"),
    VIETNAMESE("vi", "vi", "Tiếng Việt", "Vietnamese (vi)"),
    INDONESIAN("id", "id", "Bahasa Indonesia", "Indonesian (id)"),
    ARABIC("ar", "ar", "العربية", "Arabic (ar)"),
    HINDI("hi", "hi", "हिन्दी", "Hindi (hi)");

    fun displayName(ideLocale: Locale = DynamicBundle.getLocale()): String = if (this == AUTO) {
        EasyAiCommitBundle.message("output.language.auto", resolve(ideLocale).nativeName)
    } else {
        nativeName
    }

    fun resolve(ideLocale: Locale = DynamicBundle.getLocale()): ResolvedOutputLanguage = if (this == AUTO) {
        resolveIdeLocale(ideLocale)
    } else {
        asResolved()
    }

    override fun toString(): String = displayName()

    private fun asResolved(): ResolvedOutputLanguage = ResolvedOutputLanguage(
        localeTag = requireNotNull(localeTag),
        nativeName = nativeName,
        promptName = promptName,
    )

    companion object {
        val options: Array<OutputLanguage>
            get() = entries.toTypedArray()

        fun fromStored(storedValue: String): OutputLanguage = entries.firstOrNull {
            it.id.equals(storedValue.trim(), ignoreCase = true)
        } ?: AUTO

        fun normalize(storedValue: String): String = fromStored(storedValue).id

        fun resolve(
            storedValue: String,
            ideLocale: Locale = DynamicBundle.getLocale(),
        ): ResolvedOutputLanguage = fromStored(storedValue).resolve(ideLocale)

        private fun resolveIdeLocale(locale: Locale): ResolvedOutputLanguage {
            val language = locale.language.lowercase(Locale.ROOT)
            if (language.isBlank() || language == "und") return ENGLISH.asResolved()

            val country = locale.country.uppercase(Locale.ROOT)
            val script = locale.script
            val matchingPreset = when (language) {
                "zh" -> if (
                    country in setOf("TW", "HK", "MO") ||
                    script.equals("Hant", ignoreCase = true)
                ) {
                    TRADITIONAL_CHINESE
                } else {
                    SIMPLIFIED_CHINESE
                }
                "pt" -> PORTUGUESE_BRAZIL.takeIf { country.isBlank() || country == "BR" }
                else -> entries.firstOrNull { option ->
                    option != AUTO &&
                        option.localeTag?.contains('-') == false &&
                        Locale.forLanguageTag(option.localeTag).language.equals(language, ignoreCase = true)
                }
            }
            if (matchingPreset != null) return matchingPreset.asResolved()

            val localeTag = locale.toLanguageTag().takeUnless { it.isBlank() || it == "und" }
                ?: return ENGLISH.asResolved()
            val englishName = locale.getDisplayLanguage(Locale.ENGLISH).ifBlank { localeTag }
            val localizedName = locale.getDisplayLanguage(locale).ifBlank { englishName }
            return ResolvedOutputLanguage(
                localeTag = localeTag,
                nativeName = localizedName.replaceFirstChar { it.titlecase(locale) },
                promptName = "${englishName.replaceFirstChar { it.titlecase(Locale.ENGLISH) }} ($localeTag)",
            )
        }
    }
}
