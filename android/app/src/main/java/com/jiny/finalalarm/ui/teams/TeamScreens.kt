package com.jiny.finalalarm.ui.teams

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
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.data.api.CreateInviteReq
import com.jiny.finalalarm.ui.util.assistedViewModel
import com.jiny.finalalarm.data.api.CreateTeamReq
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.InviteDto
import com.jiny.finalalarm.data.api.TeamDetail
import com.jiny.finalalarm.ui.Routes
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
            .onFailure { error = it.userMessage(); saving = false }
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
    private val tokenStore: com.jiny.finalalarm.core.auth.TokenStore,
    @Assisted private val teamId: String,
) : ViewModel() {
    private val _state = MutableStateFlow<TeamDetail?>(null)
    val state = _state.asStateFlow()
    var error by mutableStateOf<String?>(null)
    var leftTeam by mutableStateOf(false)
    var myUserId by mutableStateOf<String?>(null)

    init {
        refresh()
        viewModelScope.launch { myUserId = tokenStore.userIdFlow.first() }
    }
    fun refresh() = viewModelScope.launch {
        _state.value = runCatching { api.getTeam(teamId) }.getOrNull()
    }

    fun leaveTeam() = viewModelScope.launch {
        error = null
        runCatching { api.leaveTeam(teamId) }
            .onSuccess { leftTeam = true }
            .onFailure { error = it.userMessage() }
    }

    fun kickMember(userId: String) = viewModelScope.launch {
        error = null
        runCatching { api.kickMember(teamId, userId) }
            .onFailure { error = it.userMessage() }
        refresh()
    }

    fun changeRole(userId: String, role: com.jiny.finalalarm.data.api.TeamRole) = viewModelScope.launch {
        error = null
        runCatching {
            api.changeMemberRole(teamId, userId, com.jiny.finalalarm.data.api.ChangeRoleReq(role))
        }.onFailure { error = it.userMessage() }
        refresh()
    }
}

@AssistedFactory
interface TeamDetailVmFactory { fun create(teamId: String): TeamDetailVm }

@HiltViewModel
class TeamDetailVmHost @Inject constructor(val factory: TeamDetailVmFactory) : ViewModel()

@Composable
fun TeamDetailScreen(nav: NavController, teamId: String) {
    val host = hiltViewModel<TeamDetailVmHost>()
    val vm = assistedViewModel(teamId) { host.factory.create(teamId) }
    val team by vm.state.collectAsState()
    var confirmLeave by remember { mutableStateOf(false) }

    LaunchedEffect(vm.leftTeam) { if (vm.leftTeam) nav.popBackStack() }

    // 본인 role 결정 — TokenStore에서 받아온 user id로 멤버 목록에서 찾기
    val myRoleInTeam = remember(team, vm.myUserId) {
        val uid = vm.myUserId ?: return@remember null
        team?.members?.find { it.user.id == uid }?.role
    }

    Scaffold(topBar = { TopAppBar(title = { Text(team?.name ?: "팀") }) }) { inner ->
        Column(modifier = Modifier.padding(inner).padding(16.dp)) {
            Column {
                Row {
                    OutlinedButton(onClick = { nav.navigate(Routes.TEAM_INVITE.replace("{id}", teamId)) }) {
                        Text("초대")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { nav.navigate(Routes.PUSH_ALARM.replace("{teamId}", teamId)) }) {
                        Text("팀원 깨우기")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { nav.navigate(Routes.INBOX_LIST.replace("{teamId}", teamId)) },
                ) { Text("잠금해제 요청 인박스") }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { confirmLeave = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("팀 탈퇴") }
            }
            Spacer(Modifier.height(16.dp))
            Text("멤버", style = MaterialTheme.typography.titleMedium)
            team?.members?.forEach { m ->
                val isOwner = myRoleInTeam == com.jiny.finalalarm.data.api.TeamRole.OWNER
                val canModerate = isOwner || myRoleInTeam == com.jiny.finalalarm.data.api.TeamRole.ADMIN
                ListItem(
                    headlineContent = { Text(m.user.displayName) },
                    supportingContent = { Text("${m.role}") },
                    trailingContent = {
                        Row {
                            // OWNER만 권한 변경 가능
                            if (isOwner && m.role != com.jiny.finalalarm.data.api.TeamRole.OWNER) {
                                val nextRole = if (m.role == com.jiny.finalalarm.data.api.TeamRole.ADMIN)
                                    com.jiny.finalalarm.data.api.TeamRole.MEMBER
                                else com.jiny.finalalarm.data.api.TeamRole.ADMIN
                                TextButton(onClick = { vm.changeRole(m.user.id, nextRole) }) {
                                    Text("→ $nextRole")
                                }
                            }
                            if (canModerate && m.role != com.jiny.finalalarm.data.api.TeamRole.OWNER) {
                                TextButton(onClick = { vm.kickMember(m.user.id) }) { Text("강퇴") }
                            }
                        }
                    },
                )
                HorizontalDivider()
            }
            vm.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("팀 탈퇴") },
            text = { Text("정말 이 팀에서 나가시겠어요? (오너는 다른 사람에게 권한을 넘긴 뒤에 탈퇴할 수 있어요)") },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; vm.leaveTeam() }) {
                    Text("탈퇴", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("취소") } },
        )
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
            .onFailure { error = it.userMessage() }
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
    val vm = assistedViewModel(teamId) { host.factory.create(teamId) }
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
                com.jiny.finalalarm.ui.components.ListRow(
                    headline = inv.code,
                    supporting = inv.url,
                    trailing = {
                        Row {
                            TextButton(onClick = { clip.setText(AnnotatedString(inv.code)) }) {
                                Text("코드", style = MaterialTheme.typography.bodyMedium)
                            }
                            TextButton(onClick = { clip.setText(AnnotatedString(inv.url)) }) {
                                Text("링크", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    },
                )
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
            .onFailure { error = it.userMessage(); loading = false }
    }
}

@Composable
fun JoinTeamScreen(nav: NavController, initialCode: String = "", vm: JoinTeamVm = hiltViewModel()) {
    LaunchedEffect(initialCode) {
        if (initialCode.isNotBlank() && vm.code.isBlank()) vm.code = initialCode
    }
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
