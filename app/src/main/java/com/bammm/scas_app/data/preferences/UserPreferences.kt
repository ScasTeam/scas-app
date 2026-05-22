package com.bammm.scas_app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scas_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val USER_ROLE_KEY = stringPreferencesKey("user_role")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { prefs -> prefs[TOKEN_KEY] }
    val userName: Flow<String?> = context.dataStore.data.map { prefs -> prefs[USER_NAME_KEY] }
    val userRole: Flow<String?> = context.dataStore.data.map { prefs -> prefs[USER_ROLE_KEY] }
    
    fun getTokenSync(): String? {
        return runBlocking {
            context.dataStore.data.first()[TOKEN_KEY]
        }
    }

    suspend fun saveAuthData(token: String, name: String, email: String, role: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_NAME_KEY] = name
            prefs[USER_EMAIL_KEY] = email
            prefs[USER_ROLE_KEY] = role
        }
    }
    
    suspend fun clearAuthData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
