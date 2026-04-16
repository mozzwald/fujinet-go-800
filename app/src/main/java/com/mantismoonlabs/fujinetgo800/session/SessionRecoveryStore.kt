package com.mantismoonlabs.fujinetgo800.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionRecoveryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "session_recovery",
)

internal object SessionRecoveryPreferenceKeys {
    val lastLaunchMode = stringPreferencesKey("last_launch_mode")
    val hadRunningSession = booleanPreferencesKey("had_running_session")
    val lastSessionToken = longPreferencesKey("last_session_token")
}

data class SessionRecoverySnapshot(
    val lastLaunchMode: LaunchMode = LaunchMode.FUJINET_ENABLED,
    val hadRunningSession: Boolean = false,
    val lastSessionToken: Long? = null,
)

class SessionRecoveryStore private constructor(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.sessionRecoveryDataStore)

    val snapshot: Flow<SessionRecoverySnapshot> = dataStore.data.map { preferences ->
        SessionRecoverySnapshot(
            lastLaunchMode = preferences.getLaunchMode(),
            hadRunningSession = preferences[SessionRecoveryPreferenceKeys.hadRunningSession] ?: false,
            lastSessionToken = preferences[SessionRecoveryPreferenceKeys.lastSessionToken],
        )
    }

    suspend fun markRunning(launchMode: LaunchMode, sessionToken: Long) {
        dataStore.edit { preferences ->
            preferences[SessionRecoveryPreferenceKeys.lastLaunchMode] = launchMode.name
            preferences[SessionRecoveryPreferenceKeys.hadRunningSession] = true
            preferences[SessionRecoveryPreferenceKeys.lastSessionToken] = sessionToken
        }
    }

    suspend fun markRecovered() {
        dataStore.edit { preferences ->
            preferences[SessionRecoveryPreferenceKeys.hadRunningSession] = false
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            val lastLaunchMode = preferences.getLaunchMode()
            preferences.clear()
            preferences[SessionRecoveryPreferenceKeys.lastLaunchMode] = lastLaunchMode.name
            preferences[SessionRecoveryPreferenceKeys.hadRunningSession] = false
        }
    }

    suspend fun currentSnapshot(): SessionRecoverySnapshot = snapshot.first()

    companion object {
        internal fun createForTest(
            produceFile: () -> File,
            scope: CoroutineScope,
        ): SessionRecoveryStore {
            return SessionRecoveryStore(
                dataStore = PreferenceDataStoreFactory.create(
                    scope = scope,
                    produceFile = produceFile,
                ),
            )
        }
    }
}

private fun Preferences.getLaunchMode(): LaunchMode {
    val storedValue = this[SessionRecoveryPreferenceKeys.lastLaunchMode]
    return LaunchMode.entries.firstOrNull { launchMode ->
        launchMode.name == storedValue
    } ?: LaunchMode.FUJINET_ENABLED
}
