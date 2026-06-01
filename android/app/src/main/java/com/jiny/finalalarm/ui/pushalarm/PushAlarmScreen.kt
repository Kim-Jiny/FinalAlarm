package com.jiny.finalalarm.ui.pushalarm

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.PushAlarmReq
import com.jiny.finalalarm.data.api.TeamMemberDto
import com.jiny.finalalarm.ui.components.ErrorText
import com.jiny.finalalarm.ui.components.FaTextField
import com.jiny.finalalarm.ui.components.HelloHeader
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.components.PrimaryButton
import com.jiny.finalalarm.ui.components.Section
import com.jiny.finalalarm.ui.components.WarmBackground
import com.jiny.finalalarm.ui.theme.FaSpacing
import com.jiny.finalalarm.ui.util.assistedViewModel
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

    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = FaSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = FaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { nav.popBackStack() }) { Text("뒤로") }
                Spacer(Modifier.weight(1f))
            }
            HelloHeader(
                title = "친구를 깨워요",
                subtitle = "이 시간대 안에서만 알람이 울려요",
            )

            Section("메시지") {
                FaTextField(s.label, vm::onLabel, "예: 일어나!")
            }

            Section("누구를 깨울까요?") {
                if (s.members.isEmpty()) {
                    Text(
                        "팀원이 없어요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    s.members.forEach { m ->
                        ListRow(
                            headline = m.user.displayName,
                            supporting = when (m.role.name) {
                                "OWNER" -> "오너"
                                "ADMIN" -> "관리자"
                                else -> "멤버"
                            },
                            trailing = {
                                RadioButton(
                                    selected = s.selectedUserId == m.user.id,
                                    onClick = { vm.selectUser(m.user.id) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { vm.selectUser(m.user.id) },
                        )
                    }
                }
            }

            s.error?.let { ErrorText(it) }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = if (s.sending) "발사 중…" else "알람 발사",
                onClick = vm::send,
                enabled = !s.sending && s.selectedUserId != null,
            )
            Spacer(Modifier.height(FaSpacing.md))
        }
    }
}
