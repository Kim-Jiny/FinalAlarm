package app.finalalarm.ui.auth

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.finalalarm.ui.Routes

@Composable
fun OnboardingScreen(nav: NavController) {
    val ctx = LocalContext.current
    var notifGranted by remember { mutableStateOf(checkNotifications(ctx)) }
    var exactGranted by remember { mutableStateOf(checkExactAlarm(ctx)) }
    var batteryIgnored by remember { mutableStateOf(checkBatteryOpt(ctx)) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notifGranted = it }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("권한 설정", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Text("알람이 제대로 동작하려면 아래 권한이 필요해요.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        PermissionRow(
            title = "알림",
            done = notifGranted,
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
        PermissionRow(
            title = "정확한 알람 (Android 12+)",
            done = exactGranted,
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            },
        )
        PermissionRow(
            title = "배터리 최적화 제외",
            done = batteryIgnored,
            onClick = {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${ctx.packageName}"),
                    ),
                )
            },
        )

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { nav.navigate(Routes.MAIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("계속") }
    }
}

@Composable
private fun PermissionRow(title: String, done: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        if (done) Text("✓", style = MaterialTheme.typography.titleLarge)
        else OutlinedButton(onClick = onClick) { Text("허용") }
    }
}

private fun checkNotifications(ctx: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return androidx.core.content.ContextCompat.checkSelfPermission(
        ctx, Manifest.permission.POST_NOTIFICATIONS,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun checkExactAlarm(ctx: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return am.canScheduleExactAlarms()
}

private fun checkBatteryOpt(ctx: Context): Boolean {
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}
