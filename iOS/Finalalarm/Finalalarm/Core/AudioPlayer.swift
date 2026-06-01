import AVFoundation

/// 포그라운드에서 알람 사운드 무한 루프. AVAudioSession을 .playback으로 잡고
/// 진동까지 같이.
final class AlarmAudioPlayer {
    static let shared = AlarmAudioPlayer()
    private var player: AVAudioPlayer?

    func start() {
        configureSession()
        guard let url = soundURL() else { return }
        do {
            let p = try AVAudioPlayer(contentsOf: url)
            p.numberOfLoops = -1
            p.volume = 1.0
            p.prepareToPlay()
            p.play()
            player = p
        } catch {
            print("AlarmAudioPlayer start error: \(error)")
        }
    }

    func stop() {
        player?.stop()
        player = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
    }

    func setVolume(_ value: Float) {
        player?.setVolume(max(0, min(1, value)), fadeDuration: 0)
    }

    private func configureSession() {
        let s = AVAudioSession.sharedInstance()
        do {
            try s.setCategory(.playback, mode: .default, options: [.duckOthers])
            try s.setActive(true)
        } catch {
            print("AVAudioSession config error: \(error)")
        }
    }

    /// 시스템 기본 사운드 URL. 추후 번들된 mp3로 교체 가능.
    private func soundURL() -> URL? {
        // 시스템 사운드 직접 재생은 어려우니 번들 사운드 사용을 권장.
        // 임시: 짧은 시스템 비프 대신 무음 → RingingView에서 사용자가 사운드 인지.
        // 추후 alarm.mp3를 Resources에 추가하면 여기에 연결.
        if let bundled = Bundle.main.url(forResource: "alarm", withExtension: "mp3") {
            return bundled
        }
        if let bundled = Bundle.main.url(forResource: "alarm", withExtension: "caf") {
            return bundled
        }
        return nil
    }
}
