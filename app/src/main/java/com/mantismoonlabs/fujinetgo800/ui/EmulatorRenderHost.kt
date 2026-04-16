package com.mantismoonlabs.fujinetgo800.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.SystemClock
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mantismoonlabs.fujinetgo800.settings.ScaleMode
import com.mantismoonlabs.fujinetgo800.settings.applyKeepScreenOn
import com.mantismoonlabs.fujinetgo800.settings.destinationRectFor
import com.mantismoonlabs.fujinetgo800.session.SessionCommand
import com.mantismoonlabs.fujinetgo800.session.SessionRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Composable
fun EmulatorRenderHost(
    sessionRepository: SessionRepository,
    scaleMode: ScaleMode = ScaleMode.FIT,
    scanlinesEnabled: Boolean = false,
    keepScreenOn: Boolean = true,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            EmulatorTextureView(
                context = context,
                initialSessionRepository = sessionRepository,
                initialScaleMode = scaleMode,
                initialScanlinesEnabled = scanlinesEnabled,
                initialKeepScreenOn = keepScreenOn,
            )
        },
        update = { view ->
            view.bindSessionRepository(sessionRepository)
            view.bindHostDisplaySettings(
                scaleMode = scaleMode,
                scanlinesEnabled = scanlinesEnabled,
                keepScreenOn = keepScreenOn,
            )
        },
    )
}

private const val EmulatorFrameWidth = 320
private const val EmulatorFrameHeight = 240
private const val TargetFrameTimeMs = 16L

private class EmulatorTextureView(
    context: Context,
    initialSessionRepository: SessionRepository,
    initialScaleMode: ScaleMode,
    initialScanlinesEnabled: Boolean,
    initialKeepScreenOn: Boolean,
) : TextureView(context), TextureView.SurfaceTextureListener {
    private var sessionRepository: SessionRepository = initialSessionRepository
    private var attachedSurface: Surface? = null
    private var renderThread: EmulatorRenderThread? = null
    @Volatile
    private var scaleMode: ScaleMode = initialScaleMode
    @Volatile
    private var scanlinesEnabled: Boolean = initialScanlinesEnabled

    init {
        surfaceTextureListener = this
        isOpaque = true
        bindHostDisplaySettings(
            scaleMode = initialScaleMode,
            scanlinesEnabled = initialScanlinesEnabled,
            keepScreenOn = initialKeepScreenOn,
        )
    }

    fun bindSessionRepository(repository: SessionRepository) {
        sessionRepository = repository
        if (isAvailable) {
            attachSurface(
                width = width.takeIf { it > 0 } ?: this.width,
                height = height.takeIf { it > 0 } ?: this.height,
            )
            startRenderThread()
        }
    }

    fun bindHostDisplaySettings(scaleMode: ScaleMode, scanlinesEnabled: Boolean, keepScreenOn: Boolean) {
        this.scaleMode = scaleMode
        this.scanlinesEnabled = scanlinesEnabled
        applyKeepScreenOn(keepScreenOn = keepScreenOn, setKeepScreenOn = ::setKeepScreenOn)
    }

    fun copyLatestFrame(target: ByteBuffer): Boolean = sessionRepository.copyLatestFrame(target)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isAvailable) {
            attachSurface(
                width = width.takeIf { it > 0 } ?: this.width,
                height = height.takeIf { it > 0 } ?: this.height,
            )
            startRenderThread()
        }
    }

    override fun onDetachedFromWindow() {
        stopRenderThread()
        detachSurface()
        super.onDetachedFromWindow()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        attachSurface(width, height)
        startRenderThread()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        attachSurface(width, height)
        startRenderThread()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopRenderThread()
        detachSurface()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    private fun attachSurface(width: Int, height: Int) {
        val texture = surfaceTexture ?: return
        attachedSurface?.release()
        attachedSurface = Surface(texture)
        sessionRepository.dispatch(
            SessionCommand.AttachSurface(
                surface = attachedSurface ?: return,
                width = width.coerceAtLeast(1),
                height = height.coerceAtLeast(1),
            ),
        )
    }

    private fun detachSurface() {
        sessionRepository.dispatch(SessionCommand.DetachSurface)
        attachedSurface?.release()
        attachedSurface = null
    }

    private fun startRenderThread() {
        if (renderThread?.isAlive == true) {
            return
        }
        renderThread = EmulatorRenderThread(this, { scaleMode }, { scanlinesEnabled }).also { it.start() }
    }

    private fun stopRenderThread() {
        val thread = renderThread ?: return
        thread.requestStop()
        thread.join(500)
        renderThread = null
    }
}

private class EmulatorRenderThread(
    private val textureView: EmulatorTextureView,
    private val scaleModeProvider: () -> ScaleMode,
    private val scanlinesEnabledProvider: () -> Boolean,
) : Thread("EmulatorRenderThread") {
    @Volatile
    private var running = true

    private val frameBuffer = ByteBuffer
        .allocateDirect(EmulatorFrameWidth * EmulatorFrameHeight * 4)
        .order(ByteOrder.nativeOrder())
    private val bitmap = Bitmap.createBitmap(
        EmulatorFrameWidth,
        EmulatorFrameHeight,
        Bitmap.Config.ARGB_8888,
    )
    private val scanlinePaint = Paint().apply {
        color = Color.argb(48, 0, 0, 0)
        style = Paint.Style.FILL
    }

    override fun run() {
        while (running) {
            val frameStart = SystemClock.elapsedRealtime()
            if (!textureView.isAvailable) {
                SystemClock.sleep(TargetFrameTimeMs)
                continue
            }

            frameBuffer.rewind()
            if (!textureView.copyLatestFrame(frameBuffer)) {
                SystemClock.sleep(TargetFrameTimeMs)
                continue
            }
            frameBuffer.rewind()
            bitmap.copyPixelsFromBuffer(frameBuffer)

            val canvas = textureView.lockCanvas()
            if (canvas == null) {
                SystemClock.sleep(TargetFrameTimeMs)
                continue
            }

            try {
                canvas.drawColor(Color.BLACK)
                val destinationRect = destinationRect(
                    scaleMode = scaleModeProvider(),
                    canvasWidth = canvas.width,
                    canvasHeight = canvas.height,
                )
                canvas.drawBitmap(
                    bitmap,
                    null,
                    destinationRect,
                    null,
                )
                if (scanlinesEnabledProvider()) {
                    drawScanlines(canvas, destinationRect)
                }
            } finally {
                textureView.unlockCanvasAndPost(canvas)
            }

            val elapsed = SystemClock.elapsedRealtime() - frameStart
            val delay = TargetFrameTimeMs - elapsed
            if (delay > 0) {
                SystemClock.sleep(delay)
            }
        }
    }

    fun requestStop() {
        running = false
        interrupt()
    }

    private fun drawScanlines(canvas: android.graphics.Canvas, destinationRect: Rect) {
        var y = destinationRect.top
        while (y < destinationRect.bottom) {
            canvas.drawRect(
                destinationRect.left.toFloat(),
                y.toFloat(),
                destinationRect.right.toFloat(),
                (y + 1).toFloat(),
                scanlinePaint,
            )
            y += 2
        }
    }

    private fun destinationRect(
        scaleMode: ScaleMode,
        canvasWidth: Int,
        canvasHeight: Int,
    ): Rect {
        return destinationRectFor(
            scaleMode = scaleMode,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            frameWidth = EmulatorFrameWidth,
            frameHeight = EmulatorFrameHeight,
        )
    }
}
