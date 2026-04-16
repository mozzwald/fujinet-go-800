package com.mantismoonlabs.fujinetgo800.fujinet

import com.mantismoonlabs.fujinetgo800.storage.RuntimePaths
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FujiNetServiceBridgeTest {
    @Test
    fun bridgePublishesReadyBeforeSessionStart() = runTest {
        val runtimePaths = RuntimePaths(createTempDirectory(prefix = "fujinet-bridge-test").toFile())
        val dispatcher = StandardTestDispatcher(testScheduler)
        val connector = RecordingConnector()
        val bridge = FujiNetServiceBridge(
            runtimePaths = runtimePaths,
            connector = connector,
            dispatcher = dispatcher,
        )

        val states = mutableListOf<FujiNetRuntimeState>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) {
            bridge.state.take(3).toList(states)
        }

        val ready = bridge.start()
        advanceUntilIdle()

        assertEquals(
            listOf(
                FujiNetRuntimeState.Stopped,
                FujiNetRuntimeState.Starting(runtimePaths.fujiNetRuntimeDirectory),
                FujiNetRuntimeState.Ready(
                    runtimeRoot = runtimePaths.fujiNetRuntimeDirectory,
                    listenPort = FujiNetLaunchRequest.DEFAULT_LISTEN_PORT,
                ),
            ),
            states,
        )
        assertEquals(
            FujiNetRuntimeState.Ready(
                runtimeRoot = runtimePaths.fujiNetRuntimeDirectory,
                listenPort = FujiNetLaunchRequest.DEFAULT_LISTEN_PORT,
            ),
            ready,
        )

        bridge.stop()
        assertEquals(1, connector.stopCalls)
        collection.cancel()
    }

    @Test
    fun bridgeUsesStagedRuntimeRoot() = runTest {
        val rootDirectory = createTempDirectory(prefix = "fujinet-runtime-root").toFile()
        val runtimePaths = RuntimePaths(rootDirectory)
        val connector = RecordingConnector()
        val bridge = FujiNetServiceBridge(
            runtimePaths = runtimePaths,
            connector = connector,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        bridge.start()

        val request = connector.startRequests.single()
        assertEquals(runtimePaths.fujiNetRuntimeDirectory, request.runtimeRoot)
        assertEquals(runtimePaths.fujiNetDataDirectory, request.dataDirectory)
        assertEquals(runtimePaths.fujiNetSdDirectory, request.sdDirectory)
        assertEquals(runtimePaths.fujiNetConfigFile, request.configFile)
        assertTrue(request.runtimeRoot.path.endsWith("${File.separator}fujinet"))
    }

    @Test
    fun isHealthyReturnsFalseAfterConnectorReportsDisconnect() = runTest {
        val runtimePaths = RuntimePaths(createTempDirectory(prefix = "fujinet-health-test").toFile())
        val connector = RecordingConnector()
        val bridge = FujiNetServiceBridge(
            runtimePaths = runtimePaths,
            connector = connector,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        bridge.start()
        connector.healthy = false

        assertEquals(false, bridge.isHealthy())
        assertEquals(FujiNetRuntimeState.Stopped, bridge.state.value)
    }

    private class RecordingConnector : FujiNetServiceConnector {
        val startRequests = mutableListOf<FujiNetLaunchRequest>()
        var stopCalls = 0
        var healthy = true

        override suspend fun start(request: FujiNetLaunchRequest): FujiNetRuntimeState.Ready {
            startRequests += request
            return FujiNetRuntimeState.Ready(
                runtimeRoot = request.runtimeRoot,
                listenPort = request.listenPort,
            )
        }

        override suspend fun isHealthy(): Boolean = healthy

        override suspend fun stop() {
            stopCalls += 1
        }
    }
}
