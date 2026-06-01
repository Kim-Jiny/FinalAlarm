import SwiftUI

@Observable
final class PushAlarmVm {
    var members: [TeamMember] = []
    var selectedUserId: String? = nil
    var label: String = "일어나!"
    var sending = false
    var sent = false
    var error: String? = nil
    let teamId: String
    private let teamsRepo = TeamsRepository.shared
    private let pushRepo = PushRepository.shared
    init(teamId: String) { self.teamId = teamId }

    func load() async {
        let team = try? await teamsRepo.get(teamId)
        members = team?.members ?? []
    }

    func send() async {
        guard let target = selectedUserId else { return }
        sending = true; error = nil
        do {
            try await pushRepo.send(PushAlarmRequest(toUserId: target, teamId: teamId, label: label))
            sent = true
        } catch {
            self.error = (error as? LocalizedError)?.errorDescription
        }
        sending = false
    }
}

struct PushAlarmView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var vm: PushAlarmVm
    init(teamId: String) { _vm = State(initialValue: PushAlarmVm(teamId: teamId)) }

    var body: some View {
        WarmBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HelloHeader(title: "친구를 깨워요", subtitle: "이 시간대 안에서만 알람이 울려요")

                    FASection(title: "메시지") {
                        FaTextField(placeholder: "예: 일어나!", text: $vm.label)
                    }

                    FASection(title: "누구를 깨울까요?") {
                        if vm.members.isEmpty {
                            Text("팀원이 없어요")
                                .font(.system(size: 14))
                                .foregroundStyle(FA.labelSecondary)
                        } else {
                            ForEach(vm.members) { m in
                                ListRow(
                                    headline: m.user.displayName,
                                    supporting: m.role.label,
                                    onTap: { vm.selectedUserId = m.user.id },
                                    trailing: {
                                        if vm.selectedUserId == m.user.id {
                                            Image(systemName: "checkmark.circle.fill")
                                                .foregroundStyle(FA.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if let error = vm.error { ErrorText(text: error) }
                    Spacer().frame(height: FASpacing.xl)
                    PrimaryButton(
                        text: vm.sending ? "발사 중…" : "알람 발사",
                        enabled: !vm.sending && vm.selectedUserId != nil,
                        action: {
                            Task {
                                await vm.send()
                                if vm.sent { dismiss() }
                            }
                        }
                    )
                    Spacer().frame(height: FASpacing.xxl)
                }
                .padding(.horizontal, FASpacing.lg)
            }
        }
        .navigationTitle("팀원 깨우기")
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.load() }
    }
}
