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
                            ListRow(
                                headline: formatDate(e.triggeredAt),
                                supporting: buildSupporting(e)
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

    private func formatDate(_ s: String) -> String {
        let trimmed = s.split(separator: ".").first.map(String.init) ?? s
        return trimmed.replacingOccurrences(of: "T", with: " ")
    }

    private func buildSupporting(_ e: AlarmEventDto) -> String {
        let source = e.senderUserId != nil ? "팀원이 깨움" : "본인 알람"
        var lines: [String] = ["\(e.state.label) · \(source)"]
        if let d = e.dismissedAt {
            lines.append("해제 \(formatDate(d))")
        }
        var deviceParts: [String] = []
        if let v = e.volumePctAtTrigger {
            deviceParts.append("울릴 때 볼륨 \(v)%")
        }
        if e.dndAtTrigger == true {
            deviceParts.append("방해금지 ON")
        }
        if let v = e.volumePctAtDismiss {
            deviceParts.append("끌 때 볼륨 \(v)%")
        }
        if !deviceParts.isEmpty {
            lines.append(deviceParts.joined(separator: " · "))
        }
        return lines.joined(separator: "\n")
    }
}
