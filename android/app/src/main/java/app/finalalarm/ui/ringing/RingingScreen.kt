package app.finalalarm.ui.ringing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.finalalarm.ui.util.assistedViewModel
import android.content.Context
import app.finalalarm.core.alarm.AlarmRingPayload
import app.finalalarm.core.network.userMessage
import app.finalalarm.core.sync.EventReconcileWorker
import app.finalalarm.core.sync.PendingEventStore
import app.finalalarm.data.api.AlarmEventState
import app.finalalarm.data.api.DismissReq
import app.finalalarm.data.api.FinalAlarmApi
import app.finalalarm.data.api.MissionType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.OffsetDateTime
import app.finalalarm.ui.missions.MathMissionRunner
import app.finalalarm.ui.missions.PhotoMissionRunner
import app.finalalarm.ui.missions.ShakeMissionRunner
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

enum class RingingPhase { IDLE, MISSION, WAITING_APPROVAL, DONE, ERROR }

data class RingingUi(
    val phase: RingingPhase = RingingPhase.IDLE,
    val eventState: AlarmEventState = AlarmEventState.RINGING,
    val error: String? = null,
)

class RingingVm @AssistedInject constructor(
    private val api: FinalAlarmApi,
    private val pendingStore: PendingEventStore,
    @ApplicationContext private val appCtx: Context,
    @Assisted private val eventId: String,
) : ViewModel() {

    private val isLocal: Boolean get() = eventId.startsWith("local-")
    private val _state = MutableStateFlow(RingingUi())
    val state = _state.asStateFlow()

    init { if (!isLocal) poll() }

    private fun poll() = viewModelScope.launch {
        while (true) {
            val e = runCatching { api.getEvent(eventId) }.getOrNull()
            if (e != null) {
                _state.value = _state.value.copy(eventState = e.state)
                if (e.state == AlarmEventState.UNLOCK_APPROVED && _state.value.phase == RingingPhase.WAITING_APPROVAL) {
                    _state.value = _state.value.copy(phase = RingingPhase.MISSION)
                }
                if (e.state == AlarmEventState.DISMISSED) {
                    _state.value = _state.value.copy(phase = RingingPhase.DONE)
                    return@launch
                }
            }
            delay(3000)
        }
    }

    fun snooze() = viewModelScope.launch {
        if (isLocal) {
            // 오프라인 — 서버에 스누즈 못 알림. 그냥 끄기로 종료.
            _state.value = _state.value.copy(phase = RingingPhase.DONE)
            return@launch
        }
        runCatching { api.snooze(eventId) }
            .onFailure { _state.value = _state.value.copy(error = it.userMessage()) }
            .onSuccess { _state.value = _state.value.copy(phase = RingingPhase.DONE) }
    }

    fun requestUnlock() = viewModelScope.launch {
        if (isLocal) {
            _state.value = _state.value.copy(
                error = "오프라인 상태입니다. 인터넷 연결 후 다시 시도해주세요.",
            )
            return@launch
        }
        runCatching { api.requestUnlock(eventId) }
            .onFailure { _state.value = _state.value.copy(error = it.userMessage()) }
            .onSuccess { _state.value = _state.value.copy(phase = RingingPhase.WAITING_APPROVAL) }
    }

    fun startMission() {
        _state.value = _state.value.copy(phase = RingingPhase.MISSION)
    }

    fun submitDismiss(type: MissionType, proof: Map<String, JsonElement>) = viewModelScope.launch {
        if (isLocal) {
            // 오프라인 — pending queue에 dismiss 기록 후 종료 (네트워크 복구 시 reconcile)
            runCatching {
                pendingStore.markDismissed(eventId, OffsetDateTime.now().toString())
                EventReconcileWorker.enqueue(appCtx)
            }
            _state.value = _state.value.copy(phase = RingingPhase.DONE)
            return@launch
        }
        val payload = proof.toMutableMap()
        payload["type"] = JsonPrimitive(type.name)
        runCatching { api.dismiss(eventId, DismissReq(payload)) }
            .onSuccess { _state.value = _state.value.copy(phase = RingingPhase.DONE) }
            .onFailure { _state.value = _state.value.copy(error = it.userMessage()) }
    }
}

@AssistedFactory
interface RingingVmFactory { fun create(eventId: String): RingingVm }

@Composable
fun RingingRoot(payload: AlarmRingPayload, onFinished: () -> Unit) {
    val host = hiltViewModel<RingingVmHostVm>()
    val vm: RingingVm = assistedViewModel(payload.eventId) { host.factory.create(payload.eventId) }
    val ui by vm.state.collectAsState()

    LaunchedEffect(ui.phase) {
        if (ui.phase == RingingPhase.DONE) onFinished()
    }

    when (ui.phase) {
        RingingPhase.IDLE -> RingingPrompt(payload, ui, vm, onFinished)
        RingingPhase.WAITING_APPROVAL -> WaitingApprovalScreen(payload, vm)
        RingingPhase.MISSION -> MissionFlow(payload, vm)
        RingingPhase.DONE -> {}
        RingingPhase.ERROR -> Text(ui.error ?: "오류")
    }
}

/**
 * VM 인스턴스화에 Hilt assisted factory를 받아오는 가벼운 호스트 VM.
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class RingingVmHostVm @javax.inject.Inject constructor(
    val factory: RingingVmFactory,
) : ViewModel()

@Composable
private fun RingingPrompt(payload: AlarmRingPayload, ui: RingingUi, vm: RingingVm, onFinished: () -> Unit) {
    val isOffline = payload.eventId.startsWith("local-")
    val isTeamApproval = payload.alarmKind == "TEAM_APPROVAL"

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isOffline) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    "오프라인 상태입니다. 연결 복구 시 자동 동기화됩니다.",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        Text(payload.label.ifBlank { "알람" }, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        payload.senderDisplayName?.let {
            Text("$it 님이 깨우고 있어요", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(48.dp))

        if (isTeamApproval) {
            Button(
                onClick = vm::requestUnlock,
                enabled = !isOffline,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isOffline) "오프라인 — 요청 불가" else "팀원에게 잠금해제 요청")
            }
        } else {
            Button(onClick = vm::startMission, modifier = Modifier.fillMaxWidth()) {
                Text("미션 시작해서 끄기")
            }
        }
        Spacer(Modifier.height(16.dp))
        if (payload.snoozeEnabled && payload.snoozeRemaining > 0) {
            OutlinedButton(onClick = vm::snooze, modifier = Modifier.fillMaxWidth()) {
                Text("${payload.snoozeMinutes}분 후 다시 (${payload.snoozeRemaining}회 남음)")
            }
        }
        ui.error?.let { Spacer(Modifier.height(16.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun WaitingApprovalScreen(payload: AlarmRingPayload, vm: RingingVm) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text("팀원의 승인을 기다리는 중…", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("5분 내 응답이 없으면 만료됩니다", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MissionFlow(payload: AlarmRingPayload, vm: RingingVm) {
    val type = runCatching { MissionType.valueOf(payload.missionType) }.getOrNull() ?: MissionType.SHAKE
    val cfg = remember(payload.missionConfigJson) {
        runCatching { kotlinx.serialization.json.Json.parseToJsonElement(payload.missionConfigJson).jsonObject }
            .getOrNull() ?: kotlinx.serialization.json.buildJsonObject {}
    }
    when (type) {
        MissionType.MATH -> {
            val difficulty = cfg["difficulty"]?.toString()?.trim('"') ?: "medium"
            val count = cfg["questionCount"]?.toString()?.toIntOrNull() ?: 3
            MathMissionRunner(difficulty, count) { answers ->
                vm.submitDismiss(
                    MissionType.MATH,
                    mapOf("answers" to buildJsonArray { answers.forEach { add(JsonPrimitive(it)) } }),
                )
            }
        }
        MissionType.PHOTO -> {
            val mode = cfg["mode"]?.toString()?.trim('"') ?: "QR"
            val expectedCode = cfg["expectedCode"]?.toString()?.trim('"')?.takeIf { it.isNotBlank() }
            PhotoMissionRunner(mode, expectedCode) { url ->
                vm.submitDismiss(MissionType.PHOTO, mapOf("imageUrl" to JsonPrimitive(url)))
            }
        }
        MissionType.SHAKE -> {
            val target = cfg["shakeCount"]?.toString()?.toIntOrNull() ?: 30
            ShakeMissionRunner(target) { count ->
                vm.submitDismiss(MissionType.SHAKE, mapOf("shakeCount" to JsonPrimitive(count)))
            }
        }
    }
}
