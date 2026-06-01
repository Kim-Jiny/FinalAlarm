package com.jiny.finalalarm.core.push

import android.content.Context
import android.provider.Settings
import com.jiny.finalalarm.data.api.DevicePlatform
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.RegisterPushTokenReq
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushTokenRegistrar @Inject constructor(
    private val api: FinalAlarmApi,
    @ApplicationContext private val ctx: Context,
) {
    suspend fun register(token: String) {
        val deviceId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
        runCatching {
            api.registerPushToken(RegisterPushTokenReq(DevicePlatform.ANDROID, token, deviceId))
        }.onFailure { Timber.e(it, "Push token register failed") }
    }
}
