package app.finalalarm.data

import app.finalalarm.core.auth.TokenStore
import app.finalalarm.data.api.AuthResponse
import app.finalalarm.data.api.FinalAlarmApi
import app.finalalarm.data.api.LoginReq
import app.finalalarm.data.api.LogoutReq
import app.finalalarm.data.api.SignupReq
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: FinalAlarmApi,
    private val tokenStore: TokenStore,
) {
    val loggedInFlow: Flow<Boolean> = tokenStore.accessTokenFlow.map { it != null }

    suspend fun signup(email: String, password: String, displayName: String, timezone: String?): AuthResponse {
        val r = api.signup(SignupReq(email, password, displayName, timezone))
        tokenStore.save(r.accessToken, r.refreshToken, r.user.id)
        return r
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val r = api.login(LoginReq(email, password))
        tokenStore.save(r.accessToken, r.refreshToken, r.user.id)
        return r
    }

    suspend fun logout() {
        val refresh = tokenStore.refresh() ?: return
        runCatching { api.logout(LogoutReq(refresh)) }
        tokenStore.clear()
    }
}
