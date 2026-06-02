package com.jiny.finalalarm.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth")

/**
 * 토큰 저장소. DataStore + 메모리 캐시 이중화.
 * - OkHttp Interceptor·Authenticator에서 동기로 토큰을 빠르게 얻기 위해 메모리 캐시 보유
 * - 초기 로드는 첫 인스턴스 생성 시 한 번만 (runBlocking로 막힘 — Hilt가 lazy로 호출하므로 영향 적음)
 */
@Singleton
class TokenStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val accessKey = stringPreferencesKey("access")
    private val refreshKey = stringPreferencesKey("refresh")
    private val userIdKey = stringPreferencesKey("user_id")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 메모리 캐시 — 동기 접근용. 모든 변경 시 같이 갱신.
    @Volatile private var cachedAccess: String? = null
    @Volatile private var cachedRefresh: String? = null
    @Volatile private var cachedUserId: String? = null

    private val _accessState = MutableStateFlow<String?>(null)
    private val _userIdState = MutableStateFlow<String?>(null)

    val accessTokenFlow: Flow<String?> = _accessState
    val refreshTokenFlow: Flow<String?> = ctx.dataStore.data.map { it[refreshKey] }
    val userIdFlow: Flow<String?> = _userIdState

    init {
        // DataStore 초기 1회 동기 로드. Hilt가 IO 스레드에서 inject하면 영향 적음.
        runBlocking {
            val prefs = ctx.dataStore.data.first()
            cachedAccess = prefs[accessKey]
            cachedRefresh = prefs[refreshKey]
            cachedUserId = prefs[userIdKey]
            _accessState.value = cachedAccess
            _userIdState.value = cachedUserId
        }
    }

    // ---- 동기 접근 (OkHttp 인터셉터·Worker용) ----
    fun accessSync(): String? = cachedAccess
    fun refreshSync(): String? = cachedRefresh
    fun userIdSync(): String? = cachedUserId

    // ---- suspend 접근 (호환 유지) ----
    suspend fun access(): String? = cachedAccess
    suspend fun refresh(): String? = cachedRefresh

    suspend fun save(accessToken: String, refreshToken: String, userId: String? = null) {
        cachedAccess = accessToken
        cachedRefresh = refreshToken
        if (userId != null) cachedUserId = userId
        _accessState.value = accessToken
        if (userId != null) _userIdState.value = userId
        ctx.dataStore.edit {
            it[accessKey] = accessToken
            it[refreshKey] = refreshToken
            if (userId != null) it[userIdKey] = userId
        }
    }

    /** 동기 save — interceptor에서 호출. DataStore 쓰기는 백그라운드. */
    fun saveSync(accessToken: String, refreshToken: String, userId: String? = null) {
        cachedAccess = accessToken
        cachedRefresh = refreshToken
        if (userId != null) cachedUserId = userId
        _accessState.value = accessToken
        if (userId != null) _userIdState.value = userId
        scope.launch {
            ctx.dataStore.edit {
                it[accessKey] = accessToken
                it[refreshKey] = refreshToken
                if (userId != null) it[userIdKey] = userId
            }
        }
    }

    suspend fun updateAccess(accessToken: String) {
        cachedAccess = accessToken
        _accessState.value = accessToken
        ctx.dataStore.edit { it[accessKey] = accessToken }
    }

    suspend fun clear() {
        cachedAccess = null
        cachedRefresh = null
        cachedUserId = null
        _accessState.value = null
        _userIdState.value = null
        ctx.dataStore.edit { it.clear() }
    }

    /** 동기 clear — refresh 실패 시 인터셉터에서 호출. DataStore 쓰기는 백그라운드. */
    fun clearSync() {
        cachedAccess = null
        cachedRefresh = null
        cachedUserId = null
        _accessState.value = null
        _userIdState.value = null
        scope.launch { ctx.dataStore.edit { it.clear() } }
    }
}
