import Foundation

enum AppConfig {
    /// 로컬 서버 LAN IP. Android와 동일하게 디버그용으로 LAN IP 사용.
    /// 운영 빌드에서는 실제 도메인으로 교체.
    static let apiBaseURL = URL(string: "http://172.30.1.22:3500/api/v1/")!

    static let deepLinkScheme = "finalalarm"
    static let deepLinkInviteHost = "i"
}
