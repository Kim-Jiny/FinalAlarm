import Foundation

/// 네트워크 실패로 서버에 보고 못 한 알람 이벤트를 보관. 앱 재진입 시 drain.
/// Android의 PendingEventStore + EventReconcileWorker 패턴을 단순화.
@Observable
final class PendingEventStore {
    static let shared = PendingEventStore()

    struct Pending: Codable, Identifiable {
        var id: String { localId }
        let localId: String
        let definitionId: String
        let triggeredAt: String
        var dismissed: Bool
        var dismissedAt: String?
        var volumePctAtTrigger: Int?
        var dndAtTrigger: Bool?
        var volumePctAtDismiss: Int?
        var dndAtDismiss: Bool?
    }

    private let key = "fa.pendingEvents"
    private let queue = DispatchQueue(label: "fa.pendingStore", attributes: .concurrent)

    func list() -> [Pending] {
        queue.sync {
            guard let data = UserDefaults.standard.data(forKey: key),
                  let arr = try? JSONDecoder().decode([Pending].self, from: data) else {
                return []
            }
            return arr
        }
    }

    func add(_ p: Pending) {
        queue.async(flags: .barrier) {
            var cur = self.listUnsafe().filter { $0.localId != p.localId }
            cur.append(p)
            self.writeUnsafe(cur)
        }
    }

    func markDismissed(localId: String, dismissedAt: String, volumePct: Int?, dnd: Bool?) {
        queue.async(flags: .barrier) {
            let cur = self.listUnsafe().map { p -> Pending in
                guard p.localId == localId else { return p }
                var copy = p
                copy.dismissed = true
                copy.dismissedAt = dismissedAt
                copy.volumePctAtDismiss = volumePct
                copy.dndAtDismiss = dnd
                return copy
            }
            self.writeUnsafe(cur)
        }
    }

    func remove(_ localId: String) {
        queue.async(flags: .barrier) {
            let cur = self.listUnsafe().filter { $0.localId != localId }
            self.writeUnsafe(cur)
        }
    }

    private func listUnsafe() -> [Pending] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let arr = try? JSONDecoder().decode([Pending].self, from: data) else {
            return []
        }
        return arr
    }

    private func writeUnsafe(_ items: [Pending]) {
        if let data = try? JSONEncoder().encode(items) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }
}

/// 큐 비우기. 앱 active 시점에 호출.
enum EventReconciler {
    static func drain() async {
        let store = PendingEventStore.shared
        let pendings = store.list()
        guard !pendings.isEmpty, TokenStore.shared.isLoggedIn else { return }
        for p in pendings {
            let req = CreateAlarmEventRequest(
                definitionId: p.definitionId,
                triggeredAt: p.triggeredAt,
                initialState: p.dismissed ? "DISMISSED" : "RINGING",
                dismissedAt: p.dismissedAt,
                volumePctAtTrigger: p.volumePctAtTrigger,
                dndAtTrigger: p.dndAtTrigger,
                volumePctAtDismiss: p.volumePctAtDismiss,
                dndAtDismiss: p.dndAtDismiss
            )
            do {
                _ = try await EventsRepository.shared.create(req)
                store.remove(p.localId)
            } catch {
                // 다음 진입 시 재시도. 401이면 토큰 만료 — 클리어는 APIClient가 처리.
                continue
            }
        }
    }
}
