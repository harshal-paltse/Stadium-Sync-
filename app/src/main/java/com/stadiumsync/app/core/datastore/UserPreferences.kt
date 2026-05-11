package com.stadiumsync.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stadium_sync_prefs")

data class UserPreferencesData(
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val criticalAlertsEnabled: Boolean = true,
    val syncIntervalMinutes: Int = 5,
    val lastSyncTimestamp: Long = 0L,
    val cachedUserToken: String = "",
    val cachedUserName: String = "",
    val cachedUserRole: String = "OPERATOR",
    val cachedUserEmail: String = "",
    val cachedUserBadgeId: String = "",
    val cachedUserDepartment: String = "",
    val apiBaseUrl: String = "http://10.0.2.2:8000"
)

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val store = context.dataStore

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val CRITICAL_ALERTS = booleanPreferencesKey("critical_alerts")
        val SYNC_INTERVAL = intPreferencesKey("sync_interval_min")
        val LAST_SYNC = longPreferencesKey("last_sync_time")
        val USER_TOKEN = stringPreferencesKey("user_token")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_BADGE = stringPreferencesKey("user_badge")
        val USER_DEPT = stringPreferencesKey("user_dept")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
    }

    val preferences: Flow<UserPreferencesData> = store.data.map { p ->
        UserPreferencesData(
            isDarkMode = p[Keys.DARK_MODE] ?: false,
            notificationsEnabled = p[Keys.NOTIFICATIONS] ?: true,
            criticalAlertsEnabled = p[Keys.CRITICAL_ALERTS] ?: true,
            syncIntervalMinutes = p[Keys.SYNC_INTERVAL] ?: 5,
            lastSyncTimestamp = p[Keys.LAST_SYNC] ?: 0L,
            cachedUserToken = p[Keys.USER_TOKEN] ?: "",
            cachedUserName = p[Keys.USER_NAME] ?: "",
            cachedUserRole = p[Keys.USER_ROLE] ?: "OPERATOR",
            cachedUserEmail = p[Keys.USER_EMAIL] ?: "",
            cachedUserBadgeId = p[Keys.USER_BADGE] ?: "",
            cachedUserDepartment = p[Keys.USER_DEPT] ?: "",
            apiBaseUrl = p[Keys.API_BASE_URL] ?: "http://10.0.2.2:8000"
        )
    }

    val isDarkMode: Flow<Boolean> = store.data.map { it[Keys.DARK_MODE] ?: false }

    suspend fun setDarkMode(enabled: Boolean) = store.edit { it[Keys.DARK_MODE] = enabled }
    suspend fun setNotifications(enabled: Boolean) = store.edit { it[Keys.NOTIFICATIONS] = enabled }
    suspend fun setCriticalAlerts(enabled: Boolean) = store.edit { it[Keys.CRITICAL_ALERTS] = enabled }
    suspend fun setSyncInterval(minutes: Int) = store.edit { it[Keys.SYNC_INTERVAL] = minutes }
    suspend fun setLastSync(time: Long) = store.edit { it[Keys.LAST_SYNC] = time }

    suspend fun saveUserSession(token: String, name: String, role: String, email: String = "", badgeId: String = "", department: String = "") =
        store.edit {
            it[Keys.USER_TOKEN] = token; it[Keys.USER_NAME] = name; it[Keys.USER_ROLE] = role
            it[Keys.USER_EMAIL] = email; it[Keys.USER_BADGE] = badgeId; it[Keys.USER_DEPT] = department
        }

    suspend fun clearSession() = store.edit {
        it.remove(Keys.USER_TOKEN); it.remove(Keys.USER_NAME); it.remove(Keys.USER_ROLE)
        it.remove(Keys.USER_EMAIL); it.remove(Keys.USER_BADGE); it.remove(Keys.USER_DEPT)
    }

    suspend fun setApiBaseUrl(url: String) = store.edit { it[Keys.API_BASE_URL] = url }
}
