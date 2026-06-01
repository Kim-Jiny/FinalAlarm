package com.jiny.finalalarm.ui.teams

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
import com.jiny.finalalarm.data.api.TeamSummary
import com.jiny.finalalarm.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamsTabVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    private val _state = MutableStateFlow<List<TeamSummary>>(emptyList())
    val state = _state.asStateFlow()

    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = runCatching { api.listTeams() }.getOrDefault(emptyList())
    }
}

@Composable
fun TeamsTab(nav: NavController, modifier: Modifier = Modifier, vm: TeamsTabVm = hiltViewModel()) {
    val teams by vm.state.collectAsState()
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.TEAM_CREATE) }) {
                Icon(Icons.Outlined.Add, "팀 추가")
            }
        },
        modifier = modifier,
    ) { inner ->
        Column(modifier = Modifier.padding(inner).padding(8.dp)) {
            OutlinedButton(
                onClick = { nav.navigate(Routes.joinTeamRoute()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("초대 코드로 가입") }
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(teams) { t ->
                    ListItem(
                        headlineContent = { Text(t.name) },
                        supportingContent = { Text(t.role.name) },
                        modifier = Modifier.fillMaxWidth().clickable {
                            nav.navigate(Routes.TEAM_DETAIL.replace("{id}", t.id))
                        },
                    )
                    HorizontalDivider()
                }
                if (teams.isEmpty()) item {
                    Text("팀이 없어요. 만들거나 가입해보세요.", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
