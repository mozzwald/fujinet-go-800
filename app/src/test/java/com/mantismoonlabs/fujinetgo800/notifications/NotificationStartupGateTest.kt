package com.mantismoonlabs.fujinetgo800.notifications

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationStartupGateTest {
    @Test
    fun firstRunWithoutPermissionShowsEducation() {
        val state = determineNotificationStartupGateState(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            runtimePermissionGranted = false,
            appNotificationsEnabled = true,
            educationSeen = false,
        )

        assertEquals(NotificationStartupGateState.EducationRequired, state)
    }

    @Test
    fun returningUserWithoutPermissionShowsPermissionGate() {
        val state = determineNotificationStartupGateState(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            runtimePermissionGranted = false,
            appNotificationsEnabled = true,
            educationSeen = true,
        )

        assertEquals(NotificationStartupGateState.PermissionRequired, state)
    }

    @Test
    fun grantedPermissionButAppNotificationsDisabledShowsSettingsGate() {
        val state = determineNotificationStartupGateState(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            runtimePermissionGranted = true,
            appNotificationsEnabled = false,
            educationSeen = true,
        )

        assertEquals(NotificationStartupGateState.AppNotificationsDisabled, state)
    }

    @Test
    fun preTiramisuStillBlocksWhenAppNotificationsAreDisabled() {
        val state = determineNotificationStartupGateState(
            sdkInt = Build.VERSION_CODES.S_V2,
            runtimePermissionGranted = true,
            appNotificationsEnabled = false,
            educationSeen = false,
        )

        assertEquals(NotificationStartupGateState.AppNotificationsDisabled, state)
    }

    @Test
    fun readyWhenPermissionAndAppNotificationsAreAvailable() {
        val state = determineNotificationStartupGateState(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            runtimePermissionGranted = true,
            appNotificationsEnabled = true,
            educationSeen = true,
        )

        assertEquals(NotificationStartupGateState.Ready, state)
    }
}
