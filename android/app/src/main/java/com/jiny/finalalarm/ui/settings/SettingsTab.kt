package com.jiny.finalalarm.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.data.AuthRepository
import com.jiny.finalalarm.data.api.ChangePasswordReq
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.ui.Routes
import com.jiny.finalalarm.ui.components.ErrorText
import com.jiny.finalalarm.ui.components.FaTextField
import com.jiny.finalalarm.ui.components.HelloHeader
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.components.Section
import com.jiny.finalalarm.ui.theme.FaSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsVm @Inject constructor(
    private val authRepo: AuthRepository,
    private val api: FinalAlarmApi,
) : ViewModel() {
    var error by mutableStateOf<String?>(null)
    var pwdError by mutableStateOf<String?>(null)
    var pwdSuccess by mutableStateOf(false)
    var deleted by mutableStateOf(false)

    fun logout() = viewModelScope.launch { authRepo.logout() }

    fun deleteAccount() = viewModelScope.launch {
        error = null
        runCatching { api.deleteMe() }
            .onSuccess {
                runCatching { authRepo.logout() }
                deleted = true
            }
            .onFailure { error = it.userMessage() }
    }

    fun changePassword(current: String, new: String) = viewModelScope.launch {
        pwdError = null
        pwdSuccess = false
        runCatching { api.changePassword(ChangePasswordReq(current, new)) }
            .onSuccess { pwdSuccess = true }
            .onFailure { pwdError = it.userMessage() }
    }

    fun clearPwdState() { pwdError = null; pwdSuccess = false }
}

@Composable
fun SettingsTab(
    nav: NavController,
    padding: PaddingValues = PaddingValues(),
    vm: SettingsVm = hiltViewModel(),
) {
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var showPwdDialog by remember { mutableStateOf(false) }
    val pm = remember { ctx.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var batteryIgnored by remember { mutableStateOf(pm.isIgnoringBatteryOptimizations(ctx.packageName)) }
    com.jiny.finalalarm.ui.components.OnResume {
        batteryIgnored = pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = FaSpacing.lg,
                end = FaSpacing.lg,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            ),
    ) {
        HelloHeader(title = "설정")

        Section("알람") {
            ListRow(
                headline = "배터리 최적화",
                supporting = if (batteryIgnored) "제외됨 — 알람 정확히 울려요" else "제한됨 — 탭하여 변경",
                onClick = {
                    val direct = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${ctx.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val launched = runCatching { ctx.startActivity(direct) }.isSuccess
                    if (!launched) runCatching { ctx.startActivity(fallback) }
                },
                trailing = if (batteryIgnored) ({
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "배터리 최적화 제외됨",
                        tint = com.jiny.finalalarm.ui.theme.FA.Success,
                    )
                }) else null,
            )
        }

        Section("관리") {
            ListRow("내 미션", onClick = { nav.navigate(Routes.MISSION_LIST) })
            ListRow("알람 시간대", onClick = { nav.navigate(Routes.WINDOW_LIST) })
            ListRow("히스토리", onClick = { nav.navigate(Routes.HISTORY) })
        }

        Section("계정") {
            ListRow("비밀번호 변경", onClick = { showPwdDialog = true })
            ListRow("로그아웃", onClick = { vm.logout() })
            ListRow(
                "계정 삭제",
                destructive = true,
                onClick = { confirmDelete = true },
            )
        }

        vm.error?.let { ErrorText(it) }
        Spacer(Modifier.height(FaSpacing.xxl))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("계정 삭제", style = MaterialTheme.typography.titleLarge) },
            text = { Text("알람·팀 기록이 모두 사라집니다. 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteAccount()
                }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("취소") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    if (showPwdDialog) {
        ChangePasswordDialog(vm = vm, onDismiss = { showPwdDialog = false; vm.clearPwdState() })
    }
}

@Composable
private fun ChangePasswordDialog(vm: SettingsVm, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    LaunchedEffect(vm.pwdSuccess) { if (vm.pwdSuccess) onDismiss() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("비밀번호 변경", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                FaTextField(current, { current = it }, "현재 비밀번호",
                    visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(FaSpacing.sm))
                FaTextField(new, { new = it }, "새 비밀번호 (8자 이상)",
                    visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(FaSpacing.sm))
                FaTextField(confirm, { confirm = it }, "새 비밀번호 확인",
                    visualTransformation = PasswordVisualTransformation())
                vm.pwdError?.let { ErrorText(it) }
                if (new.isNotEmpty() && confirm.isNotEmpty() && new != confirm) {
                    ErrorText("비밀번호가 일치하지 않습니다")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = current.isNotBlank() && new.length >= 8 && new == confirm,
                onClick = { vm.changePassword(current, new) },
            ) { Text("변경") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
