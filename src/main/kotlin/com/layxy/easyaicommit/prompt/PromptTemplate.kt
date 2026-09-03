package com.layxy.easyaicommit.prompt

import com.layxy.easyaicommit.model.ModelProfile
import com.layxy.easyaicommit.model.OutputLanguage
import com.intellij.DynamicBundle
import java.util.Locale

object PromptTemplate {
    const val DEFAULT = """You are a senior software engineer. Generate a concise and accurate Git commit message from the provided changes.

Requirements:
1. Use the Conventional Commits format.
2. Keep the subject line at or below 72 characters.
3. Write the commit message in {language}.
4. Return only the commit message without explanations or Markdown fences.

Changed files:
{files}

Changes:
{changes}"""

    fun render(
        profile: ModelProfile,
        files: List<String>,
        changes: String,
        ideLocale: Locale = DynamicBundle.getLocale(),
    ): String {
        val template = profile.promptTemplate.ifBlank { DEFAULT }
        val outputLanguage = OutputLanguage.resolve(profile.outputLanguage, ideLocale)
        val rendered = template
            .replace("{language}", outputLanguage.promptName)
            .replace("{files}", files.joinToString("\n"))
            .replace("{changes}", changes)

        return if ("{changes}" in template) {
            rendered
        } else {
            "$rendered\n\nChanges:\n$changes"
        }
    }
}
