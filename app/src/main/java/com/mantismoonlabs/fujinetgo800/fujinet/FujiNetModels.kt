package com.mantismoonlabs.fujinetgo800.fujinet

import java.io.File
import java.io.IOException

sealed interface FujiNetRuntimeState {
    data object Stopped : FujiNetRuntimeState
    data class Starting(val runtimeRoot: File) : FujiNetRuntimeState
    data class Ready(
        val runtimeRoot: File,
        val listenPort: Int,
    ) : FujiNetRuntimeState

    data class Failed(
        val runtimeRoot: File,
        val message: String,
    ) : FujiNetRuntimeState
}

data class FujiNetLaunchRequest(
    val runtimeRoot: File,
    val dataDirectory: File,
    val sdDirectory: File,
    val configFile: File,
    val listenPort: Int = DEFAULT_LISTEN_PORT,
) {
    companion object {
        const val DEFAULT_LISTEN_PORT = 9997
    }
}

enum class FujiNetDebugFailureMode {
    ASSET_INIT_FAILURE,
    SERVICE_START_FAILURE,
    READINESS_TIMEOUT,
}

class FujiNetStartupException(
    val failureMode: FujiNetDebugFailureMode,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)
