import SwiftUI

// MARK: - Login

struct LoginView: View {
    @State private var email = ""
    @State private var password = ""
    @State private var loading = false
    @State private var error: String? = nil
    private let repo = AuthRepository.shared

    var body: some View {
        WarmBackground {
            VStack(alignment: .leading, spacing: FASpacing.sm) {
                Spacer().frame(maxHeight: .infinity)
                HelloHeader(
                    title: "좋은 아침,\n혼자 일어나기 힘들죠?",
                    subtitle: "친구들이 깨워줄게요."
                )
                Spacer().frame(height: FASpacing.xxl)

                FaTextField(placeholder: "이메일", text: $email, keyboardType: .emailAddress)
                FaTextField(placeholder: "비밀번호", text: $password, isSecure: true)

                if let error { ErrorText(text: error) }

                Spacer().frame(height: FASpacing.lg)
                PrimaryButton(
                    text: loading ? "잠시만요…" : "시작하기",
                    enabled: !loading && !email.isEmpty && !password.isEmpty,
                    action: login
                )
                Spacer().frame(height: FASpacing.sm)
                NavigationLink {
                    SignupView()
                } label: {
                    Text("처음이에요 → 계정 만들기")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(FA.primary)
                        .frame(maxWidth: .infinity)
                        .padding()
                }
                Spacer().frame(maxHeight: .infinity)
            }
            .padding(.horizontal, FASpacing.lg)
        }
        .navigationBarBackButtonHidden()
        .toolbar(.hidden, for: .navigationBar)
    }

    private func login() {
        Task {
            loading = true
            error = nil
            do {
                try await repo.login(email: email, password: password)
            } catch {
                self.error = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            }
            loading = false
        }
    }
}

// MARK: - Signup

struct SignupView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var loading = false
    @State private var error: String? = nil
    private let repo = AuthRepository.shared

    var body: some View {
        WarmBackground {
            VStack(alignment: .leading, spacing: FASpacing.sm) {
                HStack {
                    Button(action: { dismiss() }) {
                        Text("뒤로")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundStyle(FA.primary)
                    }
                    Spacer()
                }
                .padding(.top, FASpacing.sm)

                Spacer().frame(maxHeight: .infinity)
                HelloHeader(
                    title: "반가워요!",
                    subtitle: "내일 아침이 기대돼요."
                )
                Spacer().frame(height: FASpacing.xxl)

                FaTextField(placeholder: "어떻게 부를까요?", text: $name)
                FaTextField(placeholder: "이메일", text: $email, keyboardType: .emailAddress)
                FaTextField(placeholder: "비밀번호 (8자 이상)", text: $password, isSecure: true)

                if let error { ErrorText(text: error) }

                Spacer().frame(height: FASpacing.lg)
                PrimaryButton(
                    text: loading ? "가입 중…" : "가입하고 시작",
                    enabled: !loading && !name.isEmpty && !email.isEmpty && password.count >= 8,
                    action: signup
                )
                Spacer().frame(maxHeight: .infinity)
            }
            .padding(.horizontal, FASpacing.lg)
        }
        .navigationBarBackButtonHidden()
        .toolbar(.hidden, for: .navigationBar)
    }

    private func signup() {
        Task {
            loading = true
            error = nil
            do {
                try await repo.signup(email: email, password: password, displayName: name)
            } catch {
                self.error = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            }
            loading = false
        }
    }
}

// MARK: - Onboarding

struct OnboardingView: View {
    let done: () -> Void
    @State private var notifGranted = false
    @State private var checking = false

    var body: some View {
        WarmBackground {
            VStack(alignment: .leading, spacing: FASpacing.sm) {
                Spacer().frame(maxHeight: .infinity)
                HelloHeader(
                    title: "알람이 제대로 울리려면",
                    subtitle: "두 가지만 허용해주세요"
                )
                Spacer().frame(height: FASpacing.xl)

                FASection(title: "알림 권한") {
                    ListRow(
                        headline: "푸시 알림 허용",
                        supporting: notifGranted ? "허용됨" : "탭하여 허용",
                        onTap: requestNotif
                    )
                }

                FASection(title: "포커스/방해금지 모드") {
                    Text("아이폰 설정 → 집중 모드에서 FinalAlarm을 허용 앱에 추가하면 끝까지 울려요.")
                        .font(.system(size: 14))
                        .foregroundStyle(FA.labelSecondary)
                        .padding(.horizontal, FASpacing.sm)
                }

                Spacer().frame(maxHeight: .infinity)

                PrimaryButton(text: "준비 완료", enabled: true, action: done)
                SecondaryButton(text: "나중에", action: done)
                    .frame(maxWidth: .infinity)
                Spacer().frame(height: FASpacing.md)
            }
            .padding(.horizontal, FASpacing.lg)
        }
        .task {
            let settings = await UNUserNotificationCenter.current().notificationSettings()
            notifGranted = settings.authorizationStatus == .authorized
        }
    }

    private func requestNotif() {
        Task {
            let granted = await AlarmScheduler.shared.requestAuthorization()
            notifGranted = granted
        }
    }
}
