package com.layxy.easyaicommit.settings

import com.layxy.easyaicommit.model.ProxyConfiguration
import com.layxy.easyaicommit.model.ProxyMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertFalse

class CredentialChangesTest {
    @Test
    fun `silent password provider write failure is detected and earlier edits rolled back`() {
        var apiKey = "previous"
        val error = assertFailsWith<IllegalStateException> {
            CredentialChanges.apply(listOf(
                CredentialChanges.Edit({ apiKey }, { apiKey = it }, "new-api-key"),
                CredentialChanges.Edit({ "" }, { /* silently failed native storage */ }, "new-proxy-secret"),
            ))
        }
        assertEquals("previous", apiKey)
        assertFalse(error.message.orEmpty().contains("new-proxy-secret"))
    }

    @Test
    fun `deleting credentials is verified too`() {
        assertFailsWith<IllegalStateException> {
            CredentialChanges.apply(listOf(CredentialChanges.Edit({ "old-key" }, {}, "")))
        }
    }

    @Test
    fun `proxy password belongs to a profile endpoint and account but not its mode`() {
        val proxy = ProxyConfiguration(ProxyMode.CUSTOM, host = "proxy.example", port = 8080, username = "alice")
        val key = proxy.passwordKey("profile-1")
        assertNotEquals(key, proxy.passwordKey("profile-2"))
        assertNotEquals(key, proxy.copy(host = "another.example").passwordKey("profile-1"))
        assertNotEquals(key, proxy.copy(port = 8081).passwordKey("profile-1"))
        assertNotEquals(key, proxy.copy(username = "bob").passwordKey("profile-1"))
        assertEquals(key, proxy.copy(mode = ProxyMode.DIRECT).passwordKey("profile-1"))
    }
}
