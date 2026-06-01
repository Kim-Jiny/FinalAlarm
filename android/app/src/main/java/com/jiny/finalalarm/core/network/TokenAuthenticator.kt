package com.jiny.finalalarm.core.network

import com.jiny.finalalarm.BuildConfig
import com.jiny.finalalarm.core.auth.TokenStore
import com.jiny.finalalarm.data.api.AuthResponse
import com.jiny.finalalarm.data.api.RefreshReq
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
) : Authenticator {

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().build() // refresh 호출용. 인증 인터셉터 없음

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null

        return runBlocking {
            mutex.withLock {
                val current = tokenStore.access()
                val attempted = response.request.header("Authorization")?.removePrefix("Bearer ")

                // 다른 스레드가 이미 갱신했으면 재사용
                if (current != null && current != attempted) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $current")
                        .build()
                }

                val refresh = tokenStore.refresh() ?: return@withLock null
                val body = json.encodeToString(RefreshReq.serializer(), RefreshReq(refresh))
                val req = Request.Builder()
                    .url(BuildConfig.API_BASE_URL + "auth/refresh")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    tokenStore.clear()
                    return@withLock null
                }
                val raw = resp.body?.string() ?: return@withLock null
                val auth = json.decodeFromString(AuthResponse.serializer(), raw)
                tokenStore.save(auth.accessToken, auth.refreshToken, auth.user.id)

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${auth.accessToken}")
                    .build()
            }
        }
    }
}
