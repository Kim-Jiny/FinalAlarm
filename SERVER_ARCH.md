# FinalAlarm — 서버 아키텍처

## 배포 환경

**별도 git 레포**로 운영. coachDesk와 **같은 호스트**에 각자 docker-compose로 독립 실행. coachDesk PostgreSQL은 외부 네트워크 공유로 접근.

```
┌────────────────────────────────────────────────────────┐
│ Host (existing)                                        │
│                                                        │
│  ┌──────────────────────┐  ┌────────────────────────┐  │
│  │ coachDesk            │  │ finalalarm             │  │
│  │ (자체 compose)       │  │ (자체 compose)         │  │
│  │ ┌──────────────────┐ │  │ ┌────────────────────┐ │  │
│  │ │ coachdesk-api    │ │  │ │ finalalarm-api     │ │  │
│  │ └──────────────────┘ │  │ └────────────────────┘ │  │
│  │ ┌──────────────────┐ │  │            │           │  │
│  │ │ postgres ←───────┼─┼──┼────────────┘           │  │
│  │ └──────────────────┘ │  │  (외부 네트워크 공유:   │  │
│  │ network:             │  │   coachdesk_default)   │  │
│  │  coachdesk_default   │  │                        │  │
│  └──────────────────────┘  └────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### 네트워크 연결

finalalarm-api 컨테이너는 두 네트워크에 join:
- `coachdesk_default` (external) — postgres 접근
- `default` (finalalarm 내부) — 자체용

`docker-compose.yml`의 `networks.coachdesk_net.name`을 호스트의 실제 네트워크 이름에 맞춰야 함 (`docker network ls`로 확인).

## 컨테이너 구성

| 컨테이너 | 역할 |
|---------|------|
| `finalalarm-api` | REST API 서버. 백그라운드 워커 없음 (모두 동기 처리). |

## 스택

- **Node.js + NestJS + Prisma + PostgreSQL** (coachDesk 스택과 일치)
- 마이그레이션: Prisma migrate. coachDesk와 **물리적으로 분리**된 마이그레이션 경로.
- 테이블 prefix `alarm_*`로 격리.

## FCM 발송

- **Firebase Admin SDK** 동기 호출 (큐 없음)
- 발송 함수에 **자동 재시도** (1s → 3s 백오프, 총 3회)
- 영구 실패 (`registration-token-not-registered` 등)는 토큰 즉시 무효화
- 본인이 등록한 알람은 클라이언트 `AlarmManager`로 발사 → FCM 발송은 다음 케이스에만:
  - 팀원에게 push-alarm 발사
  - 잠금해제 요청 (팀원 인박스)
  - 승인 알림 (요청자)

## 워커 / 큐 — 없음

원래 알람 스케줄러, 만료 처리 등을 워커로 두려 했으나 전부 다른 방식으로 처리:

| 원래 워커 역할 | 대체 |
|---------------|------|
| 매 분 알람 스캔 후 FCM 발사 | 클라이언트 로컬 `AlarmManager` |
| 5분 unlock_request 만료 | **Lazy expiration** — 조회·승인 시 `expires_at > now()` 필터 |
| 스누즈 깨우기 | 클라가 `next_ring_at`에 `AlarmManager` 재등록 |
| 6개월 히스토리 정리 | admin 페이지의 수동 정리 버튼 (차후) |

→ Redis도 BullMQ도 없음. 단일 프로세스, 단일 컨테이너.

## 클라이언트 ↔ 서버 이벤트 흐름

**본인 알람 (PERSONAL/TEAM_APPROVAL) 로컬 발사 시**:
```
AlarmManager 발사 → ForegroundService 시작 (foreground 즉시)
                 → 동시에 코루틴으로 POST /alarm-events { definitionId }
                 → 서버가 alarm_events 생성 → event id 반환
                 → RingingActivity가 event id로 snooze/dismiss/unlock-request 호출
```

오프라인 실패 시 클라가 로컬 UUID 생성 후 진행. PERSONAL은 그래도 끄기 가능, TEAM_APPROVAL은 다음 온라인 시 reconcile (MVP 미구현).

**팀원이 push-alarm 발사 시**:
```
POST /push-alarm → 서버가 window 검증 → alarm_events 생성 → FCM ALARM_RING 발송
                                                          → 응답 (sync)
대상 폰: MessagingService → ForegroundService (eventId 이미 채워짐) → RingingActivity
```

## 인증 / 보안

- **JWT 액세스(1h) + refresh(30d)**, refresh는 DB에 저장하고 logout/탈퇴 시 revoke
- 비밀번호: argon2id
- HTTPS 강제 (역방향 프록시 nginx/Caddy 가정)
- Rate limiting: API_SPEC.md §15 참조

## 관측 (Observability)

- 로그: JSON 라인, stdout → 기존 로그 파이프라인 합류
- 메트릭/트레이싱: 필요 시 추후 도입

## 환경 변수

```
DATABASE_URL=postgres://...
JWT_ACCESS_SECRET=...
JWT_REFRESH_SECRET=...
FCM_SERVICE_ACCOUNT_PATH=/run/secrets/fcm-service-account.json
INVITE_LINK_BASE=https://finalalarm.app/i/
PORT=3000
```

## 결정 사항

- ✅ DB 공유 + 테이블 prefix 분리
- ✅ Node.js + NestJS + Prisma + PostgreSQL
- ✅ 워커 / 큐 / Redis 사용 안 함
- ✅ FCM은 동기 호출 + 자동 재시도
- ✅ unlock_request 만료는 lazy expiration
- ✅ 6개월 히스토리 정리는 admin 페이지에서 수동
- 🟡 OAuth (Google/Apple) — MVP는 이메일/비번, 추후
- 🟡 파일 저장소 (사용자 알람음, 미션 사진) — 추후
