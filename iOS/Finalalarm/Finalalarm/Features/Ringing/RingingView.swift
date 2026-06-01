import SwiftUI

/// 풀스크린 알람 화면. 미션을 풀어야 닫힘.
/// FCM/local notification 탭 또는 포그라운드 도달 시 시트로 표시.
struct RingingView: View {
    let alarm: AlarmDto?
    let isTeamApproval: Bool
    let onDismiss: () -> Void

    @State private var mission: MissionDto? = nil
    @State private var showingMission = false
    @State private var missionPassed = false
    @State private var loadingMission = true

    var body: some View {
        ZStack {
            FA.bgGradient.ignoresSafeArea()
            VStack(spacing: FASpacing.lg) {
                Spacer()
                Text("⏰")
                    .font(.system(size: 80))
                Text(isTeamApproval ? "팀과 함께 일어나요" : "일어날 시간이에요")
                    .font(.system(size: 28, weight: .heavy))
                    .foregroundStyle(FA.label)
                if let alarm {
                    Text(alarm.label)
                        .font(.system(size: 18))
                        .foregroundStyle(FA.labelSecondary)
                }
                Spacer()

                if missionPassed {
                    if isTeamApproval {
                        Text("팀원의 승인을 기다리고 있어요…")
                            .font(.system(size: 16))
                            .foregroundStyle(FA.labelSecondary)
                        SecondaryButton(text: "닫기", action: stop)
                            .frame(maxWidth: .infinity)
                    } else {
                        PrimaryButton(text: "알람 해제", enabled: true, action: stop)
                            .padding(.horizontal, FASpacing.lg)
                    }
                } else {
                    PrimaryButton(text: "미션 풀기", enabled: mission != nil, action: {
                        showingMission = true
                    })
                    .padding(.horizontal, FASpacing.lg)
                }
                Spacer().frame(height: FASpacing.xxl)
            }
        }
        .onAppear {
            AlarmAudioPlayer.shared.start()
            Task { await loadMission() }
        }
        .onDisappear { AlarmAudioPlayer.shared.stop() }
        .fullScreenCover(isPresented: $showingMission) {
            if let mission {
                MissionRunnerView(
                    mission: mission,
                    onSuccess: {
                        missionPassed = true
                        showingMission = false
                    },
                    onCancel: { showingMission = false }
                )
            }
        }
    }

    private func loadMission() async {
        guard let mid = alarm?.missionId else {
            loadingMission = false
            return
        }
        mission = try? await MissionsRepository.shared.get(mid)
        loadingMission = false
    }

    private func stop() {
        AlarmAudioPlayer.shared.stop()
        onDismiss()
    }
}
