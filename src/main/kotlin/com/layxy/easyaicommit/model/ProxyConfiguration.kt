package com.layxy.easyaicommit.model

import com.layxy.easyaicommit.EasyAiCommitBundle.message
import java.net.URI
import java.security.MessageDigest

enum class ProxyMode {
    IDE, DIRECT, CUSTOM;

    override fun toString(): String = message("proxy.mode.${name.lowercase()}")

    companion object {
        fun fromStored(value: String): ProxyMode = entries.firstOrNull { it.name == value } ?: IDE
    }
}

enum class ProxyType { HTTP, SOCKS5 }

data class ProxyConfiguration(
    val mode: ProxyMode = ProxyMode.IDE,
    val type: ProxyType = ProxyType.HTTP,
    val host: String = "",
    val port: Int = 7890,
    val authentication: Boolean = false,
    val username: String = "",
) {
    val requiresPassword: Boolean
        get() = mode == ProxyMode.CUSTOM && type == ProxyType.HTTP && authentication

    fun validate() {
        if (mode != ProxyMode.CUSTOM) return
        val validHost = host.isNotBlank() && host == host.trim() &&
            host.none { it.isWhitespace() || it in "/?#@" } &&
            runCatching {
                val authority = if (':' in host && !host.startsWith('[')) "[$host]" else host
                URI("http://$authority").let { it.host != null && it.port == -1 && it.userInfo == null }
            }.getOrDefault(false)
        require(validHost) { message("proxy.validation.host") }
        require(port in 1..65535) { message("proxy.validation.port") }
        require(type != ProxyType.SOCKS5 || !authentication) { message("proxy.validation.socksAuth") }
        require(!authentication || username.isNotBlank()) { message("proxy.validation.username") }
    }

    // Bind credentials to both the profile and proxy identity. A changed endpoint must never
    // silently receive the password that belonged to a different proxy server/account.
    fun passwordKey(profileId: String): String {
        val identity = listOf(type.name, host.lowercase(), port.toString(), username)
            .joinToString("") { "${it.length}:$it" }
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "Proxy password $profileId/$digest"
    }

    fun description(): String = if (mode == ProxyMode.CUSTOM) "$type $host:$port" else mode.toString()
}
