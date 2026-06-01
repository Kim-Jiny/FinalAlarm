# FinalAlarm — 데이터 모델

DB는 기존 인스턴스 공유 가정 (PostgreSQL 전제로 작성). 테이블 prefix는 `alarm_` 사용.

## 엔티티 개요

```
users ──┬── alarm_team_members ──── alarm_teams
        │                              │
        ├── alarm_user_missions        ├── alarm_team_invites
        │                              │
        ├── alarm_windows ─────────────┤
        │                              │
        ├── alarm_definitions ─────────┘
        │       │
        │       └── alarm_events ── alarm_unlock_requests
        │
        └── alarm_push_tokens
```

---

## 1. users (기존 테이블 재사용 가능)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | uuid PK | |
| email | text unique | |
| display_name | text | |
| avatar_url | text nullable | |
| timezone | text | IANA TZ (예: Asia/Seoul) |
| created_at, updated_at | timestamptz | |

> coachDesk와 공유 가능. 공유한다면 본 테이블은 건드리지 않고 FK만 참조.

## 2. alarm_teams

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | uuid PK | |
| name | text | |
| created_by | uuid FK users(id) | |
| created_at, updated_at | timestamptz | |

## 3. alarm_team_members

한 사람이 여러 팀에 가입 가능 (다대다).

| 컬럼 | 타입 | 설명 |
|------|------|------|
| team_id | uuid FK alarm_teams(id) | PK |
| user_id | uuid FK users(id) | PK |
| role | enum('OWNER','ADMIN','MEMBER') | |
| joined_at | timestamptz | |

## 4. alarm_team_invites

초대 코드 + 초대 링크 둘 다 같은 row로 처리 (code 값을 링크에 그대로 포함).

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | uuid PK | |
| team_id | uuid FK | |
| code | text unique | 8자리 영숫자 |
| created_by | uuid FK users(id) | |
| expires_at | timestamptz | 기본 7일 |
| max_uses | int nullable | null이면 무제한 |
| use_count | int | |
| revoked_at | timestamptz nullable | |

## 5. alarm_user_missions

사용자가 미리 등록해두는 "끄기 미션" 프로필. 알람마다 다른 미션 지정 가능.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | uuid PK | |
| user_id | uuid FK users(id) | |
| type | enum('MATH','PHOTO','SHAKE') | |
| config | jsonb | 타입별 설정 (난이도/문제수/사진해시/흔들기횟수 등) |
| name | text | 사용자가 붙인 별명 ("아침용 수학" 등) |
| is_default | boolean | 알람 생성 시 기본 선택 |
| created_at, updated_at | timestamptz | |

### config 예시
```jsonc
// MATH
{ "difficulty": "easy|medium|hard", "questionCount": 3 }

// PHOTO (QR/바코드 포함)
{ "mode": "REFERENCE_IMAGE" | "QR" | "BARCODE",
  "referenceImageUrl": "...",   // REFERENCE_IMAGE일 때
  "expectedCode": "..." }       // QR/BARCODE일 때

// SHAKE
{ "shakeCount": 30, "intensity": "medium" }
```

## 6. alarm_definitions

사용자가 미리 만들어두는 알람. "팀원 승인 알람" / "일반 알람" 둘 다 여기로. 기본 알람앱 기능(반복/일회성/사운드/진동/스누즈/볼륨)을 모두 표현.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | uuid PK | |
| owner_id | uuid FK users(id) | |
| team_id | uuid FK alarm_teams(id) nullable | 팀원 승인 알람일 때만 set |
| kind | enum('TEAM_APPROVAL','PERSONAL') | |
| label | text | "기상", "회의 알림" 등 |
| timezone | text | 알람 기준 TZ |
| **— 스케줄 —** | | |
| schedule_type | enum('ONE_SHOT','RECURRING') | |
| one_shot_at | timestamptz nullable | ONE_SHOT일 때만 set |
| time_of_day | time nullable | RECURRING일 때만 set (HH:MM) |
| days_of_week | smallint nullable | RECURRING일 때만 set, 7bit 비트마스크 (월=1, 화=2, …) |
| **— 사운드/진동 —** | | |
| sound_uri | text | 시스템 사운드 URI 또는 사용자 파일 경로 |
| volume | smallint | 0-100 |
| volume_ramp_seconds | int | 0이면 즉시 최대, N이면 N초에 걸쳐 점진 증가 |
| vibration_enabled | boolean | |
| vibration_pattern | enum('SHORT','MEDIUM','LONG','PULSE','HEARTBEAT') | |
| **— 스누즈 —** | | |
| snooze_enabled | boolean | |
| snooze_minutes | smallint | 다시 울리기까지 분 (기본 5) |
| snooze_max_count | smallint | 최대 스누즈 횟수, -1=무제한 (기본 3) |
| **— 기타 —** | | |
| mission_id | uuid FK alarm_user_missions(id) | 끄기 미션 |
| active | boolean | |
| created_at, updated_at | timestamptz | |

### 제약
- `schedule_type='ONE_SHOT'` ⇒ `one_shot_at NOT NULL`, `time_of_day`·`days_of_week` 모두 NULL
- `schedule_type='RECURRING'` ⇒ `time_of_day NOT NULL`, `days_of_week > 0`, `one_shot_at` NULL

> **공유 정책**: `kind=TEAM_APPROVAL`은 같은 팀 멤버 전체 조회 가능. `kind=PERSONAL`은 owner만 조회.

## 7. alarm_windows

"이 시간대 안에는 팀원이 나에게 알람 보낼 수 있음" 정의. 여러 개 가능, 겹쳐도 별개.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | uuid PK | |
| user_id | uuid FK users(id) | 시간대 주인 |
| team_id | uuid FK alarm_teams(id) | 이 팀의 멤버만 발사 가능 |
| start_time | time | HH:MM |
| end_time | time | HH:MM (start보다 작으면 자정 넘김 처리) |
| days_of_week | smallint | 비트마스크 |
| timezone | text | |
| active | boolean | |
| created_at, updated_at | timestamptz | |

## 8. alarm_events

실제로 울린 알람의 인스턴스. 두 가지 출처:
1. `alarm_definitions`의 스케줄로 발생한 알람 (자가 예약)
2. 팀원이 시간대 내에 즉시 발사한 알람 (`definition_id` = null)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | uuid PK | |
| definition_id | uuid FK alarm_definitions(id) nullable | 즉시 발사면 null |
| window_id | uuid FK alarm_windows(id) nullable | 시간대 발사일 때 set |
| target_user_id | uuid FK users(id) | 알람 받는 사람 |
| sender_user_id | uuid FK users(id) nullable | 시간대 발사 시 발사자 (항상 공개) |
| team_id | uuid FK alarm_teams(id) nullable | |
| mission_id | uuid FK alarm_user_missions(id) | 이 인스턴스에 적용된 미션 (스냅샷) |
| state | enum('RINGING','SNOOZED','UNLOCK_REQUESTED','UNLOCK_APPROVED','DISMISSED','EXPIRED') | |
| snooze_count | smallint default 0 | 지금까지 스누즈한 횟수 |
| last_snoozed_at | timestamptz nullable | 마지막 스누즈 시각 |
| next_ring_at | timestamptz nullable | SNOOZED일 때 다음에 울릴 시각 |
| triggered_at | timestamptz | 최초 울린 시각 |
| dismissed_at | timestamptz nullable | |
| created_at | timestamptz | |

### state 전이
```
RINGING ──(스누즈 버튼)────────────> SNOOZED (snooze_count 증가, next_ring_at = now + snooze_minutes)
SNOOZED ──(next_ring_at 도달)──────> RINGING
RINGING ──(팀원 승인 알람: 요청)────> UNLOCK_REQUESTED
RINGING ──(일반 알람: 미션 성공)────> DISMISSED
UNLOCK_REQUESTED ──(팀원 승인)──────> UNLOCK_APPROVED
UNLOCK_APPROVED ──(미션 성공)───────> DISMISSED
UNLOCK_REQUESTED ──(5분 경과)───────> EXPIRED (알람은 계속 울림 → 재요청 가능)
```

### 스누즈 규칙
- 스누즈는 **미션 없이** 누를 수 있음 (일반 알람앱과 동일 UX)
- `snooze_count >= snooze_max_count`면 스누즈 버튼 비활성 (끄기 미션만 남음)
- 팀원 승인 알람도 스누즈 가능. 단, 스누즈 중에 만료된 unlock_request가 있다면 RINGING 복귀 시 새로 요청해야 함

## 9. alarm_unlock_requests

팀원 승인 알람에서만 사용. 5분 만료.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | uuid PK | |
| event_id | uuid FK alarm_events(id) | |
| requester_id | uuid FK users(id) | = event.target_user_id |
| team_id | uuid FK alarm_teams(id) | |
| status | enum('PENDING','APPROVED','EXPIRED','CANCELED') | |
| approved_by | uuid FK users(id) nullable | |
| approved_at | timestamptz nullable | |
| expires_at | timestamptz | created_at + 5분 |
| created_at | timestamptz | |

## 10. alarm_push_tokens

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | uuid PK | |
| user_id | uuid FK users(id) | |
| platform | enum('ANDROID','IOS') | |
| token | text unique | FCM / APNs 토큰 |
| device_id | text | 같은 기기 재로그인 시 갱신용 |
| created_at | timestamptz | |
| last_seen_at | timestamptz | |

---

## 인덱스 (초안)

- `alarm_team_members (user_id)` — 내가 속한 팀 조회
- `alarm_team_invites (code)` — 코드 lookup (unique)
- `alarm_definitions (owner_id, active)` — 내 알람 목록
- `alarm_definitions (team_id, active) where kind='TEAM_APPROVAL'` — 팀 공유 알람
- `alarm_windows (user_id, active)` / `(team_id, active)`
- `alarm_events (target_user_id, state)` — 진행 중인 내 알람
- `alarm_unlock_requests (team_id, status, expires_at) where status='PENDING'` — 팀 대기 요청 lazy expiration 조회 가속
- `alarm_push_tokens (user_id)` — 유저별 발송 토큰

## 도메인 규칙 / 제약

1. `alarm_definitions.kind='TEAM_APPROVAL'`이면 `team_id` 필수, `kind='PERSONAL'`이면 `team_id` null.
2. `alarm_events.sender_user_id`가 set이면 `window_id`도 set (시간대 안에서만 발사 가능).
3. 즉시 발사 알람은 `definition_id` null, sender·window·team_id 모두 set.
4. `alarm_unlock_requests`는 같은 `event_id`에 대해 동시에 한 건만 PENDING (UNIQUE INDEX where status='PENDING').
5. `alarm_unlock_requests.approved_by`는 같은 `team_id`의 멤버여야 함 (앱·서버 검증).

## 데이터 보존 정책

- **자동 삭제 워커 없음.** 알람 히스토리는 무기한 유지.
- 데이터량 누적되면 admin 페이지에서 수동으로 "N개월 이상 된 이벤트 삭제" 버튼 실행.
- `alarm_unlock_requests`는 `alarm_events`에 ON DELETE CASCADE.
- `alarm_unlock_requests.status='EXPIRED'` 컬럼은 lazy expiration이므로 실제로 잘 안 채워짐 (대부분 PENDING으로 남아있고 조회 시 필터). 통계용으로 채우려면 별도 청소 작업 추가.

## 추가 컬럼 후보 (필요 시점에)

- 알람 사진/색상 테마, 라벨 아이콘
- 알람 시작 전 사전 알림 ("10분 뒤 알람 있어요" 같은 미리 알림)
