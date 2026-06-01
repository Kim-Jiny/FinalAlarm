package com.jiny.finalalarm.core.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "pending_events")

/**
 * 로컬 발사 후 서버 createEvent 실패한 이벤트들을 보관.
 * 네트워크 복구 후 EventReconcileWorker가 처리.
 */
@Singleton
class PendingEventStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val key = stringPreferencesKey("queue")
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class Pending(
        val localId: String,
        val definitionId: String,
        val triggeredAt: String,           // ISO
        val dismissed: Boolean = false,
        val dismissedAt: String? = null,
        val volumePctAtTrigger: Int? = null,
        val dndAtTrigger: Boolean? = null,
        val volumePctAtDismiss: Int? = null,
        val dndAtDismiss: Boolean? = null,
    )

    suspend fun list(): List<Pending> {
        val raw = ctx.dataStore.data.first()[key] ?: return emptyList()
        return runCatching { json.decodeFromString<List<Pending>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun add(p: Pending) {
        val cur = list().filterNot { it.localId == p.localId }
        write(cur + p)
    }

    suspend fun markDismissed(
        localId: String,
        dismissedAt: String,
        volumePct: Int? = null,
        dnd: Boolean? = null,
    ) {
        val cur = list().map {
            if (it.localId == localId) it.copy(
                dismissed = true,
                dismissedAt = dismissedAt,
                volumePctAtDismiss = volumePct,
                dndAtDismiss = dnd,
            ) else it
        }
        write(cur)
    }

    suspend fun remove(localId: String) {
        write(list().filterNot { it.localId == localId })
    }

    private suspend fun write(items: List<Pending>) {
        ctx.dataStore.edit { it[key] = json.encodeToString(items) }
    }
}
