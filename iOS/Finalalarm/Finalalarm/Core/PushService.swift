import Foundation
import UIKit

/// FCM 통합 진입점.
///
/// **Xcode에서 Firebase SDK를 추가한 뒤** 아래 절차로 활성화하세요:
/// 1. Xcode → File → Add Package Dependencies →
///    `https://github.com/firebase/firebase-ios-sdk` → `FirebaseMessaging` 선택
/// 2. Firebase Console에서 iOS 앱 추가 (Bundle ID `com.jiny.finalalarm`) →
///    `GoogleService-Info.plist` 다운로드 → 프로젝트 루트에 드래그
/// 3. APNs 인증키를 Firebase Console에 업로드
/// 4. 아래 코드의 `#if canImport(FirebaseMessaging)` 블록이 자동 활성화됨
///
/// 그 전까지는 stub으로 동작 — 빌드 깨지지 않음.
@Observable
final class PushService: NSObject {
    static let shared = PushService()

    var fcmToken: String? = nil

    func configure() {
        #if canImport(FirebaseCore) && canImport(FirebaseMessaging)
        // Firebase 추가 후 활성화:
        // FirebaseApp.configure()
        // Messaging.messaging().delegate = self
        #endif
    }

    func onAPNsToken(_ token: Data) {
        #if canImport(FirebaseMessaging)
        // Messaging.messaging().apnsToken = token
        #endif
        let hex = token.map { String(format: "%02x", $0) }.joined()
        print("APNs token: \(hex)")
    }

    /// FCM 토큰을 서버에 등록.
    func sendTokenToServer() {
        guard let token = fcmToken else { return }
        Task {
            try? await PushRepository.shared.registerToken(token, deviceId: Self.deviceId())
        }
    }

    /// 안정적인 디바이스 식별자. 앱 재설치 시 변경됨 (Apple 정책상 OK).
    static func deviceId() -> String {
        UIDevice.current.identifierForVendor?.uuidString ?? "unknown"
    }
}

// Firebase 추가 후 활성화:
// #if canImport(FirebaseMessaging)
// import FirebaseCore
// import FirebaseMessaging
// extension PushService: MessagingDelegate {
//     func messaging(_ messaging: Messaging, didReceiveRegistrationToken token: String?) {
//         self.fcmToken = token
//         sendTokenToServer()
//     }
// }
// #endif
