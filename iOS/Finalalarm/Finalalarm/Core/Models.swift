import Foundation

// MARK: - Auth

struct AuthTokens: Codable {
    let accessToken: String
    let refreshToken: String
    let expiresIn: Int
}

struct PublicUser: Codable, Identifiable {
    let id: String
    let email: String
    let displayName: String
    let timezone: String?
}

struct AuthResponse: Codable {
    let user: PublicUser
    let accessToken: String
    let refreshToken: String
    let expiresIn: Int
}

struct SignupRequest: Codable {
    let email: String
    let password: String
    let displayName: String
    let timezone: String
}

struct LoginRequest: Codable {
    let email: String
    let password: String
}

struct RefreshRequest: Codable {
    let refreshToken: String
}

struct ChangePasswordRequest: Codable {
    let currentPassword: String
    let newPassword: String
}

// MARK: - Teams

enum TeamRole: String, Codable {
    case OWNER, ADMIN, MEMBER
    var label: String {
        switch self {
        case .OWNER: return "오너"
        case .ADMIN: return "관리자"
        case .MEMBER: return "멤버"
        }
    }
}

struct TeamSummary: Codable, Identifiable {
    let id: String
    let name: String
    let role: TeamRole
}

struct TeamMember: Codable, Identifiable {
    let teamId: String
    let userId: String
    let role: TeamRole
    let user: PublicUser
    let joinedAt: String
    let lastAlarmSnapshot: LastAlarmSnapshot?

    var id: String { "\(teamId)/\(userId)" }
}

struct LastAlarmSnapshot: Codable {
    let id: String
    let targetUserId: String
    let state: AlarmEventState
    let triggeredAt: String
    let dismissedAt: String?
    let volumePctAtTrigger: Int?
    let dndAtTrigger: Bool?
    let volumePctAtDismiss: Int?
    let dndAtDismiss: Bool?
    let lastSeenAt: String?
    let liveVolumePct: Int?
    let liveDnd: Bool?
}

struct HeartbeatRequest: Codable {
    let volumePct: Int
    let dnd: Bool?
}

struct TeamDetail: Codable, Identifiable {
    let id: String
    let name: String
    let createdAt: String
    let members: [TeamMember]
}

struct CreateTeamRequest: Codable { let name: String }
struct ChangeRoleRequest: Codable { let role: TeamRole }

struct InviteDto: Codable, Identifiable {
    let id: String
    let code: String
    let url: String
    let expiresAt: String
}

struct CreateInviteRequest: Codable { let expiresInDays: Int }
struct RedeemInviteResponse: Codable { let teamId: String }

// MARK: - Missions

enum MissionType: String, Codable, CaseIterable {
    case MATH, PHOTO, SHAKE
    var label: String {
        switch self {
        case .MATH: return "수학"
        case .PHOTO: return "사진"
        case .SHAKE: return "흔들기"
        }
    }
}

/// 미션 config는 자유로운 JSON. 디코딩/인코딩을 직접 처리.
struct MissionConfig: Codable {
    var raw: [String: AnyCodable]
    init(_ raw: [String: AnyCodable] = [:]) { self.raw = raw }
    init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        self.raw = try c.decode([String: AnyCodable].self)
    }
    func encode(to encoder: Encoder) throws {
        var c = encoder.singleValueContainer()
        try c.encode(raw)
    }
}

struct MissionDto: Codable, Identifiable {
    let id: String
    let type: MissionType
    let name: String
    let config: MissionConfig
    let isDefault: Bool
    let createdAt: String
}

struct CreateMissionRequest: Codable {
    let type: MissionType
    let name: String
    let config: MissionConfig
    let isDefault: Bool
}

struct UpdateMissionRequest: Codable {
    let name: String?
    let config: MissionConfig?
    let isDefault: Bool?
}

// MARK: - Alarms

enum AlarmKind: String, Codable, CaseIterable {
    case PERSONAL, TEAM_APPROVAL
    var label: String {
        switch self {
        case .PERSONAL: return "혼자"
        case .TEAM_APPROVAL: return "팀원 승인"
        }
    }
}

enum ScheduleType: String, Codable { case RECURRING, ONE_SHOT }
enum VibrationPattern: String, Codable, CaseIterable { case OFF, LIGHT, PULSE, STRONG }

struct AlarmDto: Codable, Identifiable {
    let id: String
    let userId: String
    let kind: AlarmKind
    let teamId: String?
    let label: String
    let timezone: String
    let scheduleType: ScheduleType
    let timeOfDay: String?
    let daysOfWeek: Int?
    let oneShotAt: String?
    let soundUri: String
    let volume: Int
    let volumeRampSeconds: Int
    let vibrationEnabled: Bool
    let vibrationPattern: VibrationPattern
    let snoozeEnabled: Bool
    let snoozeMinutes: Int
    let snoozeMaxCount: Int
    let missionId: String?
    let active: Bool
    let createdAt: String
}

struct CreateAlarmRequest: Codable {
    let kind: AlarmKind
    let teamId: String?
    let label: String
    let timezone: String
    let scheduleType: ScheduleType
    let timeOfDay: String?
    let daysOfWeek: Int?
    let oneShotAt: String?
    let soundUri: String
    let volume: Int
    let volumeRampSeconds: Int
    let vibrationEnabled: Bool
    let vibrationPattern: VibrationPattern
    let snoozeEnabled: Bool
    let snoozeMinutes: Int
    let snoozeMaxCount: Int
    let missionId: String
}

// MARK: - Windows

struct WindowDto: Codable, Identifiable {
    let id: String
    let teamId: String
    let startTime: String
    let endTime: String
    let daysOfWeek: Int
    let timezone: String
    let createdAt: String
}

struct CreateWindowRequest: Codable {
    let teamId: String
    let startTime: String
    let endTime: String
    let daysOfWeek: Int
    let timezone: String
}

// MARK: - Alarm events

enum AlarmEventState: String, Codable {
    case RINGING, UNLOCK_REQUESTED, UNLOCK_APPROVED, SNOOZED, DISMISSED, EXPIRED
    var label: String {
        switch self {
        case .RINGING: return "울리는 중"
        case .UNLOCK_REQUESTED: return "요청 중"
        case .UNLOCK_APPROVED: return "승인됨"
        case .SNOOZED: return "스누즈"
        case .DISMISSED: return "해제"
        case .EXPIRED: return "만료"
        }
    }
}

struct AlarmEventDto: Codable, Identifiable {
    let id: String
    let alarmId: String?
    let userId: String
    let senderUserId: String?
    let triggeredAt: String
    let dismissedAt: String?
    let state: AlarmEventState
    let volumePctAtTrigger: Int?
    let dndAtTrigger: Bool?
    let volumePctAtDismiss: Int?
    let dndAtDismiss: Bool?
}

struct CreateAlarmEventRequest: Codable {
    let definitionId: String
    let triggeredAt: String?
    let initialState: String?
    let dismissedAt: String?
    let volumePctAtTrigger: Int?
    let dndAtTrigger: Bool?
    let volumePctAtDismiss: Int?
    let dndAtDismiss: Bool?
}

struct DismissRequest: Codable {
    let missionProof: [String: AnyCodable]
    let volumePct: Int?
    let dnd: Bool?
}

// MARK: - Unlock requests

enum UnlockRequestStatus: String, Codable {
    case PENDING, APPROVED, EXPIRED
    var label: String {
        switch self {
        case .PENDING: return "대기 중"
        case .APPROVED: return "승인됨"
        case .EXPIRED: return "만료됨"
        }
    }
}

struct UnlockRequestDto: Codable, Identifiable {
    let id: String
    let alarmEventId: String
    let teamId: String
    let requesterId: String
    let status: UnlockRequestStatus
    let createdAt: String
    let expiresAt: String
}

// MARK: - Push alarm

struct PushAlarmRequest: Codable {
    let toUserId: String
    let teamId: String
    let label: String
}

// MARK: - Push tokens

enum PushPlatform: String, Codable { case ios, android }

struct RegisterPushTokenRequest: Codable {
    let token: String
    let platform: PushPlatform
}

// MARK: - AnyCodable (간단한 JSON 표현)

enum AnyCodable: Codable {
    case string(String)
    case int(Int)
    case double(Double)
    case bool(Bool)
    case array([AnyCodable])
    case object([String: AnyCodable])
    case null

    init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        if c.decodeNil() { self = .null; return }
        if let v = try? c.decode(Bool.self) { self = .bool(v); return }
        if let v = try? c.decode(Int.self) { self = .int(v); return }
        if let v = try? c.decode(Double.self) { self = .double(v); return }
        if let v = try? c.decode(String.self) { self = .string(v); return }
        if let v = try? c.decode([AnyCodable].self) { self = .array(v); return }
        if let v = try? c.decode([String: AnyCodable].self) { self = .object(v); return }
        throw DecodingError.dataCorruptedError(in: c, debugDescription: "AnyCodable unsupported")
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.singleValueContainer()
        switch self {
        case .string(let v): try c.encode(v)
        case .int(let v): try c.encode(v)
        case .double(let v): try c.encode(v)
        case .bool(let v): try c.encode(v)
        case .array(let v): try c.encode(v)
        case .object(let v): try c.encode(v)
        case .null: try c.encodeNil()
        }
    }

    var stringValue: String? { if case .string(let v) = self { return v } else { return nil } }
    var intValue: Int? {
        if case .int(let v) = self { return v }
        if case .double(let v) = self { return Int(v) }
        return nil
    }
    var boolValue: Bool? { if case .bool(let v) = self { return v } else { return nil } }
}
