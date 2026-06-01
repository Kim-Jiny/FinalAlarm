package app.finalalarm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.finalalarm.core.sync.EventReconcileWorker
import app.finalalarm.ui.RootNav
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        EventReconcileWorker.enqueue(applicationContext)

        val inviteCode = extractInviteCode(intent)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootNav(initialInviteCode = inviteCode)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (extractInviteCode(intent) != null) {
            // 앱이 떠 있는 상태에서 딥링크 탭 — RootNav 초기 라우팅을 다시 계산하려면 recreate
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
