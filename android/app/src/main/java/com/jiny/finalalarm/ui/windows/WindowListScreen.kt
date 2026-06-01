package com.jiny.finalalarm.ui.windows

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
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.WindowDto
import com.jiny.finalalarm.ui.Routes
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
        topBar = { TopAppBar(title = { Text("내 알람 시간대") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.WINDOW_EDIT) }) {
                Icon(Icons.Outlined.Add, "추가")
            }
        },
    ) { inner ->
        LazyColumn(modifier = Modifier.padding(inner).fillMaxSize()) {
            items(items) { w ->
                val days = listOf("월", "화", "수", "목", "금", "토", "일")
                    .filterIndexed { i, _ -> (w.daysOfWeek shr i) and 1 == 1 }
                    .joinToString(",")
                ListItem(
                    modifier = Modifier.clickable {
                        nav.navigate(Routes.WINDOW_EDIT_WITH_ID.replace("{id}", w.id))
                    },
                    headlineContent = { Text("${w.startTime} - ${w.endTime}") },
                    supportingContent = { Text("$days · 팀 ${w.teamId.take(8)}") },
                    trailingContent = {
                        TextButton(onClick = { vm.delete(w.id) }) { Text("삭제") }
                    },
                )
                HorizontalDivider()
            }
            if (items.isEmpty()) item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("시간대를 추가해서 팀원이 깨울 수 있게 해보세요")
                }
            }
        }
    }
}
