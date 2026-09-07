package com.layxy.easyaicommit.client

import com.layxy.easyaicommit.EasyAiCommitBundle.message
import com.layxy.easyaicommit.model.ModelProtocol
import com.layxy.easyaicommit.model.ProxyConfiguration
import com.layxy.easyaicommit.model.ProxyMode
import com.layxy.easyaicommit.model.ProxyType
import com.layxy.easyaicommit.settings.ModelProfileState
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.ServerSocket
import java.net.SocketAddress
import java.net.URI
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelHttpTransportTest {
    private fun profile(proxy: ProxyConfiguration = ProxyConfiguration(mode = ProxyMode.DIRECT)) =
        ModelProfileState.defaultOpenAi().toModel().copy(proxy = proxy, requestTimeoutSeconds = 5)

    private fun proxy(server: MockWebServer, authentication: Boolean = false, username: String = "test-user") =
        ProxyConfiguration(ProxyMode.CUSTOM, ProxyType.HTTP, server.hostName, server.port, authentication, username)

    private fun withTransport(test: (ModelHttpTransport) -> Unit) {
        val executor = NetworkRequestExecutor()
        try { test(ModelHttpTransport(executor) { OkHttpClient.Builder() }) } finally { executor.dispose() }
    }

    @Test
    fun `IDE route uses selected proxy while direct explicitly bypasses it`() = withTransport { transport ->
        MockWebServer().use { target ->
            MockWebServer().use { proxy ->
                target.enqueue(MockResponse().setBody("direct"))
                proxy.enqueue(MockResponse().setBody("proxied"))
                val previous = ProxySelector.getDefault()
                val selector = object : ProxySelector() {
                    override fun select(uri: URI) = listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.hostName, proxy.port)))
                    override fun connectFailed(uri: URI, sa: SocketAddress, error: IOException) = Unit
                }
                ProxySelector.setDefault(selector)
                try {
                    val url = target.url("/v1/chat/completions").toString()
                    val inherited = transport.postJson(profile(ProxyConfiguration(ProxyMode.IDE)), url, "{}", emptyMap(), "", null)
                    assertEquals("proxied", inherited.body)
                    val direct = transport.postJson(profile(ProxyConfiguration()), url, "{}", emptyMap(), "", null)
                    assertEquals("direct", direct.body)
                    assertEquals(1, target.requestCount)
                    assertEquals(1, proxy.requestCount)
                    assertTrue(ProxySelector.getDefault() === selector)
                } finally { ProxySelector.setDefault(previous) }
            }
        }
    }

    @Test
    fun `HTTP authentication keeps model Authorization and profile credentials separate in parallel`() = withTransport { transport ->
        val callers = Executors.newFixedThreadPool(2)
        try {
            MockWebServer().use { first ->
                MockWebServer().use { second ->
                    listOf(first, second).forEach {
                        it.enqueue(MockResponse().setResponseCode(407).addHeader("Proxy-Authenticate", "Basic realm=proxy"))
                        it.enqueue(MockResponse().setBody("OK"))
                    }
                    val calls = listOf(first, second).mapIndexed { index, server ->
                        callers.submit<String> {
                            transport.postJson(profile(proxy(server, true, "user-$index")),
                                "http://model.invalid/v1/messages", "{\"model\":\"test\"}",
                                mapOf("Authorization" to "Bearer model-key-$index"), "proxy-pass-$index", null).body
                        }
                    }
                    calls.forEach { assertEquals("OK", it.get(5, TimeUnit.SECONDS)) }
                    listOf(first, second).forEachIndexed { index, server ->
                        assertNull(server.takeRequest(1, TimeUnit.SECONDS)!!.getHeader("Proxy-Authorization"))
                        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
                        assertEquals(Credentials.basic("user-$index", "proxy-pass-$index"), request.getHeader("Proxy-Authorization"))
                        assertEquals("Bearer model-key-$index", request.getHeader("Authorization"))
                    }
                }
            }
        } finally { callers.shutdownNow() }
    }

    @Test
    fun `HTTPS CONNECT authenticates proxy without forwarding proxy password into the TLS request`() {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("model.invalid").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val executor = NetworkRequestExecutor()
        try {
            val transport = ModelHttpTransport(executor) { OkHttpClient.Builder().sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager) }
            MockWebServer().use { server ->
                server.useHttps(serverTls.sslSocketFactory(), true)
                server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.UPGRADE_TO_SSL_AT_END))
                server.enqueue(MockResponse().setBody("OK"))
                val response = transport.postJson(profile(proxy(server, true)), "https://model.invalid/v1/messages", "{}",
                    mapOf("x-api-key" to "model-secret"), "proxy-secret", null)
                assertEquals("OK", response.body)
                val connect = server.takeRequest(1, TimeUnit.SECONDS)!!
                assertEquals("CONNECT", connect.method)
                assertEquals(Credentials.basic("test-user", "proxy-secret"), connect.getHeader("Proxy-Authorization"))
                assertNull(connect.getHeader("x-api-key"))
                val model = server.takeRequest(1, TimeUnit.SECONDS)!!
                assertEquals("model-secret", model.getHeader("x-api-key"))
                assertNull(model.getHeader("Proxy-Authorization"))
            }
        } finally { executor.dispose() }
    }

    @Test
    fun `proxy authentication failure differs from model 401 and stops retrying`() = withTransport { transport ->
        MockWebServer().use { server ->
            repeat(2) { server.enqueue(MockResponse().setResponseCode(407).addHeader("Proxy-Authenticate", "Basic realm=proxy")) }
            val error = assertFailsWith<ModelRequestException> {
                transport.postJson(profile(proxy(server, true)), "http://model.invalid/v1/messages", "{}", emptyMap(), "wrong", null)
            }
            assertEquals(message("proxy.error.authentication"), error.message)
            assertEquals(2, server.requestCount)
            server.enqueue(MockResponse().setResponseCode(401).setBody("invalid model key"))
            assertEquals(401, transport.postJson(profile(proxy(server)), "http://model.invalid/v1/messages", "{}", emptyMap(), "", null).status)
        }
    }

    @Test
    fun `unreachable explicit proxy never falls back to a reachable model`() = withTransport { transport ->
        val port = ServerSocket(0).use { it.localPort }
        MockWebServer().use { model ->
            model.enqueue(MockResponse().setBody("must not arrive"))
            assertFailsWith<ModelRequestException> {
                transport.postJson(profile(ProxyConfiguration(ProxyMode.CUSTOM, host = "127.0.0.1", port = port)),
                    model.url("/v1/messages").toString(), "{}", emptyMap(), "", null)
            }
            assertEquals(0, model.requestCount)
        }
    }

    @Test
    fun `total timeout stops slow continuous responses in every proxy mode`() = withTransport { transport ->
        val callers = Executors.newFixedThreadPool(3)
        val servers = ProxyMode.entries.map { MockWebServer() }
        try {
            val futures = ProxyMode.entries.zip(servers).map { (mode, server) ->
                server.enqueue(MockResponse().setBody("12345678901234567890").throttleBody(1, 500, TimeUnit.MILLISECONDS))
                val profile = profile(if (mode == ProxyMode.CUSTOM) proxy(server) else ProxyConfiguration(mode))
                callers.submit {
                    val started = System.nanoTime()
                    val error = assertFailsWith<ModelRequestException> {
                        transport.postJson(profile, server.url("/v1/messages").toString(), "{}", emptyMap(), "", null)
                    }
                    assertEquals(message("network.error.timeout", profile.name, 5), error.message)
                    val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
                    assertTrue(elapsed in 4500..7500, "elapsed=$elapsed for $mode")
                }
            }
            futures.forEach { it.get(9, TimeUnit.SECONDS) }
        } finally {
            callers.shutdownNow()
            servers.forEach { it.shutdown() }
        }
    }

    @Test
    fun `SOCKS sends unresolved destination to proxy for both model protocols`() = withTransport { transport ->
        ModelProtocol.entries.forEach { protocol ->
            ServerSocket(0).use { socks ->
                val serverThread = Executors.newSingleThreadExecutor()
                val received = serverThread.submit<String> {
                    socks.accept().use { socket ->
                        socket.soTimeout = 5000
                        val input = java.io.DataInputStream(socket.getInputStream())
                        val output = socket.getOutputStream()
                        assertEquals(5, input.readUnsignedByte())
                        input.readNBytes(input.readUnsignedByte())
                        output.write(byteArrayOf(5, 0)); output.flush()
                        assertEquals(5, input.readUnsignedByte())
                        assertEquals(1, input.readUnsignedByte())
                        input.readUnsignedByte()
                        assertEquals(3, input.readUnsignedByte(), "Destination must be a hostname, not a locally resolved IP")
                        val hostname = String(input.readNBytes(input.readUnsignedByte()), Charsets.UTF_8)
                        input.readUnsignedShort()
                        output.write(byteArrayOf(5, 0, 0, 1, 127, 0, 0, 1, 0, 80)); output.flush()
                        val reader = input.bufferedReader()
                        val requestLine = reader.readLine()
                        var contentLength = 0
                        while (true) {
                            val line = reader.readLine()
                            if (line.isEmpty()) break
                            if (line.startsWith("Content-Length:", true)) contentLength = line.substringAfter(':').trim().toInt()
                        }
                        repeat(contentLength) { reader.read() }
                        output.write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nOK".toByteArray())
                        output.flush()
                        "$hostname $requestLine"
                    }
                }
                try {
                    val url = CompatibleModelClient.endpoint("http://not-resolvable.invalid/v1", protocol)
                    val result = transport.postJson(profile(ProxyConfiguration(ProxyMode.CUSTOM, ProxyType.SOCKS5, "127.0.0.1", socks.localPort)),
                        url, "{}", emptyMap(), "", null)
                    assertEquals("OK", result.body)
                    assertContains(received.get(1, TimeUnit.SECONDS), "not-resolvable.invalid POST ${URI(url).path}")
                } finally { serverThread.shutdownNow() }
            }
        }
    }
}
