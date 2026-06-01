package com.jiny.finalalarm.ui.teams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.data.api.CreateInviteReq
import com.jiny.finalalarm.data.api.CreateTeamReq
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.InviteDto
import com.jiny.finalalarm.data.api.TeamDetail
import com.jiny.finalalarm.data.api.TeamRole
import com.jiny.finalalarm.ui.Routes
import com.jiny.finalalarm.ui.components.ErrorText
import com.jiny.finalalarm.ui.components.FaTextField
import com.jiny.finalalarm.ui.components.HelloHeader
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.components.OnResume
import com.jiny.finalalarm.ui.components.PrimaryButton
import com.jiny.finalalarm.ui.components.SecondaryButton
import com.jiny.finalalarm.ui.components.Section
import com.jiny.finalalarm.ui.components.WarmBackground
import com.jiny.finalalarm.ui.theme.FaSpacing
import com.jiny.finalalarm.ui.util.assistedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = FaSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = FaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { nav.popBackStack() }) { Text("뒤로") }
                Spacer(Modifier.weight(1f))
            }
            HelloHeader(
                title = "새 팀 만들기",
                subtitle = "함께 일어날 친구들을 모아봐요",
            )
            Spacer(Modifier.height(FaSpacing.xl))
            FaTextField(vm.name, { vm.name = it }, "팀 이름")
            vm.error?.let { ErrorText(it) }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = if (vm.saving) "만드는 중…" else "만들기",
                onClick = vm::save,
                enabled = !vm.saving && vm.name.isNotBlank(),
            )
            Spacer(Modifier.height(FaSpacing.md))
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

    fun changeRole(userId: String, role: TeamRole) = viewModelScope.launch {
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
    OnResume { vm.refresh() }
    val team by vm.state.collectAsState()
    var confirmLeave by remember { mutableStateOf(false) }

    LaunchedEffect(vm.leftTeam) { if (vm.leftTeam) nav.popBackStack() }

    val myRoleInTeam = remember(team, vm.myUserId) {
        val uid = vm.myUserId ?: return@remember null
        team?.members?.find { it.user.id == uid }?.role
    }

    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FaSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = FaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { nav.popBackStack() }) { Text("뒤로") }
                Spacer(Modifier.weight(1f))
            }
            HelloHeader(
                title = team?.name ?: "팀",
                subtitle = when (myRoleInTeam) {
                    TeamRole.OWNER -> "내가 오너예요"
                    TeamRole.ADMIN -> "관리자"
                    TeamRole.MEMBER -> "멤버"
                    null -> null
                },
            )

            Section("동작") {
                ListRow(
                    headline = "초대",
                    supporting = "친구를 팀에 초대",
                    onClick = { nav.navigate(Routes.TEAM_INVITE.replace("{id}", teamId)) },
                )
                ListRow(
                    headline = "팀원 깨우기",
                    supporting = "지금 알람 보내기",
                    onClick = { nav.navigate(Routes.PUSH_ALARM.replace("{teamId}", teamId)) },
                )
                ListRow(
                    headline = "잠금해제 요청 인박스",
                    onClick = { nav.navigate(Routes.INBOX_LIST.replace("{teamId}", teamId)) },
                )
            }

            Section("멤버") {
                team?.members?.forEach { m ->
                    val isOwner = myRoleInTeam == TeamRole.OWNER
                    val canModerate = isOwner || myRoleInTeam == TeamRole.ADMIN
                    val roleLabel = when (m.role) {
                        TeamRole.OWNER -> "오너"
                        TeamRole.ADMIN -> "관리자"
                        TeamRole.MEMBER -> "멤버"
                    }
                    ListRow(
                        headline = m.user.displayName,
                        supporting = roleLabel,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOwner && m.role != TeamRole.OWNER) {
                                    val nextRole = if (m.role == TeamRole.ADMIN) TeamRole.MEMBER else TeamRole.ADMIN
                                    val nextLabel = if (nextRole == TeamRole.ADMIN) "관리자로" else "멤버로"
                                    TextButton(onClick = { vm.changeRole(m.user.id, nextRole) }) {
                                        Text(nextLabel, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                if (canModerate && m.role != TeamRole.OWNER) {
                                    TextButton(onClick = { vm.kickMember(m.user.id) }) {
                                        Text("강퇴", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        },
                    )
                }
            }

            vm.error?.let { ErrorText(it) }
            Spacer(Modifier.height(FaSpacing.xl))
            SecondaryButton(
                text = "팀 탈퇴",
                onClick = { confirmLeave = true },
                destructive = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(FaSpacing.xxl))
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("팀 탈퇴", style = MaterialTheme.typography.titleLarge) },
            text = { Text("정말 나가시겠어요? 오너는 권한을 넘긴 뒤 탈퇴할 수 있어요.") },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; vm.leaveTeam() }) {
                    Text("탈퇴", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("취소") } },
            containerColor = MaterialTheme.colorScheme.surface,
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

    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = FaSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = FaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { nav.popBackStack() }) { Text("뒤로") }
                Spacer(Modifier.weight(1f))
            }
            HelloHeader(
                title = "초대",
                subtitle = "코드나 링크를 친구에게 보내세요",
            )
            Spacer(Modifier.height(FaSpacing.lg))
            PrimaryButton(
                text = if (vm.loading) "만드는 중…" else "+ 새 초대 코드",
                onClick = vm::create,
                enabled = !vm.loading,
            )
            vm.error?.let { ErrorText(it) }

            Section("초대 코드") {
                if (invites.isEmpty()) {
                    Text(
                        "아직 초대 코드가 없어요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    invites.forEach { inv ->
                        ListRow(
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
            Spacer(Modifier.height(FaSpacing.xxl))
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
    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = FaSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = FaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { nav.popBackStack() }) { Text("뒤로") }
                Spacer(Modifier.weight(1f))
            }
            HelloHeader(
                title = "초대 코드 입력",
                subtitle = "친구가 보낸 코드를 적어주세요",
            )
            Spacer(Modifier.height(FaSpacing.xl))
            FaTextField(vm.code, { vm.code = it }, "예: ABC123")
            vm.error?.let { ErrorText(it) }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = if (vm.loading) "가입 중…" else "가입",
                onClick = vm::redeem,
                enabled = !vm.loading && vm.code.isNotBlank(),
            )
            Spacer(Modifier.height(FaSpacing.md))
        }
    }
}
