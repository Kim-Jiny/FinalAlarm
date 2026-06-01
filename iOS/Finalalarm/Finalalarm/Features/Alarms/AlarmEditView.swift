import SwiftUI

@Observable
final class AlarmEditVm {
    var label: String = "기상"
    var kind: AlarmKind = .PERSONAL
    var teamId: String? = nil
    var teams: [TeamSummary] = []
    var time: Date = Calendar.current.date(bySettingHour: 7, minute: 0, second: 0, of: Date()) ?? Date()
    var daysOfWeek: Int = 0b0011111  // 월-금
    var missions: [MissionDto] = []
    var missionId: String? = nil
    var snoozeEnabled: Bool = true
    var snoozeMinutes: Int = 5
    var snoozeMaxCount: Int = 3
    var volume: Double = 80
    var saving = false
    var saved = false
    var error: String? = nil

    private let alarmsRepo = AlarmsRepository.shared
    private let teamsRepo = TeamsRepository.shared
    private let missionsRepo = MissionsRepository.shared
    private var editingId: String? = nil

    func load(_ alarm: AlarmDto?) async {
        async let tl = (try? await teamsRepo.list()) ?? []
        async let ml = (try? await missionsRepo.list()) ?? []
        let (t, m) = await (tl, ml)
        teams = t
        missions = m

        if let a = alarm {
            editingId = a.id
            label = a.label
            kind = a.kind
            teamId = a.teamId
            if let timeStr = a.timeOfDay, let parsed = parseTime(timeStr) {
                time = parsed
            }
            daysOfWeek = a.daysOfWeek ?? daysOfWeek
            missionId = a.missionId
            snoozeEnabled = a.snoozeEnabled
            snoozeMinutes = a.snoozeMinutes
            snoozeMaxCount = a.snoozeMaxCount
            volume = Double(a.volume)
        } else {
            // 새 알람은 기본 미션 선택
            missionId = missions.first(where: { $0.isDefault })?.id ?? missions.first?.id
        }
    }

    func toggleDay(_ bit: Int) {
        daysOfWeek ^= (1 << bit)
    }

    var timeString: String {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f.string(from: time)
    }

    private func parseTime(_ s: String) -> Date? {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f.date(from: s)
    }

    func save() async {
        guard let mid = missionId else {
            error = "미션을 먼저 만들어주세요"
            return
        }
        if kind == .TEAM_APPROVAL && teamId == nil {
            error = "팀을 선택해주세요"
            return
        }
        saving = true
        error = nil
        do {
            if let id = editingId {
                _ = try await alarmsRepo.update(id, [
                    "label": .string(label),
                    "timeOfDay": .string(timeString),
                    "daysOfWeek": .int(daysOfWeek),
                    "volume": .int(Int(volume)),
                    "snoozeEnabled": .bool(snoozeEnabled),
                    "snoozeMinutes": .int(snoozeMinutes),
                    "snoozeMaxCount": .int(snoozeMaxCount),
                    "missionId": .string(mid),
                ])
            } else {
                _ = try await alarmsRepo.create(CreateAlarmRequest(
                    kind: kind,
                    teamId: kind == .PERSONAL ? nil : teamId,
                    label: label,
                    timezone: TimeZone.current.identifier,
                    scheduleType: .RECURRING,
                    timeOfDay: timeString,
                    daysOfWeek: daysOfWeek,
                    oneShotAt: nil,
                    soundUri: "system:default",
                    volume: Int(volume),
                    volumeRampSeconds: 30,
                    vibrationEnabled: true,
                    vibrationPattern: .PULSE,
                    snoozeEnabled: snoozeEnabled,
                    snoozeMinutes: snoozeMinutes,
                    snoozeMaxCount: snoozeMaxCount,
                    missionId: mid
                ))
            }
            saved = true
        } catch {
            self.error = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
        saving = false
    }
}

struct AlarmEditView: View {
    @Environment(\.dismiss) private var dismiss
    let alarm: AlarmDto?
    @State private var vm = AlarmEditVm()

    private let dayLabels = ["월", "화", "수", "목", "금", "토", "일"]

    var body: some View {
        WarmBackground {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Button("취소") { dismiss() }
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(FA.primary)
                    Spacer()
                    Text(alarm == nil ? "새 알람" : "수정")
                        .font(.system(size: 17, weight: .semibold))
                    Spacer()
                    Button("저장") {
                        Task {
                            await vm.save()
                            if vm.saved { dismiss() }
                        }
                    }
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(FA.primary)
                    .disabled(vm.saving)
                }
                .padding(.horizontal, FASpacing.lg)
                .padding(.vertical, FASpacing.md)

                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        FASection(title: "이름") {
                            FaTextField(placeholder: "예: 출근", text: $vm.label)
                        }

                        FASection(title: "알람 종류") {
                            HStack(spacing: FASpacing.sm) {
                                ForEach(AlarmKind.allCases, id: \.self) { k in
                                    ChoicePill(
                                        text: k.label,
                                        selected: vm.kind == k,
                                        action: { vm.kind = k }
                                    )
                                }
                            }
                        }

                        if vm.kind == .TEAM_APPROVAL {
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
                        }

                        FASection(title: "시각") {
                            DatePicker("", selection: $vm.time, displayedComponents: .hourAndMinute)
                                .datePickerStyle(.wheel)
                                .labelsHidden()
                                .frame(maxWidth: .infinity)
                        }

                        FASection(title: "반복 요일") {
                            HStack(spacing: FASpacing.xs) {
                                ForEach(0..<7, id: \.self) { i in
                                    DayPill(
                                        day: dayLabels[i],
                                        selected: (vm.daysOfWeek >> i) & 1 == 1,
                                        action: { vm.toggleDay(i) }
                                    )
                                }
                            }
                        }

                        FASection(title: "미션") {
                            if vm.missions.isEmpty {
                                Text("미션을 먼저 만들어주세요")
                                    .font(.system(size: 14))
                                    .foregroundStyle(FA.destructive)
                            } else {
                                ForEach(vm.missions) { m in
                                    ListRow(
                                        headline: m.name,
                                        supporting: m.type.label,
                                        onTap: { vm.missionId = m.id },
                                        trailing: {
                                            if vm.missionId == m.id {
                                                Image(systemName: "checkmark.circle.fill")
                                                    .foregroundStyle(FA.primary)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        FASection(title: "스누즈") {
                            ListRow(
                                headline: "스누즈 사용",
                                supporting: vm.snoozeEnabled ? "\(vm.snoozeMinutes)분씩, 최대 \(vm.snoozeMaxCount)회" : "사용 안 함",
                                trailing: {
                                    Toggle("", isOn: $vm.snoozeEnabled)
                                        .labelsHidden()
                                        .tint(FA.primary)
                                }
                            )
                        }

                        FASection(title: "볼륨") {
                            Text("\(Int(vm.volume))")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(FA.labelSecondary)
                            Slider(value: $vm.volume, in: 0...100)
                                .tint(FA.primary)
                        }

                        if let error = vm.error { ErrorText(text: error) }
                        Spacer().frame(height: FASpacing.xxl)
                    }
                    .padding(.horizontal, FASpacing.lg)
                }
            }
        }
        .task { await vm.load(alarm) }
    }
}
