package com.layxy.easyaicommit.client

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProcessCanceledException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Bounds caller wait even when DNS or an IDE connection provider ignores thread interruption. */
@Service(Service.Level.APP)
class NetworkRequestExecutor : Disposable {
    private val workers = pool("request", 8)
    private val cleanup = pool("disconnect", 8)
    private val active = ConcurrentHashMap.newKeySet<RequestCancellation>()

    fun <T> execute(
        timeoutMillis: Long,
        checkCanceled: () -> Unit = {},
        request: (RequestCancellation) -> T,
    ): T {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        val cancellation = RequestCancellation(cleanup)
        val future = FutureTask { request(cancellation) }
        active.add(cancellation)
        try {
            checkCanceled()
            workers.execute(future)
            while (true) {
                checkCanceled()
                cancellation.checkCanceled()
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0) throw TimeoutException()
                try {
                    val result = future.get(minOf(remaining, TimeUnit.MILLISECONDS.toNanos(50)), TimeUnit.NANOSECONDS)
                    checkCanceled()
                    cancellation.checkCanceled()
                    if (System.nanoTime() >= deadline) throw TimeoutException()
                    return result
                } catch (poll: TimeoutException) {
                    if (System.nanoTime() >= deadline) throw poll
                }
            }
        } catch (error: ExecutionException) {
            checkCanceled()
            if (System.nanoTime() >= deadline) throw TimeoutException()
            throw error.cause ?: error
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ProcessCanceledException(error)
        } finally {
            // disconnect() may block; never perform it on the thread responsible for restoring UI.
            cancellation.cancel()
            future.cancel(true)
            workers.remove(future)
            active.remove(cancellation)
        }
    }

    override fun dispose() {
        active.forEach(RequestCancellation::cancel)
        workers.shutdownNow()
        cleanup.shutdown()
    }

    private fun pool(suffix: String, size: Int): ThreadPoolExecutor {
        val sequence = AtomicInteger()
        return ThreadPoolExecutor(
            size, size, 30, TimeUnit.SECONDS, ArrayBlockingQueue(32),
            ThreadFactory { runnable ->
                Thread(runnable, "Easy AI Commit $suffix-${sequence.incrementAndGet()}").apply { isDaemon = true }
            },
        ).apply { allowCoreThreadTimeOut(true) }
    }

    companion object {
        fun getInstance(): NetworkRequestExecutor =
            ApplicationManager.getApplication().getService(NetworkRequestExecutor::class.java)
    }
}

class RequestCancellation(private val cleanup: Executor) {
    private val canceled = AtomicBoolean()
    private val actions = ConcurrentHashMap.newKeySet<() -> Unit>()

    fun checkCanceled() {
        if (canceled.get()) throw CancellationException()
    }

    fun onCancel(action: () -> Unit) {
        actions.add(action)
        if (canceled.get() && actions.remove(action)) dispatch(action)
        checkCanceled()
    }

    fun cancel() {
        canceled.set(true)
        actions.toList().forEach { if (actions.remove(it)) dispatch(it) }
    }

    private fun dispatch(action: () -> Unit) {
        runCatching { cleanup.execute { runCatching(action) } }
    }
}
