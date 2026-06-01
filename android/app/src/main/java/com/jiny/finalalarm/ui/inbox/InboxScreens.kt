package com.jiny.finalalarm.ui.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.ui.util.assistedViewModel
import com.jiny.finalalarm.data.api.UnlockRequestDto
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

    Scaffold(topBar = { TopAppBar(title = { Text("잠금해제 요청") }) }) { inner ->
        Column(modifier = Modifier.padding(inner).padding(24.dp)) {
            req?.let { r ->
                Text("요청자: ${r.requesterId.take(8)}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("상태: ${r.status}")
                Text("만료: ${r.expiresAt}")
            } ?: CircularProgressIndicator()

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = vm::approve,
                enabled = !vm.approving && req?.status?.name == "PENDING",
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (vm.approving) "승인 중…" else "승인") }
            vm.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
