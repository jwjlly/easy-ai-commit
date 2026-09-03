package com.layxy.easyaicommit.settings;

import com.intellij.credentialStore.CredentialAttributes;
import org.jetbrains.annotations.NotNull;

/**
 * Calls the stable one-argument JVM overload directly.
 *
 * <p>Kotlin compiled against IntelliJ Platform 2024.2 otherwise targets a synthetic default-argument
 * constructor that is deprecated in 2026.1, even though this overload remains available in both versions.
 */
final class CredentialAttributesFactory {
    private CredentialAttributesFactory() {
    }

    static @NotNull CredentialAttributes create(@NotNull String serviceName) {
        return new CredentialAttributes(serviceName);
    }
}
