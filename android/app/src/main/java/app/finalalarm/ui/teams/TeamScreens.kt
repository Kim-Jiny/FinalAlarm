package app.finalalarm.ui.teams

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import app.finalalarm.data.api.CreateInviteReq
import app.finalalarm.data.api.CreateTeamReq
import app.finalalarm.data.api.FinalAlarmApi
import app.finalalarm.data.api.InviteDto
import app.finalalarm.data.api.TeamDetail
import app.finalalarm.ui.Routes
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---- 팀 만들기 ----
@HiltViewModel
class TeamCreateVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    var name by mutableStateOf("")
    var saving by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var savedId by mutableStateOf<String?>(null)

    fun save() = viewModelScope.launch {
        saving = true; error = null
        runCatching { api.createTeam(CreateTeamReq(name)) }
            .onSuccess { savedId = it.id; saving = false }
            .onFailure { error = it.message; saving = false }
    }
}

@Composable
fun TeamCreateScreen(nav: NavController, vm: TeamCreateVm = hiltViewModel()) {
    LaunchedEffect(vm.savedId) {
        vm.savedId?.let { nav.popBackStack(); nav.navigate(Routes.TEAM_DETAIL.replace("{id}", it)) }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("팀 만들기") }) }) { inner ->
        Column(modifier = Modifier.padding(inner).padding(24.dp)) {
            OutlinedTextField(vm.name, { vm.name = it }, label = { Text("팀 이름") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = vm::save, enabled = !vm.saving && vm.name.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.saving) "만드는 중…" else "만들기")
            }
            vm.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

// ---- 팀 상세 ----
class TeamDetailVm @AssistedInject constructor(
    private val api: FinalAlarmApi,
    @Assisted private val teamId: String,
) : ViewModel() {
    private val _state = MutableStateFlow<TeamDetail?>(null)
    val state = _state.asStateFlow()

    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = runCatching { api.getTeam(teamId) }.getOrNull()
    }
}

@AssistedFactory
interface TeamDetailVmFactory { fun create(teamId: String): TeamDetailVm }

@HiltViewModel
class TeamDetailVmHost @Inject constructor(val factory: TeamDetailVmFactory) : ViewModel()

@Composable
fun TeamDetailScreen(nav: NavController, teamId: String) {
    val host = hiltViewModel<TeamDetailVmHost>()
    val vm = remember(teamId) { host.factory.create(teamId) }
    val team by vm.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text(team?.name ?: "팀") }) }) { inner ->
        Column(modifier = Modifier.padding(inner).padding(16.dp)) {
            Row {
                OutlinedButton(onClick = { nav.navigate(Routes.TEAM_INVITE.replace("{id}", teamId)) }) {
                    Text("초대")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { nav.navigate(Routes.PUSH_ALARM.replace("{teamId}", teamId)) }) {
                    Text("팀원 깨우기")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("멤버", style = MaterialTheme.typography.titleMedium)
            team?.members?.forEach { m ->
                ListItem(
                    headlineContent = { Text(m.user.displayName) },
                    supportingContent = { Text("${m.role}") },
                )
                HorizontalDivider()
            }
        }
    }
}

// ---- 초대 ----
class TeamInviteVm @AssistedInject constructor(
    private val api: FinalAlarmApi,
    @Assisted private val teamId: String,
) : ViewModel() {
    private val _invites = MutableStateFlow<List<InviteDto>>(emptyList())
    val invites = _invites.asStateFlow()
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _invites.value = runCatching { api.listInvites(teamId) }.getOrDefault(emptyList())
    }
    fun create() = viewModelScope.launch {
        loading = true; error = null
        runCatching { api.createInvite(teamId, CreateInviteReq(expiresInDays = 7)) }
            .onFailure { error = it.message }
        loading = false
        refresh()
    }
}

@AssistedFactory
interface TeamInviteVmFactory { fun create(teamId: String): TeamInviteVm }

@HiltViewModel
class TeamInviteVmHost @Inject constructor(val factory: TeamInviteVmFactory) : ViewModel()

@Composable
fun TeamInviteScreen(nav: NavController, teamId: String) {
    val host = hiltViewModel<TeamInviteVmHost>()
    val vm = remember(teamId) { host.factory.create(teamId) }
    val invites by vm.invites.collectAsState()
    val clip: ClipboardManager = LocalClipboardManager.current

    Scaffold(topBar = { TopAppBar(title = { Text("초대") }) }) { inner ->
        Column(modifier = Modifier.padding(inner).padding(16.dp)) {
            Button(onClick = vm::create, enabled = !vm.loading, modifier = Modifier.fillMaxWidth()) {
                Text("새 초대 코드 만들기")
            }
            vm.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(16.dp))
            invites.forEach { inv ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("코드: ${inv.code}", style = MaterialTheme.typography.titleMedium)
                        Text("링크: ${inv.url}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Row {
                            TextButton(onClick = { clip.setText(AnnotatedString(inv.code)) }) { Text("코드 복사") }
                            TextButton(onClick = { clip.setText(AnnotatedString(inv.url)) }) { Text("링크 복사") }
                        }
                    }
                }
            }
        }
    }
}

// ---- 코드로 가입 ----
@HiltViewModel
class JoinTeamVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    var code by mutableStateOf("")
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var joinedTeamId by mutableStateOf<String?>(null)

    fun redeem() = viewModelScope.launch {
        loading = true; error = null
        runCatching { api.redeemInvite(code.trim().uppercase()) }
            .onSuccess { joinedTeamId = it.teamId; loading = false }
            .onFailure { error = it.message; loading = false }
    }
}

@Composable
fun JoinTeamScreen(nav: NavController, vm: JoinTeamVm = hiltViewModel()) {
    LaunchedEffect(vm.joinedTeamId) {
        vm.joinedTeamId?.let {
            nav.popBackStack()
            nav.navigate(Routes.TEAM_DETAIL.replace("{id}", it))
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("초대 코드 입력") }) }) { inner ->
        Column(modifier = Modifier.padding(inner).padding(24.dp)) {
            OutlinedTextField(vm.code, { vm.code = it }, label = { Text("초대 코드") },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = vm::redeem, enabled = !vm.loading && vm.code.isNotBlank(),
                modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.loading) "가입 중…" else "가입")
            }
            vm.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
