package com.mantismoonlabs.fujinetgo800.fujinet

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import com.mantismoonlabs.fujinetgo800.core.FujiNetNative

class FujiNetService : Service() {
    private val messenger by lazy {
        Messenger(
            IncomingHandler(),
        )
    }

    override fun onBind(intent: Intent): IBinder = messenger.binder

    override fun onDestroy() {
        FujiNetNative.stopRuntime()
        super.onDestroy()
    }

    private inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            when (message.what) {
                MESSAGE_START -> handleStart(message)
                MESSAGE_STOP -> handleStop(message)
                MESSAGE_HEALTH_CHECK -> handleHealthCheck(message)
                else -> super.handleMessage(message)
            }
        }

        private fun handleStart(message: Message) {
            val replyTo = message.replyTo ?: return
            val request = runCatching { message.data.toLaunchRequest() }
                .getOrElse { error ->
                    reply(
                        replyTo = replyTo,
                        what = MESSAGE_FAILED,
                        data = bundleOfError(error.message ?: "Invalid FujiNet start request"),
                    )
                    return
                }

            val started = FujiNetNative.startRuntime(
                runtimeRootPath = request.runtimeRoot.absolutePath,
                configPath = request.configFile.absolutePath,
                sdPath = request.sdDirectory.absolutePath,
                dataPath = request.dataDirectory.absolutePath,
                listenPort = request.listenPort,
            )

            if (started) {
                // Native start now blocks until FujiNet finishes main_setup(...).
                reply(
                    replyTo = replyTo,
                    what = MESSAGE_READY,
                    data = Bundle().apply {
                        putInt(KEY_LISTEN_PORT, request.listenPort)
                    },
                )
            } else {
                reply(
                    replyTo = replyTo,
                    what = MESSAGE_FAILED,
                    data = bundleOfError(
                        FujiNetNative.lastErrorMessage() ?: "FujiNet runtime failed to start",
                    ),
                )
            }
        }

        private fun handleStop(message: Message) {
            FujiNetNative.stopRuntime()
            message.replyTo?.let { replyTo ->
                reply(replyTo = replyTo, what = MESSAGE_STOPPED, data = Bundle.EMPTY)
            }
            stopSelf()
        }

        private fun handleHealthCheck(message: Message) {
            message.replyTo?.let { replyTo ->
                reply(replyTo = replyTo, what = MESSAGE_HEALTHY, data = Bundle.EMPTY)
            }
        }
    }

    private fun Bundle.toLaunchRequest(): FujiNetLaunchRequest {
        val runtimeRoot = requireNotNull(getString(KEY_RUNTIME_ROOT)) { "Missing runtime root" }
        val dataDirectory = requireNotNull(getString(KEY_DATA_DIRECTORY)) { "Missing data directory" }
        val sdDirectory = requireNotNull(getString(KEY_SD_DIRECTORY)) { "Missing SD directory" }
        val configFile = requireNotNull(getString(KEY_CONFIG_FILE)) { "Missing config path" }
        val listenPort = getInt(KEY_LISTEN_PORT, FujiNetLaunchRequest.DEFAULT_LISTEN_PORT)
        return FujiNetLaunchRequest(
            runtimeRoot = java.io.File(runtimeRoot),
            dataDirectory = java.io.File(dataDirectory),
            sdDirectory = java.io.File(sdDirectory),
            configFile = java.io.File(configFile),
            listenPort = listenPort,
        )
    }

    private fun reply(replyTo: Messenger, what: Int, data: Bundle) {
        runCatching {
            replyTo.send(
                Message.obtain(null, what).apply {
                    this.data = data
                },
            )
        }.getOrElse { error ->
            if (error !is RemoteException) {
                throw error
            }
        }
    }

    private fun bundleOfError(message: String): Bundle =
        Bundle().apply { putString(KEY_ERROR_MESSAGE, message) }

    companion object {
        const val MESSAGE_START = 1
        const val MESSAGE_READY = 2
        const val MESSAGE_FAILED = 3
        const val MESSAGE_STOP = 4
        const val MESSAGE_STOPPED = 5
        const val MESSAGE_HEALTH_CHECK = 6
        const val MESSAGE_HEALTHY = 7

        const val KEY_RUNTIME_ROOT = "runtimeRoot"
        const val KEY_DATA_DIRECTORY = "dataDirectory"
        const val KEY_SD_DIRECTORY = "sdDirectory"
        const val KEY_CONFIG_FILE = "configFile"
        const val KEY_LISTEN_PORT = "listenPort"
        const val KEY_ERROR_MESSAGE = "errorMessage"
    }
}
