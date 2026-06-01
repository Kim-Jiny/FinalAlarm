import Foundation

enum APIError: LocalizedError {
    case transport(URLError)
    case http(Int, String?)
    case decoding(Error)
    case unauthorized
    case offline

    var errorDescription: String? {
        switch self {
        case .transport:
            return "네트워크 연결을 확인해주세요"
        case .http(let code, let msg):
            if code == 409 { return msg ?? "이미 사용 중입니다" }
            if code >= 500 { return "서버 오류 (재시도해주세요)" }
            return msg ?? "요청에 실패했어요 (\(code))"
        case .decoding:
            return "응답 형식이 올바르지 않아요"
        case .unauthorized:
            return "로그인이 필요합니다"
        case .offline:
            return "오프라인 상태입니다"
        }
    }
}

/// URLSession + async/await + 토큰 자동 첨부 + 401 자동 refresh.
@Observable
final class APIClient {
    static let shared = APIClient()

    private let session: URLSession
    private let baseURL: URL
    private let tokens: TokenStore
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder
    private var refreshTask: Task<Void, Error>?

    init(
        baseURL: URL = AppConfig.apiBaseURL,
        session: URLSession = .shared,
        tokens: TokenStore = .shared
    ) {
        self.baseURL = baseURL
        self.session = session
        self.tokens = tokens
        self.encoder = JSONEncoder()
        self.decoder = JSONDecoder()
    }

    // MARK: - High-level

    func get<R: Decodable>(_ path: String, query: [String: String] = [:]) async throws -> R {
        try await request(path: path, method: "GET", query: query, body: Empty?.none)
    }

    func post<B: Encodable, R: Decodable>(_ path: String, _ body: B) async throws -> R {
        try await request(path: path, method: "POST", body: body)
    }

    func postNoContent<B: Encodable>(_ path: String, _ body: B) async throws {
        let _: NoContent = try await request(path: path, method: "POST", body: body)
    }

    func patch<B: Encodable, R: Decodable>(_ path: String, _ body: B) async throws -> R {
        try await request(path: path, method: "PATCH", body: body)
    }

    func delete(_ path: String) async throws {
        let _: NoContent = try await request(path: path, method: "DELETE", body: Empty?.none)
    }

    // MARK: - Core

    private func request<B: Encodable, R: Decodable>(
        path: String,
        method: String,
        query: [String: String] = [:],
        body: B?,
        retryOn401: Bool = true
    ) async throws -> R {
        var comps = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)!
        if !query.isEmpty {
            comps.queryItems = query.map { URLQueryItem(name: $0.key, value: $0.value) }
        }
        guard let url = comps.url else { throw APIError.http(0, "잘못된 URL") }

        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        if let token = tokens.accessToken {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body, !(body is Empty?) {
            req.httpBody = try encoder.encode(body)
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: req)
        } catch let urlErr as URLError {
            throw APIError.transport(urlErr)
        }

        guard let http = response as? HTTPURLResponse else {
            throw APIError.http(0, "응답 없음")
        }

        if http.statusCode == 401 && retryOn401 && tokens.refreshToken != nil && !path.hasPrefix("auth/") {
            try await refreshIfNeeded()
            return try await request(path: path, method: method, query: query, body: body, retryOn401: false)
        }

        if !(200..<300).contains(http.statusCode) {
            let msg = extractMessage(data)
            if http.statusCode == 401 { throw APIError.unauthorized }
            throw APIError.http(http.statusCode, msg)
        }

        if R.self == NoContent.self {
            return NoContent() as! R
        }
        do {
            return try decoder.decode(R.self, from: data)
        } catch {
            throw APIError.decoding(error)
        }
    }

    private func extractMessage(_ data: Data) -> String? {
        struct ErrorBody: Decodable { let message: String?; let error: String? }
        if let body = try? JSONDecoder().decode(ErrorBody.self, from: data) {
            return body.message ?? body.error
        }
        // NestJS는 message가 배열로 올 수도 있음
        struct ErrorBodyArr: Decodable { let message: [String]?; let error: String? }
        if let body = try? JSONDecoder().decode(ErrorBodyArr.self, from: data),
           let arr = body.message, let first = arr.first {
            return first
        }
        return nil
    }

    // MARK: - Refresh

    private func refreshIfNeeded() async throws {
        if let existing = refreshTask {
            try await existing.value
            return
        }
        let task = Task<Void, Error> {
            defer { self.refreshTask = nil }
            guard let refresh = tokens.refreshToken else {
                throw APIError.unauthorized
            }
            do {
                let res: AuthResponse = try await request(
                    path: "auth/refresh",
                    method: "POST",
                    body: RefreshRequest(refreshToken: refresh),
                    retryOn401: false
                )
                tokens.save(access: res.accessToken, refresh: res.refreshToken, userId: res.user.id)
            } catch {
                tokens.clear()
                throw APIError.unauthorized
            }
        }
        refreshTask = task
        try await task.value
    }
}

/// 빈 바디.
struct Empty: Codable {}
/// 응답이 본문 없을 때 표식.
struct NoContent: Codable {}
