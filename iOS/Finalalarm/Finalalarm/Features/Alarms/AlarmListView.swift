import SwiftUI

@Observable
final class AlarmListVm {
    var items: [AlarmDto] = []
    var error: String? = nil
    private let repo = AlarmsRepository.shared

    func refresh() async {
        do {
            items = try await repo.list()
            await AlarmScheduler.shared.reschedule(items)
        } catch {
            self.error = (error as? LocalizedError)?.errorDescription
        }
    }

    func toggle(_ alarm: AlarmDto) async {
        do {
            _ = try await repo.update(alarm.id, ["active": .bool(!alarm.active)])
        } catch {
            self.error = (error as? LocalizedError)?.errorDescription
        }
        await refresh()
    }

    func delete(_ id: String) async {
        try? await repo.delete(id)
        await refresh()
    }
}

struct AlarmListView: View {
    @State private var vm = AlarmListVm()
    @State private var editing: AlarmDto? = nil
    @State private var creating = false

    var body: some View {
        WarmBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HStack(alignment: .center) {
                        Text("알람")
                            .font(.system(size: 32, weight: .heavy))
                            .foregroundStyle(FA.label)
                            .kerning(-0.6)
                        Spacer()
                        SecondaryButton(text: "+ 추가") {
                            creating = true
                        }
                    }
                    .padding(.top, FASpacing.xl)

                    if let error = vm.error { ErrorText(text: error) }

                    if vm.items.isEmpty {
                        EmptyState(text: "조용하네요.\n첫 알람을 만들어봐요")
                    } else {
                        ForEach(vm.items) { alarm in
                            ListRow(
                                headline: alarm.label,
                                supporting: "\(alarm.timeOfDay ?? alarm.oneShotAt ?? "?") · \(alarm.kind.label)",
                                onTap: { editing = alarm },
                                trailing: {
                                    Toggle("", isOn: Binding(
                                        get: { alarm.active },
                                        set: { _ in Task { await vm.toggle(alarm) } }
                                    ))
                                    .labelsHidden()
                                    .tint(FA.primary)
                                }
                            )
                        }
                    }
                    Spacer().frame(height: FASpacing.xxl)
                }
                .padding(.horizontal, FASpacing.lg)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .task { await vm.refresh() }
        .refreshable { await vm.refresh() }
        .sheet(isPresented: $creating, onDismiss: { Task { await vm.refresh() } }) {
            AlarmEditView(alarm: nil)
        }
        .sheet(item: $editing, onDismiss: { Task { await vm.refresh() } }) { alarm in
            AlarmEditView(alarm: alarm)
        }
    }
}
