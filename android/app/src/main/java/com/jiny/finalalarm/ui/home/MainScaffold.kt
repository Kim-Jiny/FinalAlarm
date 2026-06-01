package com.jiny.finalalarm.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jiny.finalalarm.ui.alarms.AlarmListTab
import com.jiny.finalalarm.ui.settings.SettingsTab
import com.jiny.finalalarm.ui.teams.TeamsTab

@Composable
fun MainScaffold(nav: NavController) {
    var tab by remember { mutableStateOf(BottomTab.HOME) }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            Column {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp,
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                ) {
                    BottomTab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = {},
                            label = {
                                Text(
                                    t.label,
                                    style = if (tab == t) MaterialTheme.typography.labelLarge
                                    else MaterialTheme.typography.labelMedium,
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
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

enum class BottomTab(val label: String) {
    HOME("홈"),
    ALARMS("알람"),
    TEAMS("팀"),
    SETTINGS("설정"),
}
