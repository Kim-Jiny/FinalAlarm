import SwiftUI
import UserNotifications

@main
struct FinalalarmApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    @State private var tokens = TokenStore.shared
    @State private var ringingAlarmId: String? = nil
    @State private var ringingKind: AlarmKind = .PERSONAL
    @State private var ringingAlarm: AlarmDto? = nil

    init() {
        AlarmScheduler.shared.registerCategories()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(tokens)
                .tint(FA.primary)
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active {
                        Task { await EventReconciler.drain() }
                    }
                }
                .onReceive(NotificationCenter.default.publisher(for: .faAlarmTriggered)) { note in
                    if let info = note.userInfo,
                       let id = info["alarmId"] as? String {
                        ringingAlarmId = id
                        if let kindStr = info["kind"] as? String,
                           let k = AlarmKind(rawValue: kindStr) {
                            ringingKind = k
                        }
                        Task {
                            ringingAlarm = try? await AlarmsRepository.shared.get(id)
                        }
                    }
                }
                .fullScreenCover(item: Binding(
                    get: { ringingAlarmId.map { RingingTrigger(id: $0) } },
                    set: { ringingAlarmId = $0?.id }
                )) { _ in
                    RingingView(
                        alarm: ringingAlarm,
                        isTeamApproval: ringingKind == .TEAM_APPROVAL,
                        onDismiss: {
                            ringingAlarmId = nil
                            ringingAlarm = nil
                        }
                    )
                }
        }
    }
}

private struct RingingTrigger: Identifiable {
    let id: String
}
