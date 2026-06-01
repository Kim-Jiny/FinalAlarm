package com.jiny.finalalarm.ui.auth

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.jiny.finalalarm.ui.Routes
import com.jiny.finalalarm.ui.components.HelloHeader
import com.jiny.finalalarm.ui.components.PrimaryButton
import com.jiny.finalalarm.ui.components.SecondaryButton
import com.jiny.finalalarm.ui.theme.FA
import com.jiny.finalalarm.ui.theme.FaSpacing

@Composable
fun OnboardingScreen(nav: NavController) {
    val ctx = LocalContext.current
    var notifGranted by remember { mutableStateOf(checkNotifications(ctx)) }
    var exactGranted by remember { mutableStateOf(checkExactAlarm(ctx)) }
    var batteryIgnored by remember { mutableStateOf(checkBatteryOpt(ctx)) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notifGranted = it }

    // 시스템 설정 다녀온 후 (배터리·정확한 알람) 상태 자동 재확인
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifGranted = checkNotifications(ctx)
                exactGranted = checkExactAlarm(ctx)
                batteryIgnored = checkBatteryOpt(ctx)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FA.BgGradient)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = FaSpacing.lg)
            .padding(top = FaSpacing.md, bottom = FaSpacing.lg),
    ) {
        HelloHeader(
            title = "권한 한 번만 허용해주세요",
            subtitle = "알람이 정확하게 울리려면 필요해요.",
        )

        Spacer(Modifier.height(FaSpacing.xl))

        PermissionRow(
            title = "알림",
            description = "팀원의 깨우기 요청을 푸시로 받아요",
            actionLabel = "허용",
            done = notifGranted,
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
        PermissionRow(
            title = "정확한 알람",
            description = "정해진 시각에 1초의 오차 없이 울려요",
            actionLabel = "설정 열기",
            done = exactGranted,
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            },
        )
        PermissionRow(
            title = "배터리 최적화 제외",
            description = "절전 모드·앱 종료 상태에서도 알람이 울려요. 탭하면 시스템 설정이 열리고, “제한 없음”을 선택하면 돼요.",
            actionLabel = "설정 열기",
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
        PrimaryButton(
            text = "계속",
            onClick = { nav.navigate(Routes.MAIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } } },
        )
        Spacer(Modifier.height(FaSpacing.sm))
        SecondaryButton(
            text = "나중에",
            onClick = { nav.navigate(Routes.MAIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    actionLabel: String,
    done: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FaSpacing.md),
        verticalAlignment = androidx.compose.ui.Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(FaSpacing.sm))
        if (done) {
            Text(
                "✓ 허용됨",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            SecondaryButton(text = actionLabel, onClick = onClick)
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline,
        thickness = 0.5.dp,
    )
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

