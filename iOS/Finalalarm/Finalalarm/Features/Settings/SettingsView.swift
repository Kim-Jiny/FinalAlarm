import SwiftUI

@Observable
final class SettingsVm {
    var error: String? = nil
    var pwdError: String? = nil
    var pwdSuccess: Bool = false
    var deleted: Bool = false
    private let repo = AuthRepository.shared

    func logout() async { await repo.logout() }

    func deleteAccount() async {
        do {
            try await repo.deleteMe()
            deleted = true
        } catch {
            self.error = (error as? LocalizedError)?.errorDescription
        }
    }

    func changePassword(current: String, new: String) async {
        pwdError = nil; pwdSuccess = false
        do {
            try await repo.changePassword(current: current, new: new)
            pwdSuccess = true
        } catch {
            self.pwdError = (error as? LocalizedError)?.errorDescription
        }
    }

    func clearPwdState() { pwdError = nil; pwdSuccess = false }
}

struct SettingsView: View {
    @State private var vm = SettingsVm()
    @State private var confirmDelete = false
    @State private var showPwdSheet = false

    var body: some View {
        WarmBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HelloHeader(title: "설정")

                    FASection(title: "알림") {
                        NotificationStatusRow()
                    }

                    FASection(title: "관리") {
                        NavigationLink {
                            MissionListView()
                        } label: {
                            ListRow(headline: "내 미션")
                        }
                        .buttonStyle(.plain)
                        NavigationLink {
                            WindowListView()
                        } label: {
                            ListRow(headline: "알람 시간대")
                        }
                        .buttonStyle(.plain)
                        NavigationLink {
                            HistoryView()
                        } label: {
                            ListRow(headline: "히스토리")
                        }
                        .buttonStyle(.plain)
                    }

                    FASection(title: "계정") {
                        ListRow(headline: "비밀번호 변경", onTap: { showPwdSheet = true })
                        ListRow(headline: "로그아웃", onTap: { Task { await vm.logout() } })
                        ListRow(
                            headline: "계정 삭제",
                            destructive: true,
                            onTap: { confirmDelete = true }
                        )
                    }

                    if let error = vm.error { ErrorText(text: error) }
                    Spacer().frame(height: FASpacing.xxl)
                }
                .padding(.horizontal, FASpacing.lg)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .alert("계정 삭제", isPresented: $confirmDelete) {
            Button("취소", role: .cancel) {}
            Button("삭제", role: .destructive) { Task { await vm.deleteAccount() } }
        } message: {
            Text("알람·팀 기록이 모두 사라집니다. 되돌릴 수 없어요.")
        }
        .sheet(isPresented: $showPwdSheet, onDismiss: { vm.clearPwdState() }) {
            ChangePasswordSheet(vm: vm)
        }
    }
}

struct NotificationStatusRow: View {
    @State private var status: UNAuthorizationStatus = .notDetermined
    var body: some View {
        ListRow(
            headline: "알림 권한",
            supporting: statusText,
            onTap: openSettings
        )
        .task { await refresh() }
    }

    private var statusText: String {
        switch status {
        case .authorized, .ephemeral: return "허용됨"
        case .denied: return "거부됨 — 탭하여 설정 열기"
        case .notDetermined: return "미설정 — 탭하여 허용"
        case .provisional: return "임시 허용"
        @unknown default: return "알 수 없음"
        }
    }

    private func refresh() async {
        let s = await UNUserNotificationCenter.current().notificationSettings()
        status = s.authorizationStatus
    }

    private func openSettings() {
        if status == .notDetermined {
            Task {
                _ = await AlarmScheduler.shared.requestAuthorization()
                await refresh()
            }
        } else if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }
}

struct ChangePasswordSheet: View {
    @Environment(\.dismiss) private var dismiss
    let vm: SettingsVm
    @State private var current = ""
    @State private var new = ""
    @State private var confirm = ""

    var body: some View {
        WarmBackground {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Button("취소") { dismiss() }
                        .foregroundStyle(FA.primary)
                    Spacer()
                    Text("비밀번호 변경")
                        .font(.system(size: 17, weight: .semibold))
                    Spacer()
                    Button("변경") {
                        Task {
                            await vm.changePassword(current: current, new: new)
                            if vm.pwdSuccess { dismiss() }
                        }
                    }
                    .foregroundStyle(FA.primary)
                    .disabled(current.isEmpty || new.count < 8 || new != confirm)
                }
                .padding(.horizontal, FASpacing.lg)
                .padding(.vertical, FASpacing.md)

                VStack(alignment: .leading, spacing: FASpacing.sm) {
                    FaTextField(placeholder: "현재 비밀번호", text: $current, isSecure: true)
                    FaTextField(placeholder: "새 비밀번호 (8자 이상)", text: $new, isSecure: true)
                    FaTextField(placeholder: "새 비밀번호 확인", text: $confirm, isSecure: true)
                    if let error = vm.pwdError { ErrorText(text: error) }
                    if !new.isEmpty, !confirm.isEmpty, new != confirm {
                        ErrorText(text: "비밀번호가 일치하지 않습니다")
                    }
                }
                .padding(.horizontal, FASpacing.lg)
                Spacer()
            }
        }
    }
}
