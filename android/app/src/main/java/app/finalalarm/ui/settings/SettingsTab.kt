package app.finalalarm.ui.settings

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
import app.finalalarm.core.network.userMessage
import app.finalalarm.data.AuthRepository
import app.finalalarm.data.api.FinalAlarmApi
import app.finalalarm.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsVm @Inject constructor(
    private val authRepo: AuthRepository,
    private val api: FinalAlarmApi,
) : ViewModel() {
    var error by mutableStateOf<String?>(null)
    var deleted by mutableStateOf(false)

    fun logout() = viewModelScope.launch { authRepo.logout() }

    fun deleteAccount() = viewModelScope.launch {
        error = null
        runCatching { api.deleteMe() }
            .onSuccess {
                runCatching { authRepo.logout() }   // refresh 토큰 무효화 시도 (이미 401일 수 있음)
                deleted = true
            }
            .onFailure { error = it.userMessage() }
    }
}

@Composable
fun SettingsTab(nav: NavController, modifier: Modifier = Modifier, vm: SettingsVm = hiltViewModel()) {
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

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

        Spacer(Modifier.height(16.dp))
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
}
