package com.mantismoonlabs.fujinetgo800.session

import com.mantismoonlabs.fujinetgo800.core.EmulatorNative
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

internal class EmulationFrameProducer(
    private val frameWidth: Int = 320,
    private val frameHeight: Int = 240,
    private val targetFrameTimeMs: Long = 16L,
    private val renderFrame: (ByteBuffer) -> Unit = EmulatorNative::renderFrame,
) {
    private val frameSizeBytes = frameWidth * frameHeight * 4
    private val workingBuffer = ByteBuffer
        .allocateDirect(frameSizeBytes)
        .order(ByteOrder.nativeOrder())
    private val latestFrame = ByteArray(frameSizeBytes)
    private val frameLock = Any()

    @Volatile
    private var running = false

    @Volatile
    private var hasFrame = false

    private var renderThread: Thread? = null

    fun start() {
        if (running) {
            return
        }
        synchronized(frameLock) {
            latestFrame.fill(0)
            hasFrame = false
        }
        running = true
        renderThread = Thread(::runLoop, "EmulationFrameProducer").also { it.start() }
    }

    fun stop() {
        running = false
        val thread = renderThread ?: return
        thread.interrupt()
        thread.join(500)
        renderThread = null
        synchronized(frameLock) {
            latestFrame.fill(0)
            hasFrame = false
        }
    }

    fun copyLatestFrame(target: ByteBuffer): Boolean {
        synchronized(frameLock) {
            if (!hasFrame || target.capacity() < latestFrame.size) {
                return false
            }
            target.rewind()
            target.put(latestFrame)
            target.rewind()
            return true
        }
    }

    private fun runLoop() {
        while (running) {
            val frameStartNanos = System.nanoTime()
            workingBuffer.rewind()
            renderFrame(workingBuffer)
            workingBuffer.rewind()
            synchronized(frameLock) {
                workingBuffer.get(latestFrame)
                workingBuffer.rewind()
                hasFrame = true
            }

            val elapsedMs = (System.nanoTime() - frameStartNanos) / 1_000_000L
            val sleepMs = max(0L, targetFrameTimeMs - elapsedMs)
            if (sleepMs > 0L) {
                try {
                    Thread.sleep(sleepMs)
                } catch (_: InterruptedException) {
                    // Stop requests interrupt the sleep; the loop condition handles shutdown.
                }
            }
        }
    }
}
