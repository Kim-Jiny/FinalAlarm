package com.jiny.finalalarm.ui.missions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.MissionDto
import com.jiny.finalalarm.ui.Routes
import com.jiny.finalalarm.ui.components.EmptyState
import com.jiny.finalalarm.ui.components.ErrorText
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.theme.FaSpacing
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
        runCatching { api.deleteMission(id) }.onFailure { error = it.userMessage() }
        refresh()
    }
}

@Composable
fun MissionListScreen(nav: NavController, vm: MissionListVm = hiltViewModel()) {
    com.jiny.finalalarm.ui.components.OnResume { vm.refresh() }
    val items by vm.state.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("미션", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    TextButton(onClick = { nav.popBackStack() }) { Text("뒤로") }
                },
                actions = {
                    TextButton(onClick = { nav.navigate(Routes.MISSION_EDIT) }) {
                        Text("추가", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).padding(horizontal = FaSpacing.screen)) {
            vm.error?.let { ErrorText(it) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = FaSpacing.xxl),
            ) {
                if (items.isEmpty()) {
                    item { EmptyState("미션을 추가해보세요") }
                } else {
                    items(items) { m ->
                        ListRow(
                            headline = m.name,
                            supporting = "${m.type}" + if (m.isDefault) " · 기본" else "",
                            onClick = {
                                nav.navigate(Routes.MISSION_EDIT_WITH_ID.replace("{id}", m.id))
                            },
                            trailing = {
                                TextButton(onClick = { vm.delete(m.id) }) {
                                    Text("삭제", color = MaterialTheme.colorScheme.error)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
