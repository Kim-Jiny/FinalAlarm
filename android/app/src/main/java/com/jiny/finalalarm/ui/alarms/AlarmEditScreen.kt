package com.jiny.finalalarm.ui.alarms

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.jiny.finalalarm.ui.components.ErrorText
import com.jiny.finalalarm.ui.components.FaTextField
import com.jiny.finalalarm.ui.components.HelloHeader
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.components.PrimaryButton
import com.jiny.finalalarm.ui.components.Section
import com.jiny.finalalarm.ui.components.WarmBackground
import com.jiny.finalalarm.ui.theme.FA
import com.jiny.finalalarm.ui.theme.FaSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

data class AlarmEditUi(
    val editingId: String? = null,
    val label: String = "기상",
    val kind: AlarmKind = AlarmKind.PERSONAL,
    val teams: List<TeamSummary> = emptyList(),
    val teamId: String? = null,
    val timeOfDay: String = "07:00",
    val daysOfWeek: Int = 0b0011111,
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
            _state.value = s.copy(error = "팀을 선택해주세요"); return@launch
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

    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FaSpacing.sm, vertical = FaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { nav.popBackStack() }) { Text("뒤로") }
                Spacer(Modifier.weight(1f))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FaSpacing.lg),
            ) {
                HelloHeader(
                    title = if (alarmId == null) "새 알람" else "알람 수정",
                    subtitle = "${s.timeOfDay}에 일어나요",
                )

                Section("이름") {
                    FaTextField(s.label, vm::onLabel, "예: 출근")
                }

                Section("알람 종류") {
                    Row(horizontalArrangement = Arrangement.spacedBy(FaSpacing.sm)) {
                        AlarmKind.entries.forEach { k ->
                            ChoicePill(
                                text = if (k == AlarmKind.TEAM_APPROVAL) "팀원 승인" else "혼자",
                                selected = s.kind == k,
                                onClick = { vm.onKind(k) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                if (s.kind == AlarmKind.TEAM_APPROVAL) {
                    Section("팀") {
                        if (s.teams.isEmpty()) {
                            Text(
                                "먼저 팀을 만들어주세요",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            s.teams.forEach { t ->
                                ListRow(
                                    headline = t.name,
                                    trailing = {
                                        RadioButton(
                                            selected = s.teamId == t.id,
                                            onClick = { vm.onTeam(t.id) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary,
                                            ),
                                        )
                                    },
                                    onClick = { vm.onTeam(t.id) },
                                )
                            }
                        }
                    }
                }

                Section("시각") {
                    com.jiny.finalalarm.ui.components.TimePickerField(
                        label = "기상 시각",
                        value = s.timeOfDay,
                        onValueChange = vm::onTime,
                    )
                }

                Section("반복 요일") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FaSpacing.xs),
                    ) {
                        listOf("월", "화", "수", "목", "금", "토", "일").forEachIndexed { i, d ->
                            DayPill(
                                day = d,
                                selected = (s.daysOfWeek shr i) and 1 == 1,
                                onClick = { vm.toggleDay(i) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Section("미션") {
                    if (s.missions.isEmpty()) {
                        Text(
                            "미션을 먼저 만들어주세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        s.missions.forEach { m ->
                            ListRow(
                                headline = m.name,
                                supporting = m.type.name,
                                trailing = {
                                    RadioButton(
                                        selected = s.missionId == m.id,
                                        onClick = { vm.onMission(m.id) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                        ),
                                    )
                                },
                                onClick = { vm.onMission(m.id) },
                            )
                        }
                    }
                }

                Section("스누즈") {
                    ListRow(
                        headline = "스누즈 사용",
                        supporting = if (s.snoozeEnabled) "${s.snoozeMinutes}분씩, 최대 ${s.snoozeMaxCount}회" else "사용 안 함",
                        trailing = {
                            Switch(
                                checked = s.snoozeEnabled,
                                onCheckedChange = { vm.toggleSnooze() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        },
                    )
                }

                Section("볼륨") {
                    Text(
                        "${s.volume}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = s.volume.toFloat(),
                        onValueChange = { vm.onVolume(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }

                s.error?.let { ErrorText(it) }
                Spacer(Modifier.height(FaSpacing.xxl))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FaSpacing.lg, vertical = FaSpacing.sm),
            ) {
                PrimaryButton(
                    text = if (s.saving) "저장 중…" else "저장",
                    onClick = vm::save,
                    enabled = !s.saving,
                )
            }
        }
    }
}

@Composable
private fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) FA.OnPrimary else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DayPill(
    day: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) FA.OnPrimary else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}
