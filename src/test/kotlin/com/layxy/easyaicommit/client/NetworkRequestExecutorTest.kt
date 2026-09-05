package com.layxy.easyaicommit.client

import com.intellij.openapi.progress.ProcessCanceledException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class NetworkRequestExecutorTest {
    @Test
    fun `deadline releases caller even when a connection provider ignores interruption`() {
        val executor = NetworkRequestExecutor()
        val release = CountDownLatch(1)
        val canceled = CountDownLatch(1)
        val finished = CountDownLatch(1)
        try {
            val started = System.nanoTime()
            assertFailsWith<TimeoutException> {
                executor.execute(200) { token ->
                    token.onCancel { canceled.countDown() }
                    // Simulates a platform DNS/provider call that does not cooperate with cancellation.
                    while (release.count > 0) {
                        try { release.await() } catch (_: InterruptedException) { /* keep waiting */ }
                    }
                    finished.countDown()
                    "late result"
                }
            }
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 2000)
            assertTrue(canceled.await(1, TimeUnit.SECONDS))
            assertEquals("next result", executor.execute(1000) { "next result" })
        } finally {
            release.countDown()
            finished.await(1, TimeUnit.SECONDS)
            executor.dispose()
        }
    }

    @Test
    fun `user cancellation propagates instead of becoming a network failure`() {
        val executor = NetworkRequestExecutor()
        val cancel = AtomicBoolean()
        val timer = Executors.newSingleThreadScheduledExecutor()
        try {
            timer.schedule({ cancel.set(true) }, 150, TimeUnit.MILLISECONDS)
            assertFailsWith<ProcessCanceledException> {
                executor.execute(5000, { if (cancel.get()) throw ProcessCanceledException() }) {
                    CountDownLatch(1).await()
                }
            }
        } finally {
            executor.dispose()
            timer.shutdownNow()
        }
    }

    @Test
    fun `resource registered after cancellation is still disconnected`() {
        val cleaned = AtomicBoolean()
        val token = RequestCancellation { it.run() }
        token.cancel()
        assertFailsWith<java.util.concurrent.CancellationException> { token.onCancel { cleaned.set(true) } }
        assertTrue(cleaned.get())
    }
}
