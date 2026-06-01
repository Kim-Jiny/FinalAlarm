import Foundation
import Security

/// Keychain 기반 토큰 저장소. access/refresh + userId.
@Observable
final class TokenStore {
    static let shared = TokenStore()

    private let service = "com.jiny.finalalarm.tokens"
    private let accessKey = "access"
    private let refreshKey = "refresh"
    private let userIdKey = "userId"

    private(set) var accessToken: String?
    private(set) var refreshToken: String?
    private(set) var userId: String?
    var isLoggedIn: Bool { accessToken != nil }

    init() {
        accessToken = read(accessKey)
        refreshToken = read(refreshKey)
        userId = read(userIdKey)
    }

    func save(access: String, refresh: String, userId: String) {
        write(accessKey, access)
        write(refreshKey, refresh)
        write(userIdKey, userId)
        self.accessToken = access
        self.refreshToken = refresh
        self.userId = userId
    }

    func updateAccess(_ access: String) {
        write(accessKey, access)
        self.accessToken = access
    }

    func clear() {
        delete(accessKey); delete(refreshKey); delete(userIdKey)
        self.accessToken = nil; self.refreshToken = nil; self.userId = nil
    }

    // MARK: - Keychain primitives

    private func write(_ key: String, _ value: String) {
        let data = Data(value.utf8)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
        let attrs: [String: Any] = [kSecValueData as String: data]
        let status = SecItemUpdate(query as CFDictionary, attrs as CFDictionary)
        if status == errSecItemNotFound {
            var add = query
            add[kSecValueData as String] = data
            add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
            SecItemAdd(add as CFDictionary, nil)
        }
    }

    private func read(_ key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private func delete(_ key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
        SecItemDelete(query as CFDictionary)
    }
}
