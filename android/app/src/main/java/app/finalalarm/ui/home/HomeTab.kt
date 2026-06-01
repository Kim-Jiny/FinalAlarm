package app.finalalarm.ui.home

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
import app.finalalarm.data.api.AlarmDto
import app.finalalarm.data.api.AlarmEventDto
import app.finalalarm.data.api.FinalAlarmApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUi(
    val upcoming: List<AlarmDto> = emptyList(),
    val active: List<AlarmEventDto> = emptyList(),
    val loading: Boolean = false,
)

@HiltViewModel
class HomeVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    private val _state = MutableStateFlow(HomeUi())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)
        val alarms = runCatching { api.listAlarms(active = true) }.getOrDefault(emptyList())
        val active = runCatching { api.listActiveEvents() }.getOrDefault(emptyList())
        _state.value = HomeUi(upcoming = alarms, active = active, loading = false)
    }
}

@Composable
fun HomeTab(nav: NavController, modifier: Modifier = Modifier, vm: HomeVm = hiltViewModel()) {
    val ui by vm.state.collectAsState()
    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("진행 중인 알람", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (ui.active.isEmpty()) Text("없음", style = MaterialTheme.typography.bodyMedium)
        }
        items(ui.active) { e ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("이벤트 ${e.id.take(8)} — ${e.state}", style = MaterialTheme.typography.titleSmall)
                    e.senderUserId?.let { Text("발사자: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text("다가올 알람", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        items(ui.upcoming) { a ->
            ListItem(
                headlineContent = { Text(a.label) },
                supportingContent = { Text("${a.timeOfDay ?: a.oneShotAt ?: "?"} • ${a.kind}") },
            )
            HorizontalDivider()
        }
    }
}
