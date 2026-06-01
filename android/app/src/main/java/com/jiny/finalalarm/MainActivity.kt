package com.jiny.finalalarm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import com.jiny.finalalarm.ui.theme.FinalAlarmTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.jiny.finalalarm.core.sync.EventReconcileWorker
import com.jiny.finalalarm.ui.RootNav
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        EventReconcileWorker.enqueue(applicationContext)

        val inviteCode = extractInviteCode(intent)
        setContent {
            FinalAlarmTheme {
                val focusManager = LocalFocusManager.current
                val noInteraction = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        // 배경 탭 → 포커스 해제 → 키보드 내려감. 자식 clickable이 먼저 소비하므로 빈 영역 탭에만 동작.
                        .clickable(
                            interactionSource = noInteraction,
                            indication = null,
                            onClick = { focusManager.clearFocus() },
                        )
                        // 키보드 영역만큼 컨텐츠를 위로 밀어줌 — TextField가 가리지 않게.
                        .imePadding(),
                ) {
                    RootNav(initialInviteCode = inviteCode)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (extractInviteCode(intent) != null) {
            recreate()
        }
    }

    private fun extractInviteCode(intent: Intent?): String? {
        val data: Uri = intent?.data ?: return null
        if (data.scheme != "https" || data.host != "finalalarm.app") return null
        val segments = data.pathSegments
        if (segments.size < 2 || segments[0] != "i") return null
        return segments[1].takeIf { it.isNotBlank() }
    }
}
