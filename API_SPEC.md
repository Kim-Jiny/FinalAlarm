# FinalAlarm — 서버 API 명세

REST + JSON. 모든 시각은 ISO 8601 (`2026-05-28T07:30:00+09:00`). 모든 ID는 UUID. base path: `/api/v1`.

## 인증

- **JWT bearer 토큰** (`Authorization: Bearer <token>`)
- 토큰 만료: access 1시간, refresh 30일
- 인증 안 해도 되는 엔드포인트: `POST /auth/*`, `GET /team-invites/:code/preview`

## 에러 응답 (공통)

```json
{ "error": { "code": "TEAM_NOT_MEMBER", "message": "..." } }
```

주요 코드: `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `VALIDATION_ERROR`, `TEAM_NOT_MEMBER`, `INVITE_EXPIRED`, `WINDOW_NOT_ACTIVE`, `REQUEST_EXPIRED`, `RATE_LIMITED`.

---

## 1. Auth

| Method | Path | 설명 |
|--------|------|------|
| POST | `/auth/signup` | 이메일/비번 회원가입 → access+refresh |
| POST | `/auth/login` | 로그인 → access+refresh |
| POST | `/auth/refresh` | refresh로 access 재발급 |
| POST | `/auth/logout` | refresh 토큰 무효화 |

```json
// POST /auth/signup
{ "email": "...", "password": "...", "displayName": "...", "timezone": "Asia/Seoul" }

// 200
{ "user": { ... }, "accessToken": "...", "refreshToken": "..." }
```

## 2. Me

| Method | Path | 설명 |
|--------|------|------|
| GET | `/me` | 내 정보 |
| PATCH | `/me` | displayName, avatarUrl, timezone 수정 |
| DELETE | `/me` | 탈퇴 (계정 + 관련 데이터 삭제) |

## 3. Push tokens

| Method | Path | 설명 |
|--------|------|------|
| POST | `/push-tokens` | 토큰 등록/갱신 (deviceId 기준 upsert) |
| DELETE | `/push-tokens/:id` | 로그아웃 시 해제 |

```json
// POST /push-tokens
{ "platform": "ANDROID", "token": "...", "deviceId": "..." }
```

## 4. Teams

| Method | Path | 설명 |
|--------|------|------|
| GET | `/teams` | 내가 속한 팀 목록 |
| POST | `/teams` | 팀 생성 (요청자가 OWNER) |
| GET | `/teams/:id` | 팀 상세 + 멤버 목록 |
| PATCH | `/teams/:id` | 이름 수정 (OWNER/ADMIN) |
| DELETE | `/teams/:id` | 팀 삭제 (OWNER) |
| DELETE | `/teams/:id/members/me` | 탈퇴 |
| DELETE | `/teams/:id/members/:userId` | 강퇴 (OWNER/ADMIN) |
| PATCH | `/teams/:id/members/:userId` | role 변경 (OWNER) |

## 5. Team invites

| Method | Path | 설명 |
|--------|------|------|
| GET | `/teams/:id/invites` | 활성 초대 목록 |
| POST | `/teams/:id/invites` | 초대 생성 → `{ code, url, expiresAt }` |
| DELETE | `/teams/:id/invites/:inviteId` | revoke |
| GET | `/team-invites/:code/preview` | **비인증** — 팀 이름·멤버수 노출 (가입 전 미리보기) |
| POST | `/team-invites/:code/redeem` | 코드 입력 또는 딥링크 탭으로 가입 |

```json
// POST /teams/:id/invites
{ "expiresInDays": 7, "maxUses": null }
// 200
{ "id": "...", "code": "AB12-CD34", "url": "https://finalalarm.app/i/AB12CD34", "expiresAt": "..." }
```

## 6. User missions

| Method | Path | 설명 |
|--------|------|------|
| GET | `/missions` | 내 미션 프로필 목록 |
| POST | `/missions` | 미션 생성 |
| GET | `/missions/:id` | |
| PATCH | `/missions/:id` | |
| DELETE | `/missions/:id` | (참조 중인 알람이 있으면 409) |

```json
// POST /missions
{ "type": "MATH", "name": "아침용", "config": { "difficulty": "medium", "questionCount": 3 }, "isDefault": true }
```

## 7. Alarm definitions

| Method | Path | 설명 |
|--------|------|------|
| GET | `/alarms` | 내 알람 + 내가 속한 팀의 TEAM_APPROVAL 알람 |
| POST | `/alarms` | 알람 생성 |
| GET | `/alarms/:id` | (가시성 정책 적용) |
| PATCH | `/alarms/:id` | owner만 |
| DELETE | `/alarms/:id` | owner만 |

### Query params on GET /alarms
- `teamId` — 특정 팀의 공유 알람만
- `kind=TEAM_APPROVAL|PERSONAL`
- `active=true|false`

```json
// POST /alarms
{
  "kind": "TEAM_APPROVAL",
  "teamId": "...",                  // PERSONAL이면 null
  "label": "기상",
  "timezone": "Asia/Seoul",
  "scheduleType": "RECURRING",      // or "ONE_SHOT"
  "oneShotAt": null,                // ONE_SHOT일 때 ISO timestamp
  "timeOfDay": "07:30",
  "daysOfWeek": 31,                 // bitmask 월-금
  "soundUri": "system:default",
  "volume": 80,
  "volumeRampSeconds": 30,
  "vibrationEnabled": true,
  "vibrationPattern": "PULSE",
  "snoozeEnabled": true,
  "snoozeMinutes": 5,
  "snoozeMaxCount": 3,
  "missionId": "..."
}
```

## 8. Alarm windows

| Method | Path | 설명 |
|--------|------|------|
| GET | `/alarm-windows` | `?userId=me`(기본) 또는 `?teamId=xx` (팀원 공유) |
| POST | `/alarm-windows` | 시간대 생성 (여러 개 가능, 겹쳐도 별개) |
| PATCH | `/alarm-windows/:id` | owner만 |
| DELETE | `/alarm-windows/:id` | owner만 |

```json
// POST /alarm-windows
{
  "teamId": "...",
  "startTime": "06:00",
  "endTime": "09:00",
  "daysOfWeek": 31,
  "timezone": "Asia/Seoul"
}
```

## 9. Push alarm (시간대 내 팀원에게 알람 발사)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/push-alarm` | 발사 |

```json
// POST /push-alarm
{ "targetUserId": "...", "teamId": "...", "label": "일어나!" }
```

서버 처리:
1. 요청자가 `teamId`의 멤버인지 확인
2. `targetUserId`의 active `alarm_windows` 중 현재 시각이 포함되고 `team_id` 일치하는 것 검색
3. 없으면 `WINDOW_NOT_ACTIVE` 에러
4. 있으면 `alarm_events` insert (`sender_user_id`, `window_id`, `team_id` 채움)
5. 대상자에게 FCM (`ALARM_RING`) 발송

## 10. Alarm events

| Method | Path | 설명 |
|--------|------|------|
| GET | `/alarm-events?state=RINGING,SNOOZED,UNLOCK_REQUESTED` | 진행 중인 내 이벤트 (앱 부팅 시 복구용) |
| POST | `/alarm-events` | **클라 로컬 알람 발사 보고** — `definitionId` 기반으로 event 생성 |
| GET | `/alarm-events/:id` | |
| GET | `/alarm-events/history?from=&to=&limit=` | 히스토리 조회 (최대 6개월) |
| POST | `/alarm-events/:id/snooze` | 스누즈 |
| POST | `/alarm-events/:id/dismiss` | 끄기 (미션 완료 증빙 동봉) |
| POST | `/alarm-events/:id/unlock-request` | 잠금해제 요청 생성 (TEAM_APPROVAL만) |

```json
// POST /alarm-events
{ "definitionId": "...", "triggeredAt": "2026-06-01T07:30:00+09:00" }
// 200
{ "id": "...", "state": "RINGING", ... }
```

```json
// POST /alarm-events/:id/dismiss
{ "missionProof": { "type": "MATH", "answers": [...] } }
// or { "type": "PHOTO", "imageUrl": "..." }
// or { "type": "SHAKE", "shakeCount": 32 }
```

서버는 mission_proof를 검증 → 통과 시 state DISMISSED.
TEAM_APPROVAL인데 아직 UNLOCK_APPROVED 아니면 403.

## 11. Unlock requests

| Method | Path | 설명 |
|--------|------|------|
| GET | `/unlock-requests/inbox?teamId=xx&status=PENDING` | 팀에 들어온 대기 요청 |
| GET | `/unlock-requests/:id` | |
| POST | `/unlock-requests/:id/approve` | 팀원 승인 |
| POST | `/unlock-requests/:id/cancel` | 요청자 본인 취소 |

승인 시 서버:
1. 요청 `status=PENDING`인지, 만료 안 됐는지 확인
2. 승인자가 같은 팀 멤버인지 확인 (단, requester 본인 ≠ approver)
3. `unlock_request.status = APPROVED`, `approved_by/approved_at` set
4. 연결된 `alarm_event.state = UNLOCK_APPROVED`
5. 요청자에게 FCM (`UNLOCK_APPROVED`) 발송

## 12. FCM payload 정의

모든 푸시는 **data message** (high-priority, `priority: "high"`, `content_available: true`).

### ALARM_RING (수신자: 알람 대상자)
```json
{
  "type": "ALARM_RING",
  "eventId": "...",
  "alarmKind": "TEAM_APPROVAL" | "PERSONAL",
  "senderUserId": "...",        // PERSONAL이면 null
  "senderDisplayName": "...",
  "label": "기상",
  "missionType": "MATH",
  "soundUri": "system:default",
  "volume": 80,
  "volumeRampSeconds": 30,
  "vibrationEnabled": true,
  "vibrationPattern": "PULSE",
  "snoozeEnabled": true,
  "snoozeMinutes": 5,
  "snoozeRemaining": 3,
  "triggeredAt": "2026-05-28T07:30:00+09:00"
}
```
→ 안드로이드는 이걸 받고 Foreground Service 시작 → 알람 화면 띄움.

### UNLOCK_REQUEST (수신자: 같은 팀 멤버들)
```json
{
  "type": "UNLOCK_REQUEST",
  "requestId": "...",
  "eventId": "...",
  "requesterUserId": "...",
  "requesterDisplayName": "...",
  "teamId": "...",
  "expiresAt": "..."
}
```
→ 일반 알림. 탭하면 잠금해제 화면.

### UNLOCK_APPROVED (수신자: 요청자)
```json
{
  "type": "UNLOCK_APPROVED",
  "requestId": "...",
  "eventId": "...",
  "approvedByUserId": "...",
  "approvedByDisplayName": "..."
}
```
→ 알람 화면 state 갱신 (미션 활성화).

### TEAM_INVITED (옵션 — 나중에)
초대 받았을 때 사용.

---

## 13. 권한·도메인 검증 요약

| 액션 | 검증 |
|------|------|
| `POST /push-alarm` | 요청자 ∈ team, target의 active window 시간대 일치 |
| `POST /unlock-requests/:id/approve` | 승인자 ∈ event.team, 승인자 ≠ requester, status=PENDING, not expired |
| `POST /alarm-events/:id/snooze` | 호출자 = event.target_user_id, state ∈ {RINGING}, snooze_count < max |
| `POST /alarm-events/:id/dismiss` | 호출자 = target, PERSONAL이면 RINGING/SNOOZED, TEAM_APPROVAL이면 UNLOCK_APPROVED |
| `GET /alarms/:id` | owner이거나 (kind=TEAM_APPROVAL ∧ 호출자 ∈ team) |

## 14. 백그라운드 워커 — 없음

별도 워커 프로세스/큐 없이 동기 처리 + 클라이언트 책임으로 분담:

- **알람 발사**: 클라 `AlarmManager` → `POST /alarm-events`로 발사 보고
- **잠금해제 5분 만료**: Lazy expiration — `inbox` 조회·`approve`·재요청 시 `expires_at > now()` 필터
- **스누즈 깨우기**: 클라가 snooze 응답의 `next_ring_at`에 로컬 `AlarmManager` 재등록
- **히스토리 6개월 정리**: admin 페이지의 수동 정리 (차후)
- **FCM 발송**: API 핸들러에서 동기 호출 + 자동 재시도 (1s/3s, 총 3회)

## 15. Rate limiting (개략)

- `POST /push-alarm` — target+sender 조합당 분당 1회
- `POST /alarm-events/:id/unlock-request` — event당 분당 1회 (스팸 방지)
- `POST /auth/login` — IP+email당 분당 5회

---

## 16. 미정

- 알람 사운드 사용자 업로드 시 파일 저장소 (S3? local?) — 서버 구현 단계에서 결정
- 사진 미션 인증 방식 (서버 비교 vs 클라 비교) — 사진 미션 구현 시 결정
- 익명/공개 푸시 발송자 옵션 미구현 (현재 정책: 항상 공개)
