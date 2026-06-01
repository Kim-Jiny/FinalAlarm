package app.finalalarm.ui.windows

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import app.finalalarm.core.network.userMessage
import app.finalalarm.data.api.CreateWindowReq
import app.finalalarm.data.api.FinalAlarmApi
import app.finalalarm.data.api.TeamSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

data class WindowUi(
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

    fun onTeam(id: String) { _state.value = _state.value.copy(teamId = id) }
    fun onStart(v: String) { _state.value = _state.value.copy(start = v) }
    fun onEnd(v: String) { _state.value = _state.value.copy(end = v) }
    fun toggleDay(i: Int) { _state.value = _state.value.copy(days = _state.value.days xor (1 shl i)) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        val team = s.teamId ?: run { _state.value = s.copy(error = "팀 선택 필요"); return@launch }
        _state.value = s.copy(saving = true, error = null)
        runCatching {
            api.createWindow(CreateWindowReq(team, s.start, s.end, s.days, TimeZone.getDefault().id))
        }.onSuccess { _state.value = _state.value.copy(saving = false, saved = true) }
            .onFailure { _state.value = _state.value.copy(saving = false, error = it.userMessage()) }
    }
}

@Composable
fun WindowEditScreen(nav: NavController, windowId: String?, vm: WindowEditVm = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.saved) { if (s.saved) nav.popBackStack() }
    Scaffold(
        topBar = { TopAppBar(title = { Text("알람 시간대") }) },
        bottomBar = {
            Button(onClick = vm::save, enabled = !s.saving, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(if (s.saving) "저장 중…" else "저장")
            }
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).padding(16.dp)) {
            Text("팀", style = MaterialTheme.typography.titleSmall)
            s.teams.forEach { t ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = s.teamId == t.id, onClick = { vm.onTeam(t.id) })
                    Text(t.name)
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(s.start, vm::onStart, label = { Text("시작 (HH:MM)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(s.end, vm::onEnd, label = { Text("끝 (HH:MM)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text("요일")
            Row {
                listOf("월", "화", "수", "목", "금", "토", "일").forEachIndexed { i, d ->
                    FilterChip(
                        selected = (s.days shr i) and 1 == 1,
                        onClick = { vm.toggleDay(i) },
                        label = { Text(d) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
            s.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
