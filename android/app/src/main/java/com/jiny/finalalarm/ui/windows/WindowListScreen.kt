package com.jiny.finalalarm.ui.windows

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
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.WindowDto
import com.jiny.finalalarm.ui.Routes
import com.jiny.finalalarm.ui.components.EmptyState
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.theme.FaSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WindowListVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    private val _state = MutableStateFlow<List<WindowDto>>(emptyList())
    val state = _state.asStateFlow()

    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = runCatching { api.listWindows() }.getOrDefault(emptyList())
    }

    fun delete(id: String) = viewModelScope.launch {
        runCatching { api.deleteWindow(id) }
        refresh()
    }
}

@Composable
fun WindowListScreen(nav: NavController, vm: WindowListVm = hiltViewModel()) {
    val items by vm.state.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("알람 시간대", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = { nav.popBackStack() }) { Text("뒤로") } },
                actions = {
                    TextButton(onClick = { nav.navigate(Routes.WINDOW_EDIT) }) {
                        Text("추가", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = FaSpacing.screen),
        ) {
            if (items.isEmpty()) {
                item { EmptyState("시간대를 추가해서 팀원이 깨울 수 있게 해보세요") }
            } else {
                items(items) { w ->
                    val days = listOf("월", "화", "수", "목", "금", "토", "일")
                        .filterIndexed { i, _ -> (w.daysOfWeek shr i) and 1 == 1 }
                        .joinToString("·")
                    ListRow(
                        headline = "${w.startTime} – ${w.endTime}",
                        supporting = days,
                        onClick = {
                            nav.navigate(Routes.WINDOW_EDIT_WITH_ID.replace("{id}", w.id))
                        },
                        trailing = {
                            TextButton(onClick = { vm.delete(w.id) }) {
                                Text("삭제", color = MaterialTheme.colorScheme.error)
                            }
                        },
                    )
                }
            }
        }
    }
}
