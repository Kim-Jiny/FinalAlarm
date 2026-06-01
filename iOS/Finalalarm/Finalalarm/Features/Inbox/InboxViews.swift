import SwiftUI

@Observable
final class InboxListVm {
    var items: [UnlockRequestDto] = []
    private let repo = EventsRepository.shared
    func load(_ teamId: String) async {
        items = (try? await repo.inbox(teamId)) ?? []
    }
}

struct InboxListView: View {
    let teamId: String
    @State private var vm = InboxListVm()

    var body: some View {
        WarmBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if vm.items.isEmpty {
                        EmptyState(text: "대기 중인 요청이 없어요")
                    } else {
                        ForEach(vm.items) { r in
                            NavigationLink(value: TeamsRoute.unlockDetail(r.id)) {
                                ListRow(
                                    headline: "요청자 \(r.requesterId.prefix(8))",
                                    supporting: "만료 \(formatDate(r.expiresAt))"
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    Spacer().frame(height: FASpacing.xxl)
                }
                .padding(.horizontal, FASpacing.lg)
            }
        }
        .navigationTitle("잠금해제 요청")
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.load(teamId) }
        .refreshable { await vm.load(teamId) }
    }

    private func formatDate(_ s: String) -> String {
        let trimmed = s.split(separator: ".").first.map(String.init) ?? s
        return trimmed.replacingOccurrences(of: "T", with: " ")
    }
}

@Observable
final class UnlockRequestDetailVm {
    var request: UnlockRequestDto? = nil
    var approving = false
    var error: String? = nil
    var done = false
    let id: String
    private let repo = EventsRepository.shared
    init(id: String) { self.id = id }

    func refresh() async {
        request = try? await repo.getUnlockRequest(id)
    }

    func approve() async {
        approving = true; error = nil
        do {
            _ = try await repo.approveUnlock(id)
            done = true
        } catch {
            self.error = (error as? LocalizedError)?.errorDescription
        }
        approving = false
    }
}

struct UnlockRequestDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var vm: UnlockRequestDetailVm
    init(id: String) { _vm = State(initialValue: UnlockRequestDetailVm(id: id)) }

    var body: some View {
        WarmBackground {
            VStack(alignment: .leading, spacing: 0) {
                HelloHeader(title: "잠금해제 요청", subtitle: "팀원이 도움을 요청했어요")

                if let r = vm.request {
                    FASection(title: "정보") {
                        ListRow(headline: "요청자", supporting: r.requesterId.prefix(8).description)
                        ListRow(headline: "상태", supporting: r.status.label)
                        ListRow(
                            headline: "만료",
                            supporting: r.expiresAt.split(separator: ".").first.map(String.init)?
                                .replacingOccurrences(of: "T", with: " ") ?? r.expiresAt
                        )
                    }
                } else {
                    Spacer().frame(height: FASpacing.xl)
                    ProgressView()
                        .frame(maxWidth: .infinity)
                }

                if let error = vm.error { ErrorText(text: error) }

                Spacer()
                PrimaryButton(
                    text: vm.approving ? "승인 중…" : "승인",
                    enabled: !vm.approving && vm.request?.status == .PENDING,
                    action: {
                        Task {
                            await vm.approve()
                            if vm.done { dismiss() }
                        }
                    }
                )
                Spacer().frame(height: FASpacing.md)
            }
            .padding(.horizontal, FASpacing.lg)
        }
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.refresh() }
    }
}
