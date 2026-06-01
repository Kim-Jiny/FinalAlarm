package com.jiny.finalalarm.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.data.api.AlarmDto
import com.jiny.finalalarm.data.api.AlarmEventDto
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.ui.components.EmptyState
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.components.Section
import com.jiny.finalalarm.ui.theme.FaSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUi(
    val upcoming: List<AlarmDto> = emptyList(),
    val active: List<AlarmEventDto> = emptyList(),
)

@HiltViewModel
class HomeVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    private val _state = MutableStateFlow(HomeUi())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val alarms = runCatching { api.listAlarms(active = true) }.getOrDefault(emptyList())
        val active = runCatching { api.listActiveEvents() }.getOrDefault(emptyList())
        _state.value = HomeUi(upcoming = alarms, active = active)
    }
}

@Composable
fun HomeTab(nav: NavController, modifier: Modifier = Modifier, vm: HomeVm = hiltViewModel()) {
    val ui by vm.state.collectAsState()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = FaSpacing.screen),
    ) {
        item {
            Spacer(Modifier.height(FaSpacing.md))
            Text("홈", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(FaSpacing.xs))
            Text(
                "오늘 ${ui.upcoming.size}개 예정",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (ui.active.isNotEmpty()) {
            item { Section("진행 중") {} }
            items(ui.active) { e ->
                ListRow(
                    headline = "${e.state}",
                    supporting = e.senderUserId?.let { "팀원이 깨움" },
                )
            }
        }

        item { Section("다가올 알람") {} }
        if (ui.upcoming.isEmpty()) {
            item { EmptyState("등록된 알람이 없습니다") }
        } else {
            items(ui.upcoming) { a ->
                ListRow(
                    headline = a.label,
                    supporting = a.timeOfDay ?: a.oneShotAt ?: "—",
                )
            }
        }
    }
}
