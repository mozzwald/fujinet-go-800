package com.mantismoonlabs.fujinetgo800.session

import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionRecoveryStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun persistsLastRunningLaunchMode() = runTest {
        val storeFile = temporaryFolder.newFile("session-recovery.preferences_pb")
        val firstScope = CoroutineScope(coroutineContext + Job())
        val firstStore = SessionRecoveryStore.createForTest(
            produceFile = { storeFile },
            scope = firstScope,
        )

        firstStore.markRunning(
            launchMode = LaunchMode.FUJINET_ENABLED,
            sessionToken = 44L,
        )
        firstScope.coroutineContext[Job]?.cancel()

        val reloadedStore = SessionRecoveryStore.createForTest(
            produceFile = { storeFile },
            scope = CoroutineScope(coroutineContext + Job()),
        )

        assertEquals(
            SessionRecoverySnapshot(
                lastLaunchMode = LaunchMode.FUJINET_ENABLED,
                hadRunningSession = true,
                lastSessionToken = 44L,
            ),
            reloadedStore.snapshot.first(),
        )
    }

    @Test
    fun clearRemovesRecoverySnapshot() = runTest {
        val storeFile = temporaryFolder.newFile("session-recovery-clear.preferences_pb")
        val store = SessionRecoveryStore.createForTest(
            produceFile = { storeFile },
            scope = CoroutineScope(coroutineContext + Job()),
        )

        store.markRunning(
            launchMode = LaunchMode.LOCAL_ONLY,
            sessionToken = 88L,
        )
        store.clear()

        val clearedSnapshot = store.snapshot.first()

        assertEquals(LaunchMode.LOCAL_ONLY, clearedSnapshot.lastLaunchMode)
        assertFalse(clearedSnapshot.hadRunningSession)
        assertEquals(null, clearedSnapshot.lastSessionToken)
    }
}
