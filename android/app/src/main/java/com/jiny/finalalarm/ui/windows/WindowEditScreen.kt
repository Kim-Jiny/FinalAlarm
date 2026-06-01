package com.jiny.finalalarm.ui.windows

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.data.api.CreateWindowReq
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.TeamSummary
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

data class WindowUi(
    val editingId: String? = null,
    val teams: List<TeamSummary> = emptyList(),
    val teamId: String? = null,
    val start: String = "06:00",
    val end: String = "09:00",
    val days: Int = 0b1111111,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class WindowEditVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    private val _state = MutableStateFlow(WindowUi())
    val state = _state.asStateFlow()

    init { viewModelScope.launch { _state.value = _state.value.copy(teams = runCatching { api.listTeams() }.getOrDefault(emptyList())) } }

    fun loadExisting(id: String) = viewModelScope.launch {
        val w = runCatching { api.listWindows() }.getOrDefault(emptyList()).firstOrNull { it.id == id }
            ?: return@launch
        _state.value = _state.value.copy(
            editingId = w.id,
            teamId = w.teamId,
            start = w.startTime,
            end = w.endTime,
            days = w.daysOfWeek,
        )
    }

    fun onTeam(id: String) { _state.value = _state.value.copy(teamId = id) }
    fun onStart(v: String) { _state.value = _state.value.copy(start = v) }
    fun onEnd(v: String) { _state.value = _state.value.copy(end = v) }
    fun toggleDay(i: Int) { _state.value = _state.value.copy(days = _state.value.days xor (1 shl i)) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        val team = s.teamId ?: run { _state.value = s.copy(error = "팀을 선택해주세요"); return@launch }
        _state.value = s.copy(saving = true, error = null)
        runCatching {
            if (s.editingId == null) {
                api.createWindow(CreateWindowReq(team, s.start, s.end, s.days, TimeZone.getDefault().id))
            } else {
                api.updateWindow(
                    s.editingId,
                    mapOf(
                        "startTime" to kotlinx.serialization.json.JsonPrimitive(s.start),
                        "endTime" to kotlinx.serialization.json.JsonPrimitive(s.end),
                        "daysOfWeek" to kotlinx.serialization.json.JsonPrimitive(s.days),
                    ),
                )
            }
        }.onSuccess { _state.value = _state.value.copy(saving = false, saved = true) }
            .onFailure { _state.value = _state.value.copy(saving = false, error = it.userMessage()) }
    }
}

@Composable
fun WindowEditScreen(nav: NavController, windowId: String?, vm: WindowEditVm = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(windowId) { if (windowId != null) vm.loadExisting(windowId) }
    LaunchedEffect(s.saved) { if (s.saved) nav.popBackStack() }

    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = FaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FaSpacing.md),
        ) {
            // TopBar substitute
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = FaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { nav.popBackStack() }) { Text("뒤로") }
                Spacer(Modifier.weight(1f))
            }
            HelloHeader(
                title = if (s.editingId == null) "시간대 만들기" else "시간대 수정",
                subtitle = "이 시간에 팀원이 깨워줄 수 있어요",
            )

            Section("팀") {
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

            Section("시간") {
                FaTextField(s.start, vm::onStart, "시작 (HH:MM)")
                Spacer(Modifier.height(FaSpacing.sm))
                FaTextField(s.end, vm::onEnd, "끝 (HH:MM)")
            }

            Section("요일") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FaSpacing.xs),
                ) {
                    listOf("월", "화", "수", "목", "금", "토", "일").forEachIndexed { i, d ->
                        val selected = (s.days shr i) and 1 == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { vm.toggleDay(i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                d,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) FA.OnPrimary else MaterialTheme.colorScheme.onBackground,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            s.error?.let { ErrorText(it) }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = if (s.saving) "저장 중…" else "저장",
                onClick = vm::save,
                enabled = !s.saving,
            )
            Spacer(Modifier.height(FaSpacing.md))
        }
    }
}
