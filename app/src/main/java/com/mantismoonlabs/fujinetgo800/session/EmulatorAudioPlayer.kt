package com.mantismoonlabs.fujinetgo800.session

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.mantismoonlabs.fujinetgo800.core.EmulatorNative
import kotlin.math.max
import kotlin.math.min

internal class EmulatorAudioPlayer(
    private val sampleRate: Int,
) {
    private data class PlaybackState(
        val playing: Boolean,
        val hostMuted: Boolean,
        val focusMuted: Boolean,
        val ducked: Boolean,
        val userVolume: Float,
    )

    private val minBufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    ).let { reportedSize ->
        if (reportedSize > 0) {
            reportedSize
        } else {
            (sampleRate / 20) * BytesPerSample
        }
    }

    private val trackBufferSize = max(minBufferSize, (sampleRate / 50) * BytesPerSample)
    private val transferBufferSamples = max(
        sampleRate / 100,
        min(trackBufferSize / BytesPerSample / 2, sampleRate / 60),
    )

    private val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(trackBufferSize)
        .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
        }
        .build()

    @Volatile
    private var running = false

    @Volatile
    private var playbackState = PlaybackState(
        playing = false,
        hostMuted = false,
        focusMuted = false,
        ducked = false,
        userVolume = 0.7f,
    )

    private var audioThread: Thread? = null

    fun start() {
        if (running) {
            return
        }
        Log.i(
            LogTag,
            "start sampleRate=$sampleRate minBufferSize=$minBufferSize trackBufferSize=$trackBufferSize transferBufferSamples=$transferBufferSamples",
        )
        running = true
        audioThread = Thread(::runLoop, "EmulatorAudioPlayer").also { it.start() }
    }

    fun resume() {
        Log.i(LogTag, "resume")
        updatePlaybackState { it.copy(playing = true) }
    }

    fun pause() {
        Log.i(LogTag, "pause")
        updatePlaybackState { it.copy(playing = false) }
    }

    fun setMuted(muted: Boolean) {
        Log.i(LogTag, "setMuted host=$muted")
        updatePlaybackState { it.copy(hostMuted = muted) }
    }

    fun setFocusMuted(muted: Boolean) {
        Log.i(LogTag, "setFocusMuted focus=$muted")
        updatePlaybackState { it.copy(focusMuted = muted) }
    }

    fun setDucked(ducked: Boolean) {
        Log.i(LogTag, "setDucked ducked=$ducked")
        updatePlaybackState { it.copy(ducked = ducked) }
    }

    fun setUserVolume(volume: Float) {
        val normalizedVolume = volume.coerceIn(0f, 1f)
        Log.i(LogTag, "setUserVolume user=$normalizedVolume")
        updatePlaybackState { it.copy(userVolume = normalizedVolume) }
    }

    fun stop() {
        running = false
        pause()
        audioThread?.join(500)
        audioThread = null
        audioTrack.release()
    }

    private fun runLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val frameBuffer = ShortArray(transferBufferSamples)
        var trackPlaying = false
        var appliedState = PlaybackState(
            playing = false,
            hostMuted = false,
            focusMuted = false,
            ducked = false,
            userVolume = 0.7f,
        )
        while (running) {
            val desiredState = playbackState
            val desiredVolume = when {
                desiredState.hostMuted || desiredState.focusMuted -> 0f
                desiredState.ducked -> desiredState.userVolume * DuckVolume
                else -> desiredState.userVolume
            }
            val appliedVolume = when {
                appliedState.hostMuted || appliedState.focusMuted -> 0f
                appliedState.ducked -> appliedState.userVolume * DuckVolume
                else -> appliedState.userVolume
            }
            if (appliedVolume != desiredVolume) {
                audioTrack.setVolume(desiredVolume)
                appliedState = appliedState.copy(
                    hostMuted = desiredState.hostMuted,
                    focusMuted = desiredState.focusMuted,
                    ducked = desiredState.ducked,
                    userVolume = desiredState.userVolume,
                )
            }

            if (!desiredState.playing) {
                if (trackPlaying || audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.pause()
                    audioTrack.flush()
                    trackPlaying = false
                }
                appliedState = appliedState.copy(playing = false)
                SystemClock.sleep(5)
                continue
            }

            if (!trackPlaying || audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                Log.i(LogTag, "play track state=${audioTrack.playState}")
                audioTrack.play()
                trackPlaying = true
                appliedState = appliedState.copy(playing = true)
            }

            EmulatorNative.fillAudioBuffer(frameBuffer)
            val written = audioTrack.write(frameBuffer, 0, frameBuffer.size, AudioTrack.WRITE_BLOCKING)
            if (written <= 0) {
                Log.w(LogTag, "write returned $written playState=${audioTrack.playState}")
                trackPlaying = audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING
                SystemClock.sleep(5)
            }
        }

        if (trackPlaying || audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.pause()
            audioTrack.flush()
        }
    }

    private fun updatePlaybackState(update: (PlaybackState) -> PlaybackState) {
        playbackState = update(playbackState)
    }

    private companion object {
        private const val LogTag = "EmulatorAudio"
        private const val DuckVolume = 0.2f
        private const val BytesPerSample = 2
    }
}
