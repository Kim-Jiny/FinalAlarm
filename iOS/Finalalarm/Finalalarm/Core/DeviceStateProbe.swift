import Foundation
import AVFoundation

/// 알람 발사·해제 시점의 디바이스 상태 스냅샷.
/// - volumePct: 미디어 출력 볼륨 (0~100). 우리 알람은 .playback 카테고리라 이 값으로 재생됨.
/// - dnd: iOS는 공식 API 없음 → 항상 nil 보고. 안드로이드와 일관성 유지용으로 필드만 둠.
struct DeviceState {
    let volumePct: Int
    let dnd: Bool?

    static func probe() -> DeviceState {
        let session = AVAudioSession.sharedInstance()
        // outputVolume은 활성 세션이 아니어도 시스템 볼륨을 반환
        let vol = session.outputVolume
        let pct = Int((vol * 100).rounded()).clamped(to: 0...100)
        return DeviceState(volumePct: pct, dnd: nil)
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
