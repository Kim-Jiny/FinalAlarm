import SwiftUI

/// 로그인 여부에 따라 분기. 토큰 변화 감지 → 자동 전환.
struct RootView: View {
    @Environment(TokenStore.self) private var tokens
    @State private var onboardingDone = false
    @State private var pendingInviteCode: String? = nil

    var body: some View {
        Group {
            if tokens.isLoggedIn {
                if onboardingDone || hasSeenOnboarding() {
                    MainTabView(initialInviteCode: pendingInviteCode)
                        .onAppear { pendingInviteCode = nil }
                } else {
                    OnboardingView(done: {
                        UserDefaults.standard.set(true, forKey: "onboardingDone")
                        onboardingDone = true
                    })
                }
            } else {
                NavigationStack {
                    LoginView()
                }
            }
        }
        .onOpenURL { url in
            // finalalarm://i/<code>
            guard url.scheme == AppConfig.deepLinkScheme,
                  url.host == AppConfig.deepLinkInviteHost else { return }
            let code = url.lastPathComponent
            if !code.isEmpty {
                pendingInviteCode = code
            }
        }
    }

    private func hasSeenOnboarding() -> Bool {
        UserDefaults.standard.bool(forKey: "onboardingDone")
    }
}
