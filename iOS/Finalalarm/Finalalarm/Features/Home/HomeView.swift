import SwiftUI

@Observable
final class HomeVm {
    var alarms: [AlarmDto] = []
    var active: [AlarmEventDto] = []
    private let repo = AlarmsRepository.shared
    private let events = EventsRepository.shared

    func refresh() async {
        async let a = (try? await repo.list(active: true)) ?? []
        async let e = (try? await events.active()) ?? []
        let (al, ev) = await (a, e)
        alarms = al
        active = ev
    }
}

struct HomeView: View {
    @State private var vm = HomeVm()

    private var greeting: String {
        let h = Calendar.current.component(.hour, from: Date())
        switch h {
        case 5...10: return "좋은 아침"
        case 11...16: return "안녕하세요"
        case 17...20: return "수고했어요"
        default: return "오늘도 고생했어요"
        }
    }

    private var subtitle: String {
        if !vm.active.isEmpty { return "지금 알람이 울리고 있어요" }
        if vm.alarms.isEmpty { return "아직 알람이 없네요. 추가해볼까요?" }
        return "오늘은 \(vm.alarms.count)개의 알람이 있어요"
    }

    var body: some View {
        WarmBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HelloHeader(title: greeting, subtitle: subtitle)

                    if !vm.active.isEmpty {
                        FASection(title: "지금 울리는 중") {
                            ForEach(vm.active) { e in
                                ListRow(
                                    headline: e.state.label,
                                    supporting: e.senderUserId != nil ? "팀원이 깨우는 중" : nil
                                )
                            }
                        }
                    }

                    if !vm.alarms.isEmpty {
                        FASection(title: "오늘의 알람") {
                            ForEach(vm.alarms) { a in
                                ListRow(
                                    headline: a.label,
                                    supporting: "\(a.timeOfDay ?? a.oneShotAt ?? "—") · \(a.kind.label)"
                                )
                            }
                        }
                    } else if vm.active.isEmpty {
                        EmptyState(text: "첫 알람을 추가해서\n친구들과 함께 일어나봐요")
                    }

                    Spacer().frame(height: FASpacing.xxl)
                }
                .padding(.horizontal, FASpacing.lg)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .task { await vm.refresh() }
        .refreshable { await vm.refresh() }
    }
}
