package com.layxy.easyaicommit.settings

import com.layxy.easyaicommit.model.ModelProfile
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

object ProxyPasswordStore {
    @RequiresBackgroundThread
    fun load(profile: ModelProfile): String = loadKey(profile.proxy.passwordKey(profile.id))

    @RequiresBackgroundThread
    fun loadKey(key: String): String = PasswordSafe.instance.getPassword(attributes(key)).orEmpty()

    @RequiresBackgroundThread
    fun saveKey(key: String, password: String) {
        PasswordSafe.instance.setPassword(attributes(key), password.takeUnless(String::isBlank))
    }

    private fun attributes(key: String) = CredentialAttributesFactory.create(generateServiceName("Easy AI Commit", key))
}
