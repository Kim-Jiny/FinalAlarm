import SwiftUI

@Observable
final class HistoryVm {
    var events: [AlarmEventDto] = []
    var loading = false
    private let repo = EventsRepository.shared
    func refresh() async {
        loading = true
        events = (try? await repo.history(limit: 100)) ?? []
        loading = false
    }
}

struct HistoryView: View {
    @State private var vm = HistoryVm()

    var body: some View {
        WarmBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if vm.events.isEmpty && !vm.loading {
                        EmptyState(text: "기록이 없습니다")
                    } else {
                        ForEach(vm.events) { e in
                            let when_ = e.triggeredAt.split(separator: ".").first
                                .map(String.init)?.replacingOccurrences(of: "T", with: " ") ?? e.triggeredAt
                            let source = e.senderUserId != nil ? "팀원이 깨움" : "본인 알람"
                            ListRow(
                                headline: when_,
                                supporting: "\(e.state.label) · \(source)"
                            )
                        }
                    }
                    Spacer().frame(height: FASpacing.xxl)
                }
                .padding(.horizontal, FASpacing.lg)
            }
        }
        .navigationTitle("히스토리")
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.refresh() }
        .refreshable { await vm.refresh() }
    }
}
