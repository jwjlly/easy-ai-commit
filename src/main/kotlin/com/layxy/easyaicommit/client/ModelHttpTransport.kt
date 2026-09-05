package com.layxy.easyaicommit.client

import com.layxy.easyaicommit.EasyAiCommitBundle.message
import com.layxy.easyaicommit.model.ModelProfile
import com.layxy.easyaicommit.model.ProxyMode
import com.layxy.easyaicommit.model.ProxyType
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.io.HttpRequests
import com.intellij.util.net.ssl.CertificateManager
import okhttp3.ConnectionPool
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.Reader
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

internal data class ModelHttpResponse(val status: Int, val body: String)

internal class ModelHttpTransport(
    private val executor: NetworkRequestExecutor = NetworkRequestExecutor.getInstance(),
    private val customClientBuilder: () -> OkHttpClient.Builder = {
        val certificates = CertificateManager.getInstance()
        OkHttpClient.Builder().sslSocketFactory(certificates.sslContext.socketFactory, certificates.trustManager)
    },
) {
    fun postJson(
        profile: ModelProfile,
        url: String,
        body: String,
        headers: Map<String, String>,
        proxyPassword: String,
        indicator: ProgressIndicator?,
    ): ModelHttpResponse {
        profile.proxy.validate()
        require(profile.requestTimeoutSeconds in 5..300) { message("network.validation.timeout") }
        if (profile.proxy.requiresPassword && proxyPassword.isBlank()) {
            throw ModelRequestException(message("proxy.error.passwordMissing"))
        }
        try {
            return executor.execute(profile.requestTimeoutSeconds * 1000L, { indicator?.checkCanceled() }) { cancellation ->
                cancellation.checkCanceled()
                val response = if (profile.proxy.mode == ProxyMode.CUSTOM) {
                    customRequest(profile, url, body, headers, proxyPassword, cancellation)
                } else {
                    ideRequest(profile, url, body, headers, cancellation)
                }
                if (response.status == 407) throw ModelRequestException(message("proxy.error.authentication"))
                response
            }
        } catch (error: ProcessCanceledException) {
            throw error
        } catch (error: CancellationException) {
            throw ProcessCanceledException(error)
        } catch (error: TimeoutException) {
            throw ModelRequestException(message("network.error.timeout", profile.name, profile.requestTimeoutSeconds), error)
        } catch (error: SocketTimeoutException) {
            throw ModelRequestException(message("network.error.connectionTimeout"), error)
        } catch (error: InterruptedIOException) {
            throw ModelRequestException(message("network.error.timeout", profile.name, profile.requestTimeoutSeconds), error)
        } catch (error: SSLException) {
            throw ModelRequestException(message("network.error.tls"), error)
        } catch (error: ConnectException) {
            throw ModelRequestException(message(if (profile.proxy.mode == ProxyMode.CUSTOM) "proxy.error.unreachable" else "network.error.unreachable"), error)
        } catch (error: UnknownHostException) {
            throw ModelRequestException(message("network.error.dns"), error)
        } catch (error: RejectedExecutionException) {
            throw ModelRequestException(message("network.error.busy"), error)
        }
    }

    private fun ideRequest(
        profile: ModelProfile, url: String, body: String, headers: Map<String, String>, cancellation: RequestCancellation,
    ): ModelHttpResponse = HttpRequests.post(url, HttpRequests.JSON_CONTENT_TYPE)
        .connectTimeout(minOf(15, profile.requestTimeoutSeconds) * 1000)
        .readTimeout(profile.requestTimeoutSeconds * 1000)
        .useProxy(profile.proxy.mode == ProxyMode.IDE)
        .redirectLimit(1) // Do not forward model credentials to a redirected endpoint.
        .gzip(true)
        .accept(HttpRequests.JSON_CONTENT_TYPE)
        .throwStatusCodeException(false)
        .tuner { connection ->
            val http = connection as HttpURLConnection
            cancellation.onCancel { http.disconnect() }
            http.instanceFollowRedirects = false
            headers.forEach(http::setRequestProperty)
        }
        .connect { request ->
            cancellation.checkCanceled()
            request.write(body)
            val http = request.connection as HttpURLConnection
            val status = http.responseCode
            val stream = if (status in 200..299) request.inputStream else http.errorStream
            val response = stream?.reader(Charsets.UTF_8)?.use { readBounded(it, cancellation) }.orEmpty()
            ModelHttpResponse(status, response)
        }

    private fun customRequest(
        profile: ModelProfile, url: String, body: String, headers: Map<String, String>,
        proxyPassword: String, cancellation: RequestCancellation,
    ): ModelHttpResponse {
        val proxy = profile.proxy
        val builder = customClientBuilder()
            .proxy(Proxy(if (proxy.type == ProxyType.HTTP) Proxy.Type.HTTP else Proxy.Type.SOCKS,
                InetSocketAddress(proxy.host.removeSurrounding("[", "]"), proxy.port)))
            .connectTimeout(minOf(15, profile.requestTimeoutSeconds).toLong(), TimeUnit.SECONDS)
            .readTimeout(profile.requestTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(profile.requestTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .callTimeout(profile.requestTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
        builder.proxyAuthenticator { route, response ->
                val preemptive = response.challenges().any { it.scheme == "OkHttp-Preemptive" }
                if (!proxy.requiresPassword) {
                    if (preemptive) return@proxyAuthenticator null
                    throw ModelRequestException(message("proxy.error.authentication"))
                }
                // Only answer for this explicit HTTP proxy; never replace the model's Authorization.
                val address = route?.proxy?.address() as? InetSocketAddress
                if (address?.port != proxy.port ||
                    !address.hostString.equals(proxy.host.removeSurrounding("[", "]"), ignoreCase = true)
                ) return@proxyAuthenticator null
                if (response.request.header("Proxy-Authorization") != null) {
                    throw ModelRequestException(message("proxy.error.authentication"))
                }
                if (!preemptive && response.challenges().none { it.scheme.equals("Basic", true) }) {
                    throw ModelRequestException(message("proxy.error.authScheme"))
                }
                response.request.newBuilder()
                    .header("Proxy-Authorization", Credentials.basic(proxy.username, proxyPassword)).build()
        }
        val client = builder.build()
        val request = Request.Builder().url(url).post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "application/json")
            .apply { headers.forEach { (key, value) -> header(key, value) } }.build()
        val call = client.newCall(request)
        cancellation.onCancel(call::cancel)
        try {
            return call.execute().use { response ->
                ModelHttpResponse(response.code, response.body?.charStream()?.use { readBounded(it, cancellation) }.orEmpty())
            }
        } finally {
            client.connectionPool.evictAll()
        }
    }

    private fun readBounded(reader: Reader, cancellation: RequestCancellation): String {
        val result = StringBuilder()
        val buffer = CharArray(4096)
        while (true) {
            cancellation.checkCanceled()
            val count = reader.read(buffer)
            if (count < 0) return result.toString()
            if (result.length + count > 100_000) throw ModelRequestException(message("network.error.responseTooLarge"))
            result.append(buffer, 0, count)
        }
    }
}
