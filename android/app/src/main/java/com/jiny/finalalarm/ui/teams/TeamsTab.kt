package com.jiny.finalalarm.ui.teams

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
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.TeamSummary
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = FaSpacing.screen),
    ) {
        item {
            Spacer(Modifier.height(FaSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    "팀",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { nav.navigate(Routes.TEAM_CREATE) }) {
                    Text("만들기", style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.height(FaSpacing.md))
            ListRow(
                headline = "초대 코드로 가입",
                onClick = { nav.navigate(Routes.joinTeamRoute()) },
            )
        }

        if (teams.isEmpty()) {
            item { EmptyState("팀이 없어요. 만들거나 가입해보세요.") }
        } else {
            items(teams) { t ->
                ListRow(
                    headline = t.name,
                    supporting = when (t.role) {
                        com.jiny.finalalarm.data.api.TeamRole.OWNER -> "오너"
                        com.jiny.finalalarm.data.api.TeamRole.ADMIN -> "관리자"
                        com.jiny.finalalarm.data.api.TeamRole.MEMBER -> "멤버"
                    },
                    onClick = { nav.navigate(Routes.TEAM_DETAIL.replace("{id}", t.id)) },
                )
            }
        }
    }
}
