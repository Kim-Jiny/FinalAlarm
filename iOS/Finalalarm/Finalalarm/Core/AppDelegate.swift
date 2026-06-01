import UIKit
import UserNotifications

/// 노티 → RingingView 트리거. APNs/FCM 토큰 등록.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    /// 알람 노티 탭/포그라운드 도달 시 RingingView를 띄울 트리거.
    static let pendingRingingAlarmId = "FA.pendingRingingAlarmId"

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()
        PushService.shared.configure()
        return true
    }

    // MARK: - APNs

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        PushService.shared.onAPNsToken(deviceToken)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("APNs register failed: \(error)")
    }

    // 포그라운드 수신 — 알람이면 노티 그대로 + RingingView 트리거
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        triggerRingingIfAlarm(notification.request.content.userInfo)
        completionHandler([.banner, .sound, .badge])
    }

    // 탭 처리 — 알람 노티면 RingingView 띄우기
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        triggerRingingIfAlarm(response.notification.request.content.userInfo)
        completionHandler()
    }

    private func triggerRingingIfAlarm(_ userInfo: [AnyHashable: Any]) {
        if let alarmId = userInfo["alarmId"] as? String {
            NotificationCenter.default.post(
                name: .faAlarmTriggered,
                object: nil,
                userInfo: ["alarmId": alarmId, "kind": userInfo["kind"] as? String ?? "PERSONAL"]
            )
        }
    }
}

extension Notification.Name {
    static let faAlarmTriggered = Notification.Name("FA.alarmTriggered")
}
