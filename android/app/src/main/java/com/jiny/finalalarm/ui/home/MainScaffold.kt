package com.jiny.finalalarm.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.jiny.finalalarm.ui.alarms.AlarmListTab
import com.jiny.finalalarm.ui.settings.SettingsTab
import com.jiny.finalalarm.ui.teams.TeamsTab

@Composable
fun MainScaffold(nav: NavController) {
    var tab by remember { mutableStateOf(BottomTab.HOME) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { inner ->
        when (tab) {
            BottomTab.HOME -> HomeTab(nav, Modifier.padding(inner))
            BottomTab.ALARMS -> AlarmListTab(nav, Modifier.padding(inner))
            BottomTab.TEAMS -> TeamsTab(nav, Modifier.padding(inner))
            BottomTab.SETTINGS -> SettingsTab(nav, Modifier.padding(inner))
        }
    }
}

enum class BottomTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("홈", Icons.Outlined.Home),
    ALARMS("알람", Icons.Outlined.Alarm),
    TEAMS("팀", Icons.Outlined.Group),
    SETTINGS("설정", Icons.Outlined.Settings),
}
