package com.mantismoonlabs.fujinetgo800.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mantismoonlabs.fujinetgo800.R
import com.mantismoonlabs.fujinetgo800.notifications.NotificationStartupGateState

@Composable
internal fun NotificationPermissionGateScreen(
    gateState: NotificationStartupGateState,
    onPrimaryAction: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (gateState) {
        NotificationStartupGateState.EducationRequired -> stringResource(R.string.notification_gate_title_first_run)
        NotificationStartupGateState.PermissionRequired -> stringResource(R.string.notification_gate_title_permission_required)
        NotificationStartupGateState.AppNotificationsDisabled -> stringResource(R.string.notification_gate_title_settings_required)
        NotificationStartupGateState.Loading,
        NotificationStartupGateState.Ready -> stringResource(R.string.notification_gate_title_first_run)
    }
    val message = when (gateState) {
        NotificationStartupGateState.AppNotificationsDisabled ->
            stringResource(R.string.notification_gate_message_settings_disabled)
        NotificationStartupGateState.EducationRequired,
        NotificationStartupGateState.PermissionRequired,
        NotificationStartupGateState.Loading,
        NotificationStartupGateState.Ready ->
            stringResource(R.string.notification_gate_message_permission_required)
    }
    val primaryLabel = when (gateState) {
        NotificationStartupGateState.AppNotificationsDisabled ->
            stringResource(R.string.notification_gate_primary_open_settings)
        NotificationStartupGateState.EducationRequired,
        NotificationStartupGateState.PermissionRequired,
        NotificationStartupGateState.Loading,
        NotificationStartupGateState.Ready ->
            stringResource(R.string.notification_gate_primary_allow)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.notification_gate_detail),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(primaryLabel)
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.notification_gate_secondary_open_settings))
                }
            }
        }
    }
}
