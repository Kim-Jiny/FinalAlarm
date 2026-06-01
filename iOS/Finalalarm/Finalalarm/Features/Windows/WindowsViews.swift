import SwiftUI

@Observable
final class WindowListVm {
    var items: [WindowDto] = []
    private let repo = WindowsRepository.shared
    func refresh() async { items = (try? await repo.list()) ?? [] }
    func delete(_ id: String) async {
        try? await repo.delete(id)
        await refresh()
    }
}

struct WindowListView: View {
    @State private var vm = WindowListVm()
    @State private var editing: WindowDto? = nil
    @State private var creating = false

    var body: some View {
        WarmBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if vm.items.isEmpty {
                        EmptyState(text: "시간대를 추가해서\n팀원이 깨울 수 있게 해보세요")
                    } else {
                        ForEach(vm.items) { w in
                            let days = ["월","화","수","목","금","토","일"]
                                .enumerated()
                                .filter { i, _ in (w.daysOfWeek >> i) & 1 == 1 }
                                .map { $0.1 }
                                .joined(separator: "·")
                            ListRow(
                                headline: "\(w.startTime) – \(w.endTime)",
                                supporting: days,
                                onTap: { editing = w },
                                trailing: {
                                    Button("삭제") { Task { await vm.delete(w.id) } }
                                        .foregroundStyle(FA.destructive)
                                        .font(.system(size: 14))
                                }
                            )
                        }
                    }
                    Spacer().frame(height: FASpacing.xxl)
                }
                .padding(.horizontal, FASpacing.lg)
            }
        }
        .navigationTitle("알람 시간대")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("추가") { creating = true }
            }
        }
        .task { await vm.refresh() }
        .sheet(isPresented: $creating, onDismiss: { Task { await vm.refresh() } }) {
            WindowEditView(window: nil)
        }
        .sheet(item: $editing, onDismiss: { Task { await vm.refresh() } }) { w in
            WindowEditView(window: w)
        }
    }
}

@Observable
final class WindowEditVm {
    var teams: [TeamSummary] = []
    var teamId: String? = nil
    var start: Date = Calendar.current.date(bySettingHour: 6, minute: 0, second: 0, of: Date()) ?? Date()
    var end: Date = Calendar.current.date(bySettingHour: 9, minute: 0, second: 0, of: Date()) ?? Date()
    var days: Int = 0b1111111
    var saving = false
    var saved = false
    var error: String? = nil
    private let teamsRepo = TeamsRepository.shared
    private let repo = WindowsRepository.shared
    private var editingId: String? = nil

    func load(_ w: WindowDto?) async {
        teams = (try? await teamsRepo.list()) ?? []
        if let w {
            editingId = w.id
            teamId = w.teamId
            if let s = parseTime(w.startTime) { start = s }
            if let e = parseTime(w.endTime) { end = e }
            days = w.daysOfWeek
        }
    }

    private func parseTime(_ s: String) -> Date? {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f.date(from: s)
    }

    private func format(_ d: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f.string(from: d)
    }

    func toggleDay(_ bit: Int) { days ^= (1 << bit) }

    func save() async {
        guard let tid = teamId else { error = "팀을 선택해주세요"; return }
        saving = true; error = nil
        do {
            if let id = editingId {
                _ = try await repo.update(id, [
                    "startTime": .string(format(start)),
                    "endTime": .string(format(end)),
                    "daysOfWeek": .int(days),
                ])
            } else {
                _ = try await repo.create(CreateWindowRequest(
                    teamId: tid,
                    startTime: format(start),
                    endTime: format(end),
                    daysOfWeek: days,
                    timezone: TimeZone.current.identifier
                ))
            }
            saved = true
        } catch {
            self.error = (error as? LocalizedError)?.errorDescription
        }
        saving = false
    }
}

struct WindowEditView: View {
    @Environment(\.dismiss) private var dismiss
    let window: WindowDto?
    @State private var vm = WindowEditVm()
    private let labels = ["월","화","수","목","금","토","일"]

    var body: some View {
        WarmBackground {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Button("취소") { dismiss() }
                        .foregroundStyle(FA.primary)
                    Spacer()
                    Text(window == nil ? "시간대 만들기" : "수정")
                        .font(.system(size: 17, weight: .semibold))
                    Spacer()
                    Button("저장") {
                        Task {
                            await vm.save()
                            if vm.saved { dismiss() }
                        }
                    }
                    .foregroundStyle(FA.primary)
                    .disabled(vm.saving)
                }
                .padding(.horizontal, FASpacing.lg)
                .padding(.vertical, FASpacing.md)

                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        FASection(title: "팀") {
                            if vm.teams.isEmpty {
                                Text("먼저 팀을 만들어주세요")
                                    .font(.system(size: 14))
                                    .foregroundStyle(FA.labelSecondary)
                            } else {
                                ForEach(vm.teams) { t in
                                    ListRow(
                                        headline: t.name,
                                        onTap: { vm.teamId = t.id },
                                        trailing: {
                                            if vm.teamId == t.id {
                                                Image(systemName: "checkmark.circle.fill")
                                                    .foregroundStyle(FA.primary)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        FASection(title: "시작 시각") {
                            DatePicker("", selection: $vm.start, displayedComponents: .hourAndMinute)
                                .datePickerStyle(.wheel)
                                .labelsHidden()
                        }

                        FASection(title: "끝 시각") {
                            DatePicker("", selection: $vm.end, displayedComponents: .hourAndMinute)
                                .datePickerStyle(.wheel)
                                .labelsHidden()
                        }

                        FASection(title: "요일") {
                            HStack(spacing: FASpacing.xs) {
                                ForEach(0..<7, id: \.self) { i in
                                    DayPill(
                                        day: labels[i],
                                        selected: (vm.days >> i) & 1 == 1,
                                        action: { vm.toggleDay(i) }
                                    )
                                }
                            }
                        }

                        if let error = vm.error { ErrorText(text: error) }
                        Spacer().frame(height: FASpacing.xxl)
                    }
                    .padding(.horizontal, FASpacing.lg)
                }
            }
        }
        .task { await vm.load(window) }
    }
}
