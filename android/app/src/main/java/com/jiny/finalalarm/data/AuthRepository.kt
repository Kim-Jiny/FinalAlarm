package com.jiny.finalalarm.data

import android.content.Context
import com.jiny.finalalarm.core.auth.TokenStore
import com.jiny.finalalarm.core.work.AlarmRescheduleWorker
import com.jiny.finalalarm.data.api.AuthResponse
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.LoginReq
import com.jiny.finalalarm.data.api.LogoutReq
import com.jiny.finalalarm.data.api.SignupReq
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: FinalAlarmApi,
    private val tokenStore: TokenStore,
    @ApplicationContext private val ctx: Context,
) {
    val loggedInFlow: Flow<Boolean> = tokenStore.accessTokenFlow.map { it != null }

    suspend fun signup(email: String, password: String, displayName: String, timezone: String?): AuthResponse {
        val r = api.signup(SignupReq(email, password, displayName, timezone))
        tokenStore.save(r.accessToken, r.refreshToken, r.user.id)
        AlarmRescheduleWorker.enqueue(ctx)
        return r
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val r = api.login(LoginReq(email, password))
        tokenStore.save(r.accessToken, r.refreshToken, r.user.id)
        AlarmRescheduleWorker.enqueue(ctx)
        return r
    }

    suspend fun logout() {
        val refresh = tokenStore.refresh() ?: return
        runCatching { api.logout(LogoutReq(refresh)) }
        tokenStore.clear()
    }
}
