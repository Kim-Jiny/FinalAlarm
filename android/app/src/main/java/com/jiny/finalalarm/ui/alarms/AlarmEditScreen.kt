package com.jiny.finalalarm.ui.alarms

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.core.work.AlarmRescheduleWorker
import com.jiny.finalalarm.data.api.AlarmKind
import com.jiny.finalalarm.data.api.CreateAlarmReq
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.MissionDto
import com.jiny.finalalarm.data.api.ScheduleType
import com.jiny.finalalarm.data.api.TeamSummary
import com.jiny.finalalarm.data.api.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

data class AlarmEditUi(
    val editingId: String? = null,         // null = 생성 모드, 값 = 수정 모드
    val label: String = "기상",
    val kind: AlarmKind = AlarmKind.PERSONAL,
    val teams: List<TeamSummary> = emptyList(),
    val teamId: String? = null,
    val timeOfDay: String = "07:00",
    val daysOfWeek: Int = 0b0011111,  // 월-금
    val missions: List<MissionDto> = emptyList(),
    val missionId: String? = null,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val snoozeMaxCount: Int = 3,
    val volume: Int = 80,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class AlarmEditVm @Inject constructor(
    private val api: FinalAlarmApi,
    @ApplicationContext private val ctx: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(AlarmEditUi())
    val state = _state.asStateFlow()

    init { loadReferences() }

    private fun loadReferences() = viewModelScope.launch {
        val teams = runCatching { api.listTeams() }.getOrDefault(emptyList())
        val missions = runCatching { api.listMissions() }.getOrDefault(emptyList())
        _state.value = _state.value.copy(
            teams = teams,
            missions = missions,
            missionId = _state.value.missionId
                ?: missions.firstOrNull { it.isDefault }?.id ?: missions.firstOrNull()?.id,
        )
    }

    fun loadExisting(id: String) = viewModelScope.launch {
        val alarm = runCatching { api.getAlarm(id) }.getOrNull() ?: return@launch
        _state.value = _state.value.copy(
            editingId = alarm.id,
            label = alarm.label,
            kind = alarm.kind,
            teamId = alarm.teamId,
            timeOfDay = alarm.timeOfDay ?: _state.value.timeOfDay,
            daysOfWeek = alarm.daysOfWeek ?: _state.value.daysOfWeek,
            missionId = alarm.missionId,
            snoozeEnabled = alarm.snoozeEnabled,
            snoozeMinutes = alarm.snoozeMinutes,
            snoozeMaxCount = alarm.snoozeMaxCount,
            volume = alarm.volume,
        )
    }

    fun onLabel(v: String) { _state.value = _state.value.copy(label = v) }
    fun onKind(v: AlarmKind) { _state.value = _state.value.copy(kind = v, teamId = if (v == AlarmKind.PERSONAL) null else _state.value.teamId) }
    fun onTeam(id: String?) { _state.value = _state.value.copy(teamId = id) }
    fun onTime(v: String) { _state.value = _state.value.copy(timeOfDay = v) }
    fun toggleDay(bit: Int) {
        val cur = _state.value.daysOfWeek
        _state.value = _state.value.copy(daysOfWeek = cur xor (1 shl bit))
    }
    fun onMission(id: String) { _state.value = _state.value.copy(missionId = id) }
    fun toggleSnooze() { _state.value = _state.value.copy(snoozeEnabled = !_state.value.snoozeEnabled) }
    fun onVolume(v: Int) { _state.value = _state.value.copy(volume = v) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        val missionId = s.missionId ?: run {
            _state.value = s.copy(error = "미션을 먼저 만들어주세요")
            return@launch
        }
        if (s.kind == AlarmKind.TEAM_APPROVAL && s.teamId == null) {
            _state.value = s.copy(error = "팀을 선택하세요"); return@launch
        }
        _state.value = s.copy(saving = true, error = null)
        runCatching {
            if (s.editingId == null) {
                api.createAlarm(
                    CreateAlarmReq(
                        kind = s.kind,
                        teamId = s.teamId,
                        label = s.label,
                        timezone = TimeZone.getDefault().id,
                        scheduleType = ScheduleType.RECURRING,
                        timeOfDay = s.timeOfDay,
                        daysOfWeek = s.daysOfWeek,
                        soundUri = "system:default",
                        volume = s.volume,
                        volumeRampSeconds = 30,
                        vibrationEnabled = true,
                        vibrationPattern = VibrationPattern.PULSE,
                        snoozeEnabled = s.snoozeEnabled,
                        snoozeMinutes = s.snoozeMinutes,
                        snoozeMaxCount = s.snoozeMaxCount,
                        missionId = missionId,
                    ),
                )
            } else {
                api.updateAlarm(
                    s.editingId,
                    mapOf(
                        "label" to kotlinx.serialization.json.JsonPrimitive(s.label),
                        "timeOfDay" to kotlinx.serialization.json.JsonPrimitive(s.timeOfDay),
                        "daysOfWeek" to kotlinx.serialization.json.JsonPrimitive(s.daysOfWeek),
                        "volume" to kotlinx.serialization.json.JsonPrimitive(s.volume),
                        "snoozeEnabled" to kotlinx.serialization.json.JsonPrimitive(s.snoozeEnabled),
                        "snoozeMinutes" to kotlinx.serialization.json.JsonPrimitive(s.snoozeMinutes),
                        "snoozeMaxCount" to kotlinx.serialization.json.JsonPrimitive(s.snoozeMaxCount),
                        "missionId" to kotlinx.serialization.json.JsonPrimitive(missionId),
                    ),
                )
            }
        }.onSuccess {
            AlarmRescheduleWorker.enqueue(ctx)
            _state.value = _state.value.copy(saving = false, saved = true)
        }
            .onFailure { _state.value = _state.value.copy(saving = false, error = it.userMessage()) }
    }
}

@Composable
fun AlarmEditScreen(nav: NavController, alarmId: String?, vm: AlarmEditVm = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(alarmId) { if (alarmId != null) vm.loadExisting(alarmId) }
    LaunchedEffect(s.saved) { if (s.saved) nav.popBackStack() }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (alarmId == null) "알람 추가" else "알람 편집") }) },
        bottomBar = {
            Button(
                onClick = vm::save,
                enabled = !s.saving,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text(if (s.saving) "저장 중…" else "저장") }
        },
    ) { inner ->
        Column(
            modifier = Modifier.padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(s.label, vm::onLabel, label = { Text("라벨") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            Text("알람 종류", style = MaterialTheme.typography.titleSmall)
            Row {
                AlarmKind.entries.forEach { k ->
                    FilterChip(
                        selected = s.kind == k,
                        onClick = { vm.onKind(k) },
                        label = { Text(if (k == AlarmKind.TEAM_APPROVAL) "팀원 승인" else "일반") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            if (s.kind == AlarmKind.TEAM_APPROVAL) {
                Spacer(Modifier.height(16.dp))
                Text("팀", style = MaterialTheme.typography.titleSmall)
                s.teams.forEach { t ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = s.teamId == t.id, onClick = { vm.onTeam(t.id) })
                        Text(t.name)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                s.timeOfDay, vm::onTime, label = { Text("시각 (HH:MM)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("반복 요일", style = MaterialTheme.typography.titleSmall)
            Row {
                listOf("월", "화", "수", "목", "금", "토", "일").forEachIndexed { i, d ->
                    val on = (s.daysOfWeek shr i) and 1 == 1
                    FilterChip(
                        selected = on,
                        onClick = { vm.toggleDay(i) },
                        label = { Text(d) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("미션", style = MaterialTheme.typography.titleSmall)
            s.missions.forEach { m ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = s.missionId == m.id, onClick = { vm.onMission(m.id) })
                    Text("${m.name} (${m.type})")
                }
            }
            if (s.missions.isEmpty()) Text("미션을 먼저 만들어주세요", color = MaterialTheme.colorScheme.error)

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Switch(checked = s.snoozeEnabled, onCheckedChange = { vm.toggleSnooze() })
                Spacer(Modifier.width(8.dp))
                Text("스누즈 사용")
            }

            Spacer(Modifier.height(16.dp))
            Text("볼륨: ${s.volume}")
            Slider(value = s.volume.toFloat(), onValueChange = { vm.onVolume(it.toInt()) }, valueRange = 0f..100f)

            s.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
