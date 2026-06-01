import Foundation
import UserNotifications

/// 로컬 알림 스케줄링. 서버 알람 정의를 받아 UNNotification으로 변환.
/// iOS는 64개 noti 제한 — 가까운 알람들을 우선 등록.
final class AlarmScheduler {
    static let shared = AlarmScheduler()
    private let center = UNUserNotificationCenter.current()
    private let category = "FA_ALARM"

    func requestAuthorization() async -> Bool {
        let granted = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
        return granted ?? false
    }

    func registerCategories() {
        let dismiss = UNNotificationAction(
            identifier: "FA_DISMISS",
            title: "확인",
            options: [.foreground]
        )
        let cat = UNNotificationCategory(
            identifier: category,
            actions: [dismiss],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        center.setNotificationCategories([cat])
    }

    /// 서버에서 받은 알람 목록을 모두 로컬 노티로 재등록.
    func reschedule(_ alarms: [AlarmDto]) async {
        center.removeAllPendingNotificationRequests()
        for alarm in alarms where alarm.active {
            await schedule(alarm)
        }
    }

    private func schedule(_ alarm: AlarmDto) async {
        guard let time = alarm.timeOfDay,
              let parts = parseHourMinute(time) else { return }

        let content = UNMutableNotificationContent()
        content.title = alarm.label
        content.body = alarm.kind == .TEAM_APPROVAL ? "팀과 함께 일어나요" : "일어날 시간이에요"
        content.sound = .defaultCritical
        content.categoryIdentifier = category
        content.userInfo = [
            "alarmId": alarm.id,
            "kind": alarm.kind.rawValue,
        ]

        if alarm.scheduleType == .RECURRING, let days = alarm.daysOfWeek {
            for weekday in 0..<7 where ((days >> weekday) & 1) == 1 {
                var date = DateComponents()
                date.hour = parts.hour
                date.minute = parts.minute
                // 우리: 0=월…6=일. UN: 1=일, 2=월…7=토
                let unWeekday = ((weekday + 1) % 7) + 1
                date.weekday = unWeekday
                let trigger = UNCalendarNotificationTrigger(dateMatching: date, repeats: true)
                let req = UNNotificationRequest(
                    identifier: "alarm.\(alarm.id).\(weekday)",
                    content: content,
                    trigger: trigger
                )
                try? await center.add(req)
            }
        } else if alarm.scheduleType == .ONE_SHOT, let oneShotISO = alarm.oneShotAt,
                  let date = ISO8601DateFormatter().date(from: oneShotISO), date > Date() {
            let comps = Calendar.current.dateComponents(
                [.year, .month, .day, .hour, .minute],
                from: date
            )
            let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
            let req = UNNotificationRequest(
                identifier: "alarm.\(alarm.id).once",
                content: content,
                trigger: trigger
            )
            try? await center.add(req)
        }
    }

    func cancel(alarmId: String) async {
        let pending = await center.pendingNotificationRequests()
        let ids = pending
            .filter { $0.identifier.hasPrefix("alarm.\(alarmId).") }
            .map { $0.identifier }
        center.removePendingNotificationRequests(withIdentifiers: ids)
    }

    private func parseHourMinute(_ s: String) -> (hour: Int, minute: Int)? {
        let parts = s.split(separator: ":")
        guard parts.count == 2, let h = Int(parts[0]), let m = Int(parts[1]) else { return nil }
        return (h, m)
    }
}
