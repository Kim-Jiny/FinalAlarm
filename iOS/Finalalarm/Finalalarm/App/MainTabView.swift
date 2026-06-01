import SwiftUI

enum AppTab: Hashable { case home, alarms, teams, settings }

struct MainTabView: View {
    @State private var selectedTab: AppTab = .home
    @State private var teamsPath = NavigationPath()
    let initialInviteCode: String?

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                HomeView()
            }
            .tabItem { Label("홈", systemImage: "house") }
            .tag(AppTab.home)

            NavigationStack {
                AlarmListView()
            }
            .tabItem { Label("알람", systemImage: "alarm") }
            .tag(AppTab.alarms)

            NavigationStack(path: $teamsPath) {
                TeamsListView()
                    .navigationDestination(for: TeamsRoute.self) { route in
                        switch route {
                        case .create: TeamCreateView()
                        case .detail(let id): TeamDetailView(teamId: id)
                        case .invite(let id): TeamInviteView(teamId: id)
                        case .join(let code): JoinTeamView(initialCode: code)
                        case .pushAlarm(let teamId): PushAlarmView(teamId: teamId)
                        case .inbox(let teamId): InboxListView(teamId: teamId)
                        case .unlockDetail(let id): UnlockRequestDetailView(id: id)
                        }
                    }
            }
            .tabItem { Label("팀", systemImage: "person.3") }
            .tag(AppTab.teams)

            NavigationStack {
                SettingsView()
            }
            .tabItem { Label("설정", systemImage: "gearshape") }
            .tag(AppTab.settings)
        }
        .tint(FA.primary)
        .onAppear {
            if let code = initialInviteCode, !code.isEmpty {
                selectedTab = .teams
                teamsPath.append(TeamsRoute.join(code))
            }
        }
    }
}

enum TeamsRoute: Hashable {
    case create
    case detail(String)
    case invite(String)
    case join(String)
    case pushAlarm(String)
    case inbox(String)
    case unlockDetail(String)
}
