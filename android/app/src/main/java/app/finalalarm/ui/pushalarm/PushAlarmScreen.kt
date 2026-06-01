package app.finalalarm.ui.pushalarm

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
import app.finalalarm.data.api.FinalAlarmApi
import app.finalalarm.data.api.PushAlarmReq
import app.finalalarm.data.api.TeamMemberDto
import app.finalalarm.ui.util.assistedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PushAlarmUi(
    val members: List<TeamMemberDto> = emptyList(),
    val selectedUserId: String? = null,
    val label: String = "일어나!",
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
)

class PushAlarmVm @AssistedInject constructor(
    private val api: FinalAlarmApi,
    @Assisted private val teamId: String,
) : ViewModel() {
    private val _state = MutableStateFlow(PushAlarmUi())
    val state = _state.asStateFlow()

    init { viewModelScope.launch {
        val team = runCatching { api.getTeam(teamId) }.getOrNull()
        _state.value = _state.value.copy(members = team?.members.orEmpty())
    } }

    fun selectUser(id: String) { _state.value = _state.value.copy(selectedUserId = id) }
    fun onLabel(v: String) { _state.value = _state.value.copy(label = v) }

    fun send() = viewModelScope.launch {
        val s = _state.value
        val target = s.selectedUserId ?: return@launch
        _state.value = s.copy(sending = true, error = null)
        runCatching { api.pushAlarm(PushAlarmReq(target, teamId, s.label)) }
            .onSuccess { _state.value = _state.value.copy(sending = false, sent = true) }
            .onFailure { _state.value = _state.value.copy(sending = false, error = it.userMessage()) }
    }
}

@AssistedFactory
interface PushAlarmVmFactory { fun create(teamId: String): PushAlarmVm }

@HiltViewModel
class PushAlarmVmHost @Inject constructor(val factory: PushAlarmVmFactory) : ViewModel()

@Composable
fun PushAlarmScreen(nav: NavController, teamId: String) {
    val host = hiltViewModel<PushAlarmVmHost>()
    val vm = assistedViewModel(teamId) { host.factory.create(teamId) }
    val s by vm.state.collectAsState()
    LaunchedEffect(s.sent) { if (s.sent) nav.popBackStack() }
    Scaffold(
        topBar = { TopAppBar(title = { Text("팀원 깨우기") }) },
        bottomBar = {
            Button(
                onClick = vm::send,
                enabled = !s.sending && s.selectedUserId != null,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text(if (s.sending) "전송 중…" else "알람 발사") }
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).padding(16.dp)) {
            OutlinedTextField(s.label, vm::onLabel, label = { Text("라벨") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text("팀원 선택", style = MaterialTheme.typography.titleSmall)
            s.members.forEach { m ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = s.selectedUserId == m.user.id, onClick = { vm.selectUser(m.user.id) })
                    Column { Text(m.user.displayName); Text(m.role.name, style = MaterialTheme.typography.bodySmall) }
                }
            }
            s.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
