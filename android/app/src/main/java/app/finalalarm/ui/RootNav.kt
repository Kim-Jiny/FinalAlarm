package app.finalalarm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.finalalarm.data.AuthRepository
import app.finalalarm.ui.auth.LoginScreen
import app.finalalarm.ui.auth.OnboardingScreen
import app.finalalarm.ui.auth.SignupScreen
import app.finalalarm.ui.home.MainScaffold
import app.finalalarm.ui.teams.JoinTeamScreen
import app.finalalarm.ui.alarms.AlarmEditScreen
import app.finalalarm.ui.missions.MissionEditScreen
import app.finalalarm.ui.missions.MissionListScreen
import app.finalalarm.ui.teams.TeamCreateScreen
import app.finalalarm.ui.teams.TeamDetailScreen
import app.finalalarm.ui.teams.TeamInviteScreen
import app.finalalarm.ui.windows.WindowEditScreen
import app.finalalarm.ui.windows.WindowListScreen
import app.finalalarm.ui.pushalarm.PushAlarmScreen
import app.finalalarm.ui.history.HistoryScreen
import app.finalalarm.ui.inbox.InboxListScreen
import app.finalalarm.ui.inbox.UnlockRequestDetailScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val ALARM_EDIT = "alarm/edit"
    const val ALARM_EDIT_WITH_ID = "alarm/edit/{id}"
    const val MISSION_LIST = "mission/list"
    const val MISSION_EDIT = "mission/edit"
    const val MISSION_EDIT_WITH_ID = "mission/edit/{id}"
    const val TEAM_CREATE = "team/create"
    const val TEAM_DETAIL = "team/{id}"
    const val TEAM_INVITE = "team/{id}/invite"
    const val JOIN_TEAM = "team/join?code={code}"
    fun joinTeamRoute(code: String? = null): String =
        if (code != null) "team/join?code=$code" else "team/join?code="
    const val WINDOW_LIST = "window/list"
    const val WINDOW_EDIT = "window/edit"
    const val WINDOW_EDIT_WITH_ID = "window/edit/{id}"
    const val PUSH_ALARM = "push-alarm/{teamId}"
    const val INBOX_LIST = "inbox/{teamId}"
    const val UNLOCK_DETAIL = "inbox/request/{id}"
    const val HISTORY = "history"
}

@HiltViewModel
class SessionVm @Inject constructor(authRepo: AuthRepository) : ViewModel() {
    val loggedIn = authRepo.loggedInFlow.stateIn(
        scope = androidx.lifecycle.viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )
}

@Composable
fun RootNav(initialInviteCode: String? = null) {
    val nav = rememberNavController()
    val sessionVm: SessionVm = hiltViewModel()
    val loggedIn by sessionVm.loggedIn.collectAsState()

    val start = when (loggedIn) {
        null -> Routes.LOGIN
        true -> Routes.MAIN
        false -> Routes.LOGIN
    }

    // 딥링크로 초대 코드 도착 → 로그인 상태면 JoinTeam으로
    androidx.compose.runtime.LaunchedEffect(loggedIn, initialInviteCode) {
        if (loggedIn == true && initialInviteCode != null) {
            nav.navigate(Routes.joinTeamRoute(initialInviteCode))
        }
    }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.LOGIN) { LoginScreen(nav) }
        composable(Routes.SIGNUP) { SignupScreen(nav) }
        composable(Routes.ONBOARDING) { OnboardingScreen(nav) }
        composable(Routes.MAIN) { MainScaffold(nav) }

        composable(Routes.ALARM_EDIT) { AlarmEditScreen(nav, alarmId = null) }
        composable(
            Routes.ALARM_EDIT_WITH_ID,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry -> AlarmEditScreen(nav, entry.arguments?.getString("id")) }

        composable(Routes.MISSION_LIST) { MissionListScreen(nav) }
        composable(Routes.MISSION_EDIT) { MissionEditScreen(nav, missionId = null) }
        composable(
            Routes.MISSION_EDIT_WITH_ID,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry -> MissionEditScreen(nav, entry.arguments?.getString("id")) }

        composable(Routes.TEAM_CREATE) { TeamCreateScreen(nav) }
        composable(
            Routes.TEAM_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry -> TeamDetailScreen(nav, entry.arguments!!.getString("id")!!) }

        composable(
            Routes.TEAM_INVITE,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry -> TeamInviteScreen(nav, entry.arguments!!.getString("id")!!) }

        composable(
            Routes.JOIN_TEAM,
            arguments = listOf(navArgument("code") {
                type = NavType.StringType
                defaultValue = ""
                nullable = false
            }),
        ) { entry ->
            JoinTeamScreen(nav, initialCode = entry.arguments?.getString("code").orEmpty())
        }

        composable(Routes.WINDOW_LIST) { WindowListScreen(nav) }
        composable(Routes.WINDOW_EDIT) { WindowEditScreen(nav, windowId = null) }
        composable(
            Routes.WINDOW_EDIT_WITH_ID,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry -> WindowEditScreen(nav, entry.arguments?.getString("id")) }

        composable(
            Routes.PUSH_ALARM,
            arguments = listOf(navArgument("teamId") { type = NavType.StringType }),
        ) { entry -> PushAlarmScreen(nav, entry.arguments!!.getString("teamId")!!) }

        composable(
            Routes.INBOX_LIST,
            arguments = listOf(navArgument("teamId") { type = NavType.StringType }),
        ) { entry -> InboxListScreen(nav, entry.arguments!!.getString("teamId")!!) }

        composable(
            Routes.UNLOCK_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry -> UnlockRequestDetailScreen(nav, entry.arguments!!.getString("id")!!) }

        composable(Routes.HISTORY) { HistoryScreen(nav) }
    }
}
