package com.mantismoonlabs.fujinetgo800.fujinet

import android.content.Context
import com.mantismoonlabs.fujinetgo800.core.FujiNetNative
import com.mantismoonlabs.fujinetgo800.storage.RuntimePaths
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface FujiNetServiceConnector {
    suspend fun start(request: FujiNetLaunchRequest): FujiNetRuntimeState.Ready
    suspend fun isHealthy(): Boolean
    suspend fun stop()
}

class FujiNetServiceBridge(
    private val runtimePaths: RuntimePaths,
    private val connector: FujiNetServiceConnector,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private val mutableState = MutableStateFlow<FujiNetRuntimeState>(FujiNetRuntimeState.Stopped)

    val state: StateFlow<FujiNetRuntimeState> = mutableState.asStateFlow()

    suspend fun start(): FujiNetRuntimeState.Ready {
        val current = state.value
        if (current is FujiNetRuntimeState.Ready) {
            if (connector.isHealthy()) {
                return current
            }
            connector.stop()
            mutableState.value = FujiNetRuntimeState.Stopped
        }

        runtimePaths.ensureDirectories()
        val request = FujiNetLaunchRequest(
            runtimeRoot = runtimePaths.fujiNetRuntimeDirectory,
            dataDirectory = runtimePaths.fujiNetDataDirectory,
            sdDirectory = runtimePaths.fujiNetSdDirectory,
            configFile = runtimePaths.fujiNetConfigFile,
        )

        mutableState.value = FujiNetRuntimeState.Starting(request.runtimeRoot)
        debugFailureMode?.let { mode ->
            val error = when (mode) {
                FujiNetDebugFailureMode.ASSET_INIT_FAILURE -> FujiNetStartupException(
                    failureMode = mode,
                    message = "Debug FujiNet asset initialization failure",
                )

                FujiNetDebugFailureMode.SERVICE_START_FAILURE -> FujiNetStartupException(
                    failureMode = mode,
                    message = "Debug FujiNet service startup failure",
                )

                FujiNetDebugFailureMode.READINESS_TIMEOUT -> FujiNetStartupException(
                    failureMode = mode,
                    message = "Timed out waiting for FujiNet readiness (debug)",
                )
            }
            mutableState.value = FujiNetRuntimeState.Failed(
                runtimeRoot = request.runtimeRoot,
                message = error.message ?: "FujiNet runtime failed to start",
            )
            throw error
        }
        return runCatching { connector.start(request) }
            .onSuccess { ready ->
                mutableState.value = ready
            }
            .onFailure { error ->
                mutableState.value = FujiNetRuntimeState.Failed(
                    runtimeRoot = request.runtimeRoot,
                    message = error.message ?: "FujiNet runtime failed to start",
                )
            }
            .getOrThrow()
    }

    suspend fun isHealthy(): Boolean {
        val current = state.value
        if (current !is FujiNetRuntimeState.Ready) {
            return false
        }
        val healthy = connector.isHealthy()
        if (!healthy) {
            mutableState.value = FujiNetRuntimeState.Stopped
        }
        return healthy
    }

    suspend fun stop() {
        connector.stop()
        mutableState.value = FujiNetRuntimeState.Stopped
    }

    companion object {
        @Volatile
        private var debugFailureMode: FujiNetDebugFailureMode? = null

        fun forContext(
            context: Context,
            runtimePaths: RuntimePaths,
        ): FujiNetServiceBridge {
            return FujiNetServiceBridge(
                runtimePaths = runtimePaths,
                connector = NativeFujiNetServiceConnector(context.applicationContext),
            )
        }

        fun setDebugFailureModeForTesting(mode: FujiNetDebugFailureMode?) {
            debugFailureMode = mode
        }

        internal fun debugFailureModeForTesting(): FujiNetDebugFailureMode? = debugFailureMode
    }
}

private class NativeFujiNetServiceConnector(
    context: Context,
) : FujiNetServiceConnector {
    override suspend fun start(request: FujiNetLaunchRequest): FujiNetRuntimeState.Ready {
        if (FujiNetNative.isRuntimeRunning()) {
            return FujiNetRuntimeState.Ready(
                runtimeRoot = request.runtimeRoot,
                listenPort = request.listenPort,
            )
        }
        val started = FujiNetNative.startRuntime(
            runtimeRootPath = request.runtimeRoot.absolutePath,
            configPath = request.configFile.absolutePath,
            sdPath = request.sdDirectory.absolutePath,
            dataPath = request.dataDirectory.absolutePath,
            listenPort = request.listenPort,
        )
        if (!started) {
            throw IOException(FujiNetNative.lastErrorMessage() ?: "FujiNet runtime failed to start")
        }
        return FujiNetRuntimeState.Ready(
            runtimeRoot = request.runtimeRoot,
            listenPort = request.listenPort,
        )
    }

    override suspend fun isHealthy(): Boolean = FujiNetNative.isRuntimeRunning()

    override suspend fun stop() {
        if (FujiNetNative.isRuntimeRunning()) {
            FujiNetNative.stopRuntime()
        }
    }
}
