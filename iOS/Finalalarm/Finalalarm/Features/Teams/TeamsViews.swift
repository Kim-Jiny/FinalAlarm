import SwiftUI

// MARK: - 팀 목록

@Observable
final class TeamsListVm {
    var teams: [TeamSummary] = []
    private let repo = TeamsRepository.shared
    func refresh() async {
        teams = (try? await repo.list()) ?? []
    }
}

struct TeamsListView: View {
    @State private var vm = TeamsListVm()

    var body: some View {
        WarmBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HStack(alignment: .center) {
                        Text("팀")
                            .font(.system(size: 32, weight: .heavy))
                            .foregroundStyle(FA.label)
                            .kerning(-0.6)
                        Spacer()
                        NavigationLink(value: TeamsRoute.create) {
                            Text("+ 만들기")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(FA.primary)
                                .padding(.horizontal, FASpacing.md)
                                .padding(.vertical, FASpacing.sm)
                        }
                    }
                    .padding(.top, FASpacing.xl)

                    ListRow(headline: "초대 코드로 가입", onTap: nil)
                        .overlay(
                            NavigationLink(value: TeamsRoute.join(""), label: { Color.clear })
                        )

                    if vm.teams.isEmpty {
                        EmptyState(text: "함께 일어날 친구가 없네요.\n팀을 만들어 초대해보세요")
                    } else {
                        ForEach(vm.teams) { t in
                            NavigationLink(value: TeamsRoute.detail(t.id)) {
                                ListRow(headline: t.name, supporting: t.role.label)
                            }
                            .buttonStyle(.plain)
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
    }
}

// MARK: - 팀 만들기

struct TeamCreateView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var saving = false
    @State private var error: String? = nil
    private let repo = TeamsRepository.shared

    var body: some View {
        WarmBackground {
            VStack(alignment: .leading, spacing: FASpacing.sm) {
                Spacer().frame(height: FASpacing.sm)
                HelloHeader(title: "새 팀 만들기", subtitle: "함께 일어날 친구들을 모아봐요")
                Spacer().frame(height: FASpacing.xl)
                FaTextField(placeholder: "팀 이름", text: $name)
                if let error { ErrorText(text: error) }
                Spacer()
                PrimaryButton(
                    text: saving ? "만드는 중…" : "만들기",
                    enabled: !saving && !name.isEmpty,
                    action: create
                )
                Spacer().frame(height: FASpacing.md)
            }
            .padding(.horizontal, FASpacing.lg)
        }
        .navigationTitle("팀 만들기")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func create() {
        Task {
            saving = true
            error = nil
            do {
                _ = try await repo.create(name: name)
                dismiss()
            } catch {
                self.error = (error as? LocalizedError)?.errorDescription
            }
            saving = false
        }
    }
}

// MARK: - 팀 상세

@Observable
final class TeamDetailVm {
    var team: TeamDetail? = nil
    var error: String? = nil
    var left: Bool = false
    private let repo = TeamsRepository.shared
    let teamId: String

    init(teamId: String) { self.teamId = teamId }

    func refresh() async {
        team = try? await repo.get(teamId)
    }

    /// 누군가 알람 울리는 중이면 3초 폴링, 아니면 10초 폴링.
    func livePoll() async {
        while !Task.isCancelled {
            let anyRinging = team?.members.contains(where: { m in
                guard let s = m.lastAlarmSnapshot?.state else { return false }
                return s == .RINGING || s == .SNOOZED || s == .UNLOCK_REQUESTED
            }) ?? false
            let delay: UInt64 = anyRinging ? 3_000_000_000 : 10_000_000_000
            try? await Task.sleep(nanoseconds: delay)
            team = try? await repo.get(teamId)
        }
    }

    func leave() async {
        do {
            try await repo.leave(teamId)
            left = true
        } catch {
            self.error = (error as? LocalizedError)?.errorDescription
        }
    }

    func kick(_ userId: String) async {
        try? await repo.kick(teamId, userId: userId)
        await refresh()
    }

    func changeRole(_ userId: String, to role: TeamRole) async {
        _ = try? await repo.changeRole(teamId, userId: userId, role: role)
        await refresh()
    }
}

struct TeamDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(TokenStore.self) private var tokens
    @State private var vm: TeamDetailVm
    @State private var showLeaveConfirm = false

    init(teamId: String) { _vm = State(initialValue: TeamDetailVm(teamId: teamId)) }

    private var myRole: TeamRole? {
        guard let uid = tokens.userId else { return nil }
        return vm.team?.members.first(where: { $0.user.id == uid })?.role
    }

    var body: some View {
        WarmBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HelloHeader(
                        title: vm.team?.name ?? "팀",
                        subtitle: myRole?.label
                    )

                    FASection(title: "동작") {
                        NavigationLink(value: TeamsRoute.invite(vm.teamId)) {
                            ListRow(headline: "초대", supporting: "친구를 팀에 초대")
                        }
                        .buttonStyle(.plain)
                        NavigationLink(value: TeamsRoute.pushAlarm(vm.teamId)) {
                            ListRow(headline: "팀원 깨우기", supporting: "지금 알람 보내기")
                        }
                        .buttonStyle(.plain)
                        NavigationLink(value: TeamsRoute.inbox(vm.teamId)) {
                            ListRow(headline: "잠금해제 요청 인박스")
                        }
                        .buttonStyle(.plain)
                    }

                    FASection(title: "멤버") {
                        if let team = vm.team {
                            ForEach(team.members) { m in
                                ListRow(
                                    headline: m.user.displayName,
                                    supporting: memberSupporting(m),
                                    trailing: {
                                        memberTrailing(for: m)
                                    }
                                )
                            }
                        }
                    }

                    if let error = vm.error { ErrorText(text: error) }

                    Spacer().frame(height: FASpacing.xl)
                    SecondaryButton(text: "팀 탈퇴", destructive: true) {
                        showLeaveConfirm = true
                    }
                    .frame(maxWidth: .infinity)
                    Spacer().frame(height: FASpacing.xxl)
                }
                .padding(.horizontal, FASpacing.lg)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await vm.refresh()
            await vm.livePoll()
        }
        .refreshable { await vm.refresh() }
        .onChange(of: vm.left) { _, left in
            if left { dismiss() }
        }
        .alert("팀 탈퇴", isPresented: $showLeaveConfirm) {
            Button("취소", role: .cancel) {}
            Button("탈퇴", role: .destructive) {
                Task { await vm.leave() }
            }
        } message: {
            Text("정말 나가시겠어요? 오너는 권한을 넘긴 뒤 탈퇴할 수 있어요.")
        }
    }

    private func memberSupporting(_ m: TeamMember) -> String {
        var lines: [String] = [m.role.label]
        if let s = m.lastAlarmSnapshot {
            var parts: [String] = []
            let isLive = s.state == .RINGING || s.state == .UNLOCK_REQUESTED || s.state == .SNOOZED
            if isLive {
                parts.append("🔔 지금 울리는 중")
                let liveVol = s.liveVolumePct ?? s.volumePctAtTrigger
                if let v = liveVol {
                    let warn = v < 30 ? " ⚠️" : ""
                    parts.append("라이브 볼륨 \(v)%\(warn)")
                }
                if s.liveDnd == true || s.dndAtTrigger == true { parts.append("방해금지") }
            } else {
                if let v = s.volumePctAtTrigger {
                    let warn = v < 30 ? " ⚠️" : ""
                    parts.append("최근 볼륨 \(v)%\(warn)")
                }
                if s.dndAtTrigger == true { parts.append("방해금지") }
            }
            if !parts.isEmpty {
                lines.append(parts.joined(separator: " · "))
            }
        }
        return lines.joined(separator: "\n")
    }

    @ViewBuilder
    private func memberTrailing(for m: TeamMember) -> some View {
        let isOwner = myRole == .OWNER
        let canModerate = isOwner || myRole == .ADMIN
        HStack {
            if isOwner, m.role != .OWNER {
                let next: TeamRole = m.role == .ADMIN ? .MEMBER : .ADMIN
                Button(next == .ADMIN ? "관리자로" : "멤버로") {
                    Task { await vm.changeRole(m.user.id, to: next) }
                }
                .font(.system(size: 14))
                .foregroundStyle(FA.primary)
            }
            if canModerate, m.role != .OWNER {
                Button("강퇴") { Task { await vm.kick(m.user.id) } }
                    .font(.system(size: 14))
                    .foregroundStyle(FA.destructive)
            }
        }
    }
}

// MARK: - 초대

@Observable
final class TeamInviteVm {
    var invites: [InviteDto] = []
    var loading = false
    var error: String? = nil
    let teamId: String
    private let repo = TeamsRepository.shared
    init(teamId: String) { self.teamId = teamId }

    func refresh() async {
        invites = (try? await repo.listInvites(teamId)) ?? []
    }

    func create() async {
        loading = true
        error = nil
        do {
            _ = try await repo.createInvite(teamId)
        } catch {
            self.error = (error as? LocalizedError)?.errorDescription
        }
        loading = false
        await refresh()
    }
}

struct TeamInviteView: View {
    @State private var vm: TeamInviteVm
    init(teamId: String) { _vm = State(initialValue: TeamInviteVm(teamId: teamId)) }

    var body: some View {
        WarmBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HelloHeader(title: "초대", subtitle: "코드나 링크를 친구에게 보내세요")

                    Spacer().frame(height: FASpacing.lg)
                    PrimaryButton(
                        text: vm.loading ? "만드는 중…" : "+ 새 초대 코드",
                        enabled: !vm.loading,
                        action: { Task { await vm.create() } }
                    )
                    if let error = vm.error { ErrorText(text: error) }

                    FASection(title: "초대 코드") {
                        if vm.invites.isEmpty {
                            Text("아직 초대 코드가 없어요")
                                .font(.system(size: 14))
                                .foregroundStyle(FA.labelSecondary)
                        } else {
                            ForEach(vm.invites) { inv in
                                ListRow(
                                    headline: inv.code,
                                    supporting: inv.url,
                                    trailing: {
                                        HStack {
                                            Button("코드") { UIPasteboard.general.string = inv.code }
                                                .font(.system(size: 14, weight: .medium))
                                            Button("링크") { UIPasteboard.general.string = inv.url }
                                                .font(.system(size: 14, weight: .medium))
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer().frame(height: FASpacing.xxl)
                }
                .padding(.horizontal, FASpacing.lg)
            }
        }
        .navigationTitle("초대")
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.refresh() }
    }
}

// MARK: - 코드로 가입

struct JoinTeamView: View {
    @Environment(\.dismiss) private var dismiss
    let initialCode: String
    @State private var code: String = ""
    @State private var loading = false
    @State private var error: String? = nil
    @State private var joinedTeamId: String? = nil
    private let repo = TeamsRepository.shared

    var body: some View {
        WarmBackground {
            VStack(alignment: .leading, spacing: FASpacing.sm) {
                HelloHeader(title: "초대 코드 입력", subtitle: "친구가 보낸 코드를 적어주세요")
                Spacer().frame(height: FASpacing.xl)
                FaTextField(placeholder: "예: ABC123", text: $code, autocapitalization: .characters)
                if let error { ErrorText(text: error) }
                Spacer()
                PrimaryButton(
                    text: loading ? "가입 중…" : "가입",
                    enabled: !loading && !code.isEmpty,
                    action: redeem
                )
                Spacer().frame(height: FASpacing.md)
            }
            .padding(.horizontal, FASpacing.lg)
        }
        .navigationTitle("초대 코드")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if !initialCode.isEmpty, code.isEmpty { code = initialCode }
        }
    }

    private func redeem() {
        Task {
            loading = true
            error = nil
            do {
                _ = try await repo.redeemInvite(code)
                dismiss()
            } catch {
                self.error = (error as? LocalizedError)?.errorDescription
            }
            loading = false
        }
    }
}
