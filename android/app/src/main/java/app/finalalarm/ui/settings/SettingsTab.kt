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
import app.finalalarm.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsVm @Inject constructor(private val authRepo: AuthRepository) : ViewModel() {
    fun logout() = viewModelScope.launch { authRepo.logout() }
}

@Composable
fun SettingsTab(nav: NavController, modifier: Modifier = Modifier, vm: SettingsVm = hiltViewModel()) {
    val ctx = LocalContext.current
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
            onClick = vm::logout,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("로그아웃") }
    }
}
