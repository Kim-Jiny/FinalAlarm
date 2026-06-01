package app.finalalarm.ui.alarms

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import app.finalalarm.core.work.AlarmRescheduleWorker
import app.finalalarm.data.api.AlarmDto
import app.finalalarm.data.api.FinalAlarmApi
import app.finalalarm.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmListVm @Inject constructor(
    private val api: FinalAlarmApi,
    @ApplicationContext private val ctx: Context,
) : ViewModel() {
    private val _state = MutableStateFlow<List<AlarmDto>>(emptyList())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = runCatching { api.listAlarms() }.getOrDefault(emptyList())
    }

    fun toggle(id: String, active: Boolean) = viewModelScope.launch {
        runCatching {
            api.updateAlarm(
                id,
                mapOf("active" to kotlinx.serialization.json.JsonPrimitive(active)),
            )
        }.onSuccess { AlarmRescheduleWorker.enqueue(ctx) }
        refresh()
    }

    fun delete(id: String) = viewModelScope.launch {
        runCatching { api.deleteAlarm(id) }
            .onSuccess { AlarmRescheduleWorker.enqueue(ctx) }
        refresh()
    }
}

@Composable
fun AlarmListTab(nav: NavController, modifier: Modifier = Modifier, vm: AlarmListVm = hiltViewModel()) {
    val items by vm.state.collectAsState()
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.ALARM_EDIT) }) {
                Icon(Icons.Outlined.Add, contentDescription = "추가")
            }
        },
        modifier = modifier,
    ) { inner ->
        LazyColumn(modifier = Modifier.padding(inner).fillMaxSize()) {
            items(items) { a ->
                ListItem(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            nav.navigate(Routes.ALARM_EDIT_WITH_ID.replace("{id}", a.id))
                        },
                    headlineContent = { Text(a.label) },
                    supportingContent = {
                        Text("${a.timeOfDay ?: a.oneShotAt ?: "?"} • ${a.kind}")
                    },
                    trailingContent = {
                        Switch(checked = a.active, onCheckedChange = { vm.toggle(a.id, it) })
                    },
                )
                HorizontalDivider()
            }
            if (items.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("알람을 추가해보세요")
                    }
                }
            }
        }
    }
}
