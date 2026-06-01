package com.jiny.finalalarm.ui.alarms

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.core.work.AlarmRescheduleWorker
import com.jiny.finalalarm.data.api.AlarmDto
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.ui.Routes
import com.jiny.finalalarm.ui.components.EmptyState
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.theme.FaSpacing
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
fun AlarmListTab(
    nav: NavController,
    padding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(),
    vm: AlarmListVm = hiltViewModel(),
) {
    com.jiny.finalalarm.ui.components.OnResume { vm.refresh() }
    val items by vm.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = FaSpacing.lg,
            end = FaSpacing.lg,
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + FaSpacing.xxl,
        ),
    ) {
        item {
            Spacer(Modifier.height(FaSpacing.xl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    "알람",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.weight(1f),
                )
                com.jiny.finalalarm.ui.components.SecondaryButton(
                    text = "+ 추가",
                    onClick = { nav.navigate(Routes.ALARM_EDIT) },
                )
            }
            Spacer(Modifier.height(FaSpacing.md))
        }

        if (items.isEmpty()) {
            item { EmptyState(text = "조용하네요.\n첫 알람을 만들어봐요") }
        } else {
            items(items) { a ->
                ListRow(
                    headline = a.label,
                    supporting = "${a.timeOfDay ?: a.oneShotAt ?: "?"} · ${if (a.kind.name == "TEAM_APPROVAL") "팀 승인" else "개인"}",
                    onClick = {
                        nav.navigate(Routes.ALARM_EDIT_WITH_ID.replace("{id}", a.id))
                    },
                    trailing = {
                        Switch(
                            checked = a.active,
                            onCheckedChange = { vm.toggle(a.id, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                    },
                )
            }
        }
    }
}
