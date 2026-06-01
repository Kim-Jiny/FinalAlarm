package com.jiny.finalalarm.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.data.AuthRepository
import com.jiny.finalalarm.data.api.ChangePasswordReq
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.ui.Routes
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
            .onSuccess {
                pwdSuccess = true
                // 서버가 모든 refresh 토큰 revoke → 다음 401 시 강제 로그아웃 발생
            }
            .onFailure { pwdError = it.userMessage() }
    }

    fun clearPwdState() { pwdError = null; pwdSuccess = false }
}

@Composable
fun SettingsTab(nav: NavController, modifier: Modifier = Modifier, vm: SettingsVm = hiltViewModel()) {
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var showPwdDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("설정", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        ListItem(
            headlineContent = { Text("배터리 최적화 제외") },
            supportingContent = {
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
                Text(if (pm.isIgnoringBatteryOptimizations(ctx.packageName)) "허용됨" else "제한됨 (탭하여 변경)")
            },
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Button(
            onClick = {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${ctx.packageName}"),
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("배터리 설정 열기") }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { nav.navigate(Routes.MISSION_LIST) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("내 미션 관리") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { nav.navigate(Routes.WINDOW_LIST) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("알람 시간대 관리") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { nav.navigate(Routes.HISTORY) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("알람 히스토리") }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { showPwdDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("비밀번호 변경") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = vm::logout,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("로그아웃") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { confirmDelete = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text("계정 삭제") }

        vm.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("계정 삭제") },
            text = { Text("정말 계정을 삭제할까요? 알람·팀 기록은 모두 사라지고 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        vm.deleteAccount()
                    },
                ) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("취소") }
            },
        )
    }

    if (showPwdDialog) {
        ChangePasswordDialog(
            vm = vm,
            onDismiss = { showPwdDialog = false; vm.clearPwdState() },
        )
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
        title = { Text("비밀번호 변경") },
        text = {
            Column {
                OutlinedTextField(
                    value = current, onValueChange = { current = it },
                    label = { Text("현재 비밀번호") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = new, onValueChange = { new = it },
                    label = { Text("새 비밀번호 (8자 이상)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm, onValueChange = { confirm = it },
                    label = { Text("새 비밀번호 확인") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                vm.pwdError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                if (new.isNotEmpty() && confirm.isNotEmpty() && new != confirm) {
                    Spacer(Modifier.height(4.dp))
                    Text("새 비밀번호가 일치하지 않습니다", color = MaterialTheme.colorScheme.error)
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
    )
}
