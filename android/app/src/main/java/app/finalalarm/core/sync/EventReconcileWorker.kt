package app.finalalarm.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.finalalarm.data.api.CreateEventReq
import app.finalalarm.data.api.FinalAlarmApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class EventReconcileWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val api: FinalAlarmApi,
    private val store: PendingEventStore,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val pendings = store.list()
        if (pendings.isEmpty()) return Result.success()

        var allOk = true
        for (p in pendings) {
            val result = runCatching {
                api.createEvent(
                    CreateEventReq(
                        definitionId = p.definitionId,
                        triggeredAt = p.triggeredAt,
                        initialState = if (p.dismissed) "DISMISSED" else "RINGING",
                        dismissedAt = p.dismissedAt,
                    ),
                )
            }
            if (result.isSuccess) {
                store.remove(p.localId)
            } else {
                allOk = false
                Timber.w(result.exceptionOrNull(), "reconcile failed for ${p.localId}")
            }
        }
        return if (allOk) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "event-reconcile"

        fun enqueue(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<EventReconcileWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, req)
        }
    }
}
