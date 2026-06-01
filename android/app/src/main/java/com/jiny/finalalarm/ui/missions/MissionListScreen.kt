package com.jiny.finalalarm.ui.missions

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
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.MissionDto
import com.jiny.finalalarm.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MissionListVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    private val _state = MutableStateFlow<List<MissionDto>>(emptyList())
    val state = _state.asStateFlow()
    var error by mutableStateOf<String?>(null)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = runCatching { api.listMissions() }.getOrDefault(emptyList())
    }

    fun delete(id: String) = viewModelScope.launch {
        error = null
        runCatching { api.deleteMission(id) }
            .onFailure { error = it.userMessage() }
        refresh()
    }
}

@Composable
fun MissionListScreen(nav: NavController, vm: MissionListVm = hiltViewModel()) {
    val items by vm.state.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("내 미션") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.MISSION_EDIT) }) {
                Icon(Icons.Outlined.Add, "추가")
            }
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner)) {
            vm.error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) { Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items) { m ->
                    ListItem(
                        modifier = Modifier.clickable {
                            nav.navigate(Routes.MISSION_EDIT_WITH_ID.replace("{id}", m.id))
                        },
                        headlineContent = { Text(m.name) },
                        supportingContent = { Text("${m.type}" + if (m.isDefault) " · 기본" else "") },
                        trailingContent = {
                            TextButton(onClick = { vm.delete(m.id) }) { Text("삭제") }
                        },
                    )
                    HorizontalDivider()
                }
                if (items.isEmpty()) item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("미션이 없어요. 추가해보세요.")
                    }
                }
            }
        }
    }
}
