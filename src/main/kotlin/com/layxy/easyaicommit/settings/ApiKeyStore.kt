package com.layxy.easyaicommit.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

object ApiKeyStore {
    private fun attributes(profileId: String): CredentialAttributes =
        CredentialAttributesFactory.create(
            generateServiceName("Easy AI Commit", "API key $profileId"),
        )

    @RequiresBackgroundThread
    fun load(profileId: String): String =
        PasswordSafe.instance.getPassword(attributes(profileId)).orEmpty()

    @RequiresBackgroundThread
    fun save(profileId: String, apiKey: String) {
        if (apiKey.isBlank()) {
            remove(profileId)
        } else {
            PasswordSafe.instance.setPassword(attributes(profileId), apiKey)
        }
    }

    @RequiresBackgroundThread
    fun remove(profileId: String) {
        PasswordSafe.instance[attributes(profileId)] = null
    }
}
