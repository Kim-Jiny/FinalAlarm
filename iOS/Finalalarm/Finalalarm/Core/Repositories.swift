import Foundation

// MARK: - Auth

@Observable
final class AuthRepository {
    static let shared = AuthRepository()
    private let api: APIClient
    private let tokens: TokenStore
    init(api: APIClient = .shared, tokens: TokenStore = .shared) {
        self.api = api
        self.tokens = tokens
    }

    func signup(email: String, password: String, displayName: String) async throws {
        let tz = TimeZone.current.identifier
        let res: AuthResponse = try await api.post("auth/signup", SignupRequest(
            email: email, password: password, displayName: displayName, timezone: tz
        ))
        tokens.save(access: res.accessToken, refresh: res.refreshToken, userId: res.user.id)
    }

    func login(email: String, password: String) async throws {
        let res: AuthResponse = try await api.post("auth/login", LoginRequest(
            email: email, password: password
        ))
        tokens.save(access: res.accessToken, refresh: res.refreshToken, userId: res.user.id)
    }

    func logout() async {
        if let refresh = tokens.refreshToken {
            _ = try? await api.post("auth/logout", RefreshRequest(refreshToken: refresh)) as NoContent
        }
        tokens.clear()
    }

    func deleteMe() async throws {
        try await api.delete("me")
        tokens.clear()
    }

    func changePassword(current: String, new: String) async throws {
        try await api.postNoContent("me/password", ChangePasswordRequest(
            currentPassword: current, newPassword: new
        ))
    }

    func me() async throws -> PublicUser {
        try await api.get("me")
    }
}

// MARK: - Teams

@Observable
final class TeamsRepository {
    static let shared = TeamsRepository()
    private let api: APIClient
    init(api: APIClient = .shared) { self.api = api }

    func list() async throws -> [TeamSummary] { try await api.get("teams") }
    func get(_ id: String) async throws -> TeamDetail { try await api.get("teams/\(id)") }
    func create(name: String) async throws -> TeamDetail {
        try await api.post("teams", CreateTeamRequest(name: name))
    }
    func leave(_ id: String) async throws { try await api.delete("teams/\(id)/members/me") }
    func kick(_ teamId: String, userId: String) async throws {
        try await api.delete("teams/\(teamId)/members/\(userId)")
    }
    func changeRole(_ teamId: String, userId: String, role: TeamRole) async throws -> TeamMember {
        try await api.patch("teams/\(teamId)/members/\(userId)", ChangeRoleRequest(role: role))
    }

    func listInvites(_ teamId: String) async throws -> [InviteDto] {
        try await api.get("teams/\(teamId)/invites")
    }
    func createInvite(_ teamId: String, expiresInDays: Int = 7) async throws -> InviteDto {
        try await api.post("teams/\(teamId)/invites", CreateInviteRequest(expiresInDays: expiresInDays))
    }
    func redeemInvite(_ code: String) async throws -> RedeemInviteResponse {
        try await api.post("team-invites/\(code.uppercased())/redeem", Empty())
    }
}

// MARK: - Missions

@Observable
final class MissionsRepository {
    static let shared = MissionsRepository()
    private let api: APIClient
    init(api: APIClient = .shared) { self.api = api }

    func list() async throws -> [MissionDto] { try await api.get("missions") }
    func get(_ id: String) async throws -> MissionDto { try await api.get("missions/\(id)") }
    func create(_ req: CreateMissionRequest) async throws -> MissionDto {
        try await api.post("missions", req)
    }
    func update(_ id: String, _ req: UpdateMissionRequest) async throws -> MissionDto {
        try await api.patch("missions/\(id)", req)
    }
    func delete(_ id: String) async throws { try await api.delete("missions/\(id)") }
}

// MARK: - Alarms

@Observable
final class AlarmsRepository {
    static let shared = AlarmsRepository()
    private let api: APIClient
    init(api: APIClient = .shared) { self.api = api }

    func list(active: Bool? = nil) async throws -> [AlarmDto] {
        try await api.get("alarms", query: active.map { ["active": "\($0)"] } ?? [:])
    }
    func get(_ id: String) async throws -> AlarmDto { try await api.get("alarms/\(id)") }
    func create(_ req: CreateAlarmRequest) async throws -> AlarmDto {
        try await api.post("alarms", req)
    }
    func update(_ id: String, _ body: [String: AnyCodable]) async throws -> AlarmDto {
        try await api.patch("alarms/\(id)", body)
    }
    func delete(_ id: String) async throws { try await api.delete("alarms/\(id)") }
}

// MARK: - Windows

@Observable
final class WindowsRepository {
    static let shared = WindowsRepository()
    private let api: APIClient
    init(api: APIClient = .shared) { self.api = api }

    func list() async throws -> [WindowDto] { try await api.get("alarm-windows") }
    func create(_ req: CreateWindowRequest) async throws -> WindowDto {
        try await api.post("alarm-windows", req)
    }
    func update(_ id: String, _ body: [String: AnyCodable]) async throws -> WindowDto {
        try await api.patch("alarm-windows/\(id)", body)
    }
    func delete(_ id: String) async throws { try await api.delete("alarm-windows/\(id)") }
}

// MARK: - Alarm events / Inbox / History

@Observable
final class EventsRepository {
    static let shared = EventsRepository()
    private let api: APIClient
    init(api: APIClient = .shared) { self.api = api }

    func history(limit: Int = 100) async throws -> [AlarmEventDto] {
        try await api.get("alarm-events/history", query: ["limit": "\(limit)"])
    }

    func active() async throws -> [AlarmEventDto] {
        try await api.get("alarm-events")
    }

    func create(_ req: CreateAlarmEventRequest) async throws -> AlarmEventDto {
        try await api.post("alarm-events", req)
    }

    func dismiss(_ id: String, _ req: DismissRequest) async throws -> AlarmEventDto {
        try await api.post("alarm-events/\(id)/dismiss", req)
    }

    func heartbeat(_ id: String, volumePct: Int, dnd: Bool?) async throws {
        try await api.postNoContent("alarm-events/\(id)/heartbeat", HeartbeatRequest(volumePct: volumePct, dnd: dnd))
    }

    func inbox(_ teamId: String, status: UnlockRequestStatus = .PENDING) async throws -> [UnlockRequestDto] {
        try await api.get("unlock-requests/inbox", query: ["teamId": teamId, "status": status.rawValue])
    }

    func getUnlockRequest(_ id: String) async throws -> UnlockRequestDto {
        try await api.get("unlock-requests/\(id)")
    }

    func approveUnlock(_ id: String) async throws -> UnlockRequestDto {
        try await api.post("unlock-requests/\(id)/approve", Empty())
    }
}

// MARK: - Push alarm + token

@Observable
final class PushRepository {
    static let shared = PushRepository()
    private let api: APIClient
    init(api: APIClient = .shared) { self.api = api }

    func send(_ req: PushAlarmRequest) async throws {
        try await api.postNoContent("push-alarm", req)
    }

    func registerToken(_ token: String, deviceId: String) async throws {
        try await api.postNoContent("push-tokens", RegisterPushTokenRequest(
            token: token, platform: .IOS, deviceId: deviceId
        ))
    }
}
