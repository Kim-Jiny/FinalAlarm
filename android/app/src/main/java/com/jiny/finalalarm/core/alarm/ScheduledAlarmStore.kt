package com.jiny.finalalarm.core.alarm

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "scheduled_alarms")

/**
 * 현재 AlarmManager에 등록되어 있는 alarm definition ID 집합.
 * 동기화 시 이 목록과 서버 active 목록 차이로 cancel 대상 산출.
 */
@Singleton
class ScheduledAlarmStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val key = stringSetPreferencesKey("ids")

    suspend fun list(): Set<String> = ctx.dataStore.data.first()[key] ?: emptySet()

    suspend fun save(ids: Set<String>) {
        ctx.dataStore.edit { it[key] = ids }
    }
}
