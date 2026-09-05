package com.layxy.easyaicommit.settings

import com.layxy.easyaicommit.EasyAiCommitBundle.message
import com.intellij.openapi.progress.ProcessCanceledException

/** Applies credential changes before the normal settings, retaining old values for best-effort rollback. */
internal object CredentialChanges {
    class Edit(val read: () -> String, val write: (String) -> Unit, val value: String)

    fun apply(edits: List<Edit>, verify: () -> Unit = {}) {
        val previous = mutableListOf<Pair<Edit, String>>()
        try {
            edits.forEach { edit ->
                previous += edit to edit.read()
                edit.write(edit.value)
                check(edit.read() == edit.value) { message("credentials.error.save") }
            }
            verify()
        } catch (error: Exception) {
            previous.asReversed().forEach { (edit, value) -> runCatching { edit.write(value) } }
            if (error is ProcessCanceledException) throw error
            // Password providers can include secret material in their exception messages.
            throw IllegalStateException(message("credentials.error.save"))
        }
    }
}
