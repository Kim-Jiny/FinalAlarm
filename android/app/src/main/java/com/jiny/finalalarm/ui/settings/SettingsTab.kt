package com.jiny.finalalarm.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.jiny.finalalarm.ui.theme.FA
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
fun SettingsTab(nav: NavController, modifier: Modifier = Modifier, vm: SettingsVm = hiltViewModel()) {
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var showPwdDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FA.BgGradient)
            .padding(horizontal = FaSpacing.lg)
            .verticalScroll(rememberScrollState()),
    ) {
        HelloHeader(emoji = "🛠️", title = "설정")

        Section("알람") {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            val ignored = pm.isIgnoringBatteryOptimizations(ctx.packageName)
            ListRow(
                headline = "배터리 최적화",
                supporting = if (ignored) "제외됨" else "제한됨 — 탭하여 변경",
                onClick = {
                    ctx.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${ctx.packageName}"),
                        ),
                    )
                },
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
