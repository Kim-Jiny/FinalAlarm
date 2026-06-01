package com.jiny.finalalarm.ui.inbox

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
import com.jiny.finalalarm.data.api.UnlockRequestDto
import com.jiny.finalalarm.ui.components.ErrorText
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

class UnlockRequestDetailVm @AssistedInject constructor(
    private val api: FinalAlarmApi,
    @Assisted private val id: String,
) : ViewModel() {
    private val _state = MutableStateFlow<UnlockRequestDto?>(null)
    val state = _state.asStateFlow()
    var approving by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var done by mutableStateOf(false)

    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = runCatching { api.getUnlockRequest(id) }.getOrNull()
    }

    fun approve() = viewModelScope.launch {
        approving = true; error = null
        runCatching { api.approveUnlock(id) }
            .onSuccess { done = true; approving = false }
            .onFailure { error = it.userMessage(); approving = false }
    }
}

@AssistedFactory
interface UnlockRequestDetailVmFactory { fun create(id: String): UnlockRequestDetailVm }

@HiltViewModel
class UnlockRequestDetailHost @Inject constructor(val factory: UnlockRequestDetailVmFactory) : ViewModel()

@Composable
fun UnlockRequestDetailScreen(nav: NavController, id: String) {
    val host = hiltViewModel<UnlockRequestDetailHost>()
    val vm = assistedViewModel(id) { host.factory.create(id) }
    val req by vm.state.collectAsState()
    LaunchedEffect(vm.done) { if (vm.done) nav.popBackStack() }

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
                title = "잠금해제 요청",
                subtitle = "팀원이 도움을 요청했어요",
            )

            if (req == null) {
                Spacer(Modifier.height(FaSpacing.xl))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            } else {
                val r = req!!
                Section("정보") {
                    ListRow(headline = "요청자", supporting = r.requesterId.take(8))
                    ListRow(
                        headline = "상태",
                        supporting = when (r.status.name) {
                            "PENDING" -> "대기 중"
                            "APPROVED" -> "승인됨"
                            "EXPIRED" -> "만료됨"
                            else -> r.status.name
                        },
                    )
                    ListRow(
                        headline = "만료",
                        supporting = r.expiresAt.substringBefore('.').replace('T', ' '),
                    )
                }
            }

            vm.error?.let { ErrorText(it) }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = if (vm.approving) "승인 중…" else "승인",
                onClick = vm::approve,
                enabled = !vm.approving && req?.status?.name == "PENDING",
            )
            Spacer(Modifier.height(FaSpacing.md))
        }
    }
}
