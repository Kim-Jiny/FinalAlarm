package com.jiny.finalalarm.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth")

@Singleton
class TokenStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val accessKey = stringPreferencesKey("access")
    private val refreshKey = stringPreferencesKey("refresh")
    private val userIdKey = stringPreferencesKey("user_id")

    val accessTokenFlow: Flow<String?> = ctx.dataStore.data.map { it[accessKey] }
    val refreshTokenFlow: Flow<String?> = ctx.dataStore.data.map { it[refreshKey] }
    val userIdFlow: Flow<String?> = ctx.dataStore.data.map { it[userIdKey] }

    suspend fun access(): String? = accessTokenFlow.first()
    suspend fun refresh(): String? = refreshTokenFlow.first()

    suspend fun save(accessToken: String, refreshToken: String, userId: String? = null) {
        ctx.dataStore.edit {
            it[accessKey] = accessToken
            it[refreshKey] = refreshToken
            if (userId != null) it[userIdKey] = userId
        }
    }

    suspend fun updateAccess(accessToken: String) {
        ctx.dataStore.edit { it[accessKey] = accessToken }
    }

    suspend fun clear() {
        ctx.dataStore.edit { it.clear() }
    }
}
