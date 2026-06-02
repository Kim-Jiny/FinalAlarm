import SwiftUI

/// 풀스크린 알람 화면. 미션을 풀어야 닫힘.
/// FCM/local notification 탭 또는 포그라운드 도달 시 시트로 표시.
struct RingingView: View {
    let alarm: AlarmDto?
    let isTeamApproval: Bool
    let onDismiss: () -> Void

    @State private var mission: MissionDto? = nil
    @State private var eventId: String? = nil
    @State private var showingMission = false
    @State private var missionPassed = false
    @State private var loadingMission = true
    @State private var heartbeatTask: Task<Void, Never>? = nil

    private let eventsRepo = EventsRepository.shared
    private let missionsRepo = MissionsRepository.shared

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
            Task {
                await loadMission()
                await reportTrigger()
                await heartbeatLoop()
            }
        }
        .onDisappear {
            AlarmAudioPlayer.shared.stop()
            heartbeatTask?.cancel()
        }
        .fullScreenCover(isPresented: $showingMission) {
            if let mission {
                MissionRunnerView(
                    mission: mission,
                    onSuccess: {
                        missionPassed = true
                        showingMission = false
                        Task { await reportDismiss(passedMission: mission) }
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
        mission = try? await missionsRepo.get(mid)
        loadingMission = false
    }

    /// 알람 울리는 시점 디바이스 상태를 서버에 보고 → eventId 받음.
    /// 네트워크 실패 시 PendingEventStore에 적재해 다음 진입 시 reconcile.
    private func reportTrigger() async {
        guard let alarm else { return }
        let ds = DeviceState.probe()
        let triggeredAt = ISO8601DateFormatter.withMillis.string(from: Date())
        let req = CreateAlarmEventRequest(
            definitionId: alarm.id,
            triggeredAt: triggeredAt,
            initialState: "RINGING",
            dismissedAt: nil,
            volumePctAtTrigger: ds.volumePct,
            dndAtTrigger: ds.dnd,
            volumePctAtDismiss: nil,
            dndAtDismiss: nil
        )
        do {
            let event = try await eventsRepo.create(req)
            eventId = event.id
        } catch {
            // 오프라인 — 로컬 ID 부여하고 큐에 적재
            let localId = "local-\(UUID().uuidString)"
            eventId = localId
            PendingEventStore.shared.add(PendingEventStore.Pending(
                localId: localId,
                definitionId: alarm.id,
                triggeredAt: triggeredAt,
                dismissed: false,
                dismissedAt: nil,
                volumePctAtTrigger: ds.volumePct,
                dndAtTrigger: ds.dnd,
                volumePctAtDismiss: nil,
                dndAtDismiss: nil
            ))
        }
    }

    /// 미션 통과 후 dismiss API 호출 (개인 알람만; 팀 승인은 별도 흐름).
    /// 오프라인이면 큐에 dismiss 표시.
    private func reportDismiss(passedMission m: MissionDto) async {
        guard !isTeamApproval, let id = eventId else { return }
        let ds = DeviceState.probe()
        let dismissedAt = ISO8601DateFormatter.withMillis.string(from: Date())

        // 로컬 이벤트면 큐에만 표시
        if id.hasPrefix("local-") {
            PendingEventStore.shared.markDismissed(
                localId: id,
                dismissedAt: dismissedAt,
                volumePct: ds.volumePct,
                dnd: ds.dnd
            )
            return
        }

        var proof: [String: AnyCodable] = ["type": .string(m.type.rawValue)]
        switch m.type {
        case .MATH: proof["answers"] = .array([])
        case .PHOTO: proof["imageUrl"] = .string("")
        case .SHAKE: proof["shakeCount"] = .int(0)
        }
        let req = DismissRequest(missionProof: proof, volumePct: ds.volumePct, dnd: ds.dnd)
        _ = try? await eventsRepo.dismiss(id, req)
    }

    /// 알람 울리는 동안 5초마다 디바이스 상태 라이브 보고. 오프라인(local-) 이벤트는 스킵.
    private func heartbeatLoop() async {
        heartbeatTask?.cancel()
        heartbeatTask = Task {
            while !Task.isCancelled {
                if let id = eventId, !id.hasPrefix("local-"), !missionPassed {
                    let ds = DeviceState.probe()
                    try? await eventsRepo.heartbeat(id, volumePct: ds.volumePct, dnd: ds.dnd)
                }
                try? await Task.sleep(nanoseconds: 5_000_000_000)
            }
        }
    }

    private func stop() {
        AlarmAudioPlayer.shared.stop()
        heartbeatTask?.cancel()
        onDismiss()
    }
}
