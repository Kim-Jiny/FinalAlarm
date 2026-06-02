package com.jiny.finalalarm.core.network

import com.jiny.finalalarm.BuildConfig
import com.jiny.finalalarm.core.auth.TokenStore
import com.jiny.finalalarm.data.api.AuthResponse
import com.jiny.finalalarm.data.api.RefreshReq
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

/**
 * 401 응답 시 refresh token으로 access 갱신 후 재시도.
 * OkHttp Authenticator는 동기 API → ReentrantLock으로 중복 refresh 방지.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
) : Authenticator {

    private val lock = ReentrantLock()
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().build() // refresh 호출용. 인증 인터셉터 없음

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null

        return lock.withLock {
            val current = tokenStore.accessSync()
            val attempted = response.request.header("Authorization")?.removePrefix("Bearer ")

            // 다른 스레드가 이미 갱신했으면 재사용
            if (current != null && current != attempted) {
                return@withLock response.request.newBuilder()
                    .header("Authorization", "Bearer $current")
                    .build()
            }

            val refresh = tokenStore.refreshSync() ?: return@withLock null
            val body = json.encodeToString(RefreshReq.serializer(), RefreshReq(refresh))
            val req = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "auth/refresh")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            val resp = runCatching { client.newCall(req).execute() }.getOrNull()
                ?: return@withLock null
            try {
                if (!resp.isSuccessful) {
                    tokenStore.clearSync()
                    return@withLock null
                }
                val raw = resp.body?.string() ?: return@withLock null
                val auth = runCatching {
                    json.decodeFromString(AuthResponse.serializer(), raw)
                }.getOrNull() ?: return@withLock null
                tokenStore.saveSync(auth.accessToken, auth.refreshToken, auth.user.id)

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${auth.accessToken}")
                    .build()
            } finally {
                resp.close()
            }
        }
    }
}
