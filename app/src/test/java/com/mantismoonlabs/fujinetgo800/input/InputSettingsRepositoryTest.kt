package com.mantismoonlabs.fujinetgo800.input

import com.mantismoonlabs.fujinetgo800.settings.ControlMode
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InputSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun defaultControlModeIsKeyboard() = runTest {
        val repository = EmulatorSettingsRepository.createForTest(
            produceFile = { temporaryFolder.newFile("input-settings.preferences_pb") },
            scope = CoroutineScope(coroutineContext + Job()),
        )

        assertEquals(ControlMode.KEYBOARD, repository.settings.first().controlMode)
    }

    @Test
    fun persistsControlMode() = runTest {
        val settingsFile = temporaryFolder.newFile("input-settings.preferences_pb")
        val firstScope = CoroutineScope(coroutineContext + Job())
        val firstRepository = EmulatorSettingsRepository.createForTest(
            produceFile = { settingsFile },
            scope = firstScope,
        )

        firstRepository.updateControlMode(ControlMode.JOYSTICK)
        firstScope.coroutineContext[Job]?.cancel()

        val reloadedRepository = EmulatorSettingsRepository.createForTest(
            produceFile = { settingsFile },
            scope = CoroutineScope(coroutineContext + Job()),
        )

        assertEquals(ControlMode.JOYSTICK, reloadedRepository.settings.first().controlMode)
    }
}
