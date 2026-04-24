package com.mantismoonlabs.fujinetgo800.notifications

import android.os.Build

internal sealed interface NotificationStartupGateState {
    data object Loading : NotificationStartupGateState
    data object Ready : NotificationStartupGateState
    data object EducationRequired : NotificationStartupGateState
    data object PermissionRequired : NotificationStartupGateState
    data object AppNotificationsDisabled : NotificationStartupGateState
}

internal fun determineNotificationStartupGateState(
    sdkInt: Int,
    runtimePermissionGranted: Boolean,
    appNotificationsEnabled: Boolean,
    educationSeen: Boolean,
): NotificationStartupGateState {
    val needsRuntimePermission = sdkInt >= Build.VERSION_CODES.TIRAMISU && !runtimePermissionGranted
    return when {
        needsRuntimePermission && !educationSeen -> NotificationStartupGateState.EducationRequired
        needsRuntimePermission -> NotificationStartupGateState.PermissionRequired
        !appNotificationsEnabled -> NotificationStartupGateState.AppNotificationsDisabled
        else -> NotificationStartupGateState.Ready
    }
}
