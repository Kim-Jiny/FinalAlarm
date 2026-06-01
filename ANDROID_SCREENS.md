# FinalAlarm — Android 화면 / 플로우 설계

## 화면 목록

### Auth
- **LoginScreen** — 이메일/비번
- **SignupScreen** — 이메일/비번/이름/타임존(자동 감지)
- **OnboardingScreen** — 권한 요청 (알림, 정확한 알람, 배터리 최적화 해제)

### Main (Bottom Nav, 4탭)
- **Tab 1: Home** — 오늘 다가올 알람, 활성 이벤트, 인박스 요약
- **Tab 2: Alarms** — 내 알람 + (팀 탭 토글) 팀 공유 알람
- **Tab 3: Teams** — 내 팀 목록
- **Tab 4: Settings**

### Alarm
- **AlarmListScreen** — kind 필터 (전체/팀승인/일반), 팀 필터
- **AlarmEditScreen** — 생성/수정 통합
  - 입력: kind, team, label, schedule(반복/일회성), 사운드, 진동, 볼륨, 스누즈, 미션
- **AlarmDetailScreen** — 조회 (공유받은 알람은 readonly)

### Mission
- **MissionListScreen** — 내 미션 프로필 목록
- **MissionEditScreen** — type 선택 → type별 설정 UI
- 타입별 sub-UI:
  - **MathMissionConfig** — 난이도, 문제 수
  - **PhotoMissionConfig** — 모드 선택 (사물 사진 / QR / 바코드), 등록
  - **ShakeMissionConfig** — 횟수, 강도

### Team
- **TeamListScreen** — 내 팀 + [팀 만들기] + [코드로 가입]
- **TeamDetailScreen** — 멤버 / 공유 알람 / 시간대 / 초대 관리
- **TeamCreateScreen** — 이름
- **JoinTeamScreen** — 코드 입력 (또는 딥링크 진입)
- **TeamInviteScreen** — 코드/링크 생성 + 공유 시트 (만료·횟수 옵션)

### Alarm Window
- **WindowListScreen** — 팀 단위로 그룹, 활성/비활성 토글
- **WindowEditScreen** — 팀 선택, 시간 범위, 요일

### Push Alarm
- **PushAlarmScreen** — 팀 멤버 선택 → (active window 있는 사람만 활성화) → 라벨 입력 → 발사
- **PushAlarmTargetPickerScreen** — 팀 멤버 + 그 사람의 현재/가까운 시간대 표시

### Ringing (전용 Activity, 풀스크린)
- **RingingActivity** (Compose 안에서)
  - 알람 정보 (라벨, 발사자 이름·아바타, 알람 종류)
  - 스누즈 버튼 (스누즈 가능할 때만)
  - 끄기 흐름:
    - PERSONAL → 즉시 미션 화면
    - TEAM_APPROVAL → [팀원에게 잠금해제 요청] → 대기 화면 → 승인 받으면 미션 화면
  - 미션 화면: type별 컴포넌트 임베드

### Unlock requests
- **InboxScreen** — 들어온 대기 요청 목록 (만료 카운트다운)
- **UnlockRequestDetailScreen** — 요청자 정보, 알람 정보, [승인] 버튼
- **MyRequestStatusBanner** — 내 요청 대기 중일 때 다른 화면 상단 띄움

### Settings
- **SettingsScreen** — 계정, 타임존, 알림 권한, 배터리 최적화, 로그아웃, 탈퇴, 약관·개인정보

---

## Navigation Graph (개략)

```
RootNav
├── Auth (start)
│   ├── Login
│   ├── Signup
│   └── Onboarding (권한)
├── Main (BottomNav 4탭)
│   ├── Home
│   ├── Alarms
│   │   ├── AlarmList
│   │   ├── AlarmDetail
│   │   └── AlarmEdit
│   ├── Teams
│   │   ├── TeamList
│   │   ├── TeamDetail
│   │   │   ├── (멤버 탭)
│   │   │   ├── (공유 알람 탭)
│   │   │   └── (시간대 탭)
│   │   ├── TeamCreate
│   │   ├── JoinTeam
│   │   └── TeamInvite
│   └── Settings
├── Missions
│   ├── MissionList
│   └── MissionEdit
├── Windows
│   ├── WindowList
│   └── WindowEdit
├── PushAlarm
│   ├── TargetPicker
│   └── Compose (라벨)
├── Inbox
│   ├── List
│   └── RequestDetail
└── Ringing (별도 Activity, FLAG_ACTIVITY_NEW_TASK + showWhenLocked)
```

---

## 핵심 사용자 플로우

### 1. 신규 가입 → 첫 알람
```
Signup → Onboarding(권한) → Home(빈 상태)
  → "팀 만들기" → TeamCreate → TeamDetail
  → "초대" → TeamInvite → 링크 공유
  → "알람 추가" → AlarmEdit
    → 미션 미설정이면 → MissionEdit (수학 etc) → 저장 → 알람에 미션 선택
  → 알람 저장 → AlarmList
```

### 2. 일반 알람 끄기
```
[알람 시간 도달]
  → AlarmManager (또는 FCM) → ForegroundService 시작
  → RingingActivity (full-screen) 표시
  → 사용자 [끄기 시도] 탭
  → MissionScreen (type별) → 성공
  → POST /alarm-events/:id/dismiss
  → 알람 종료
```

### 3. 팀원 승인 알람 끄기
```
[알람 도달] → Ringing
  → 사용자 [팀원에게 잠금해제 요청]
  → POST /alarm-events/:id/unlock-request
  → 대기 화면 (5분 카운트다운, 알람 계속 울림)
  → 팀원 측: FCM(UNLOCK_REQUEST) → Inbox 알림
  → 팀원 [승인] → POST /unlock-requests/:id/approve
  → 본인 측: FCM(UNLOCK_APPROVED) → Ringing 화면이 [미션 시작] 활성화
  → MissionScreen → 성공 → dismiss → 종료

[만료 시나리오]
  → 5분 안에 응답 없음 → request EXPIRED
  → Ringing 화면 상태 갱신: [다시 요청] 활성
```

### 4. 팀원에게 알람 발사
```
사용자 A → PushAlarm → TargetPicker → 팀 B 선택
  → 팀원 목록 (그 시간 active window 있는 사람만 활성)
  → 사용자 C 선택 (07:00-09:00 window 있음, 현재 07:30) → [발사]
  → POST /push-alarm
  → 서버: window 검증 → event 생성 → FCM(ALARM_RING) to C
  → C 기기: ForegroundService → RingingActivity (sender 이름 표시)
  → C가 위 #2 또는 #3 흐름으로 끔
```

### 5. 스누즈
```
Ringing → [스누즈] 탭 (snooze_count < max일 때만 보임)
  → POST /alarm-events/:id/snooze
  → 서버: state=SNOOZED, next_ring_at = now + snooze_minutes
  → 로컬: ForegroundService 종료, AlarmManager로 next_ring_at에 다시 등록
  → next_ring_at 도달 → RINGING 복귀
```

### 6. 재부팅 후 복구
```
BOOT_COMPLETED Broadcast
  → 서버 동기화: GET /alarms (active만)
  → 로컬 AlarmManager에 전부 재등록
  → 진행 중이던 alarm_events (RINGING/SNOOZED) 있으면 복구
```

---

## 상태 관리

- 화면별 **ViewModel** + StateFlow
- 글로벌: 
  - `AuthStateHolder` (로그인 여부, 토큰)
  - `ActiveEventStateHolder` (현재 진행 중인 alarm event — Ringing 화면 띄움 트리거)
  - `InboxBadgeState` (대기 중 요청 개수 — 탭 배지)

## 알림 채널 (Notification Channels)

| 채널 | 중요도 | 용도 |
|------|--------|------|
| `alarm` | HIGH (lockscreen + sound bypass DND) | 알람 본체 (사용 안 함, 실제 알람은 ForegroundService) |
| `alarm_fg` | LOW (silent) | ForegroundService 표시용 |
| `unlock_request` | HIGH | 팀에 들어온 요청 |
| `unlock_approved` | DEFAULT | 본인 요청 승인됨 |
| `system` | DEFAULT | 기타 |

## 위젯 / 단축

- 잠금화면에 "다음 알람" 표시 (`AlarmManager.setAlarmClock` info)
- 홈 위젯 — MVP 이후

## 미정

- 다크/라이트/시스템 테마 토글 위치 (Settings)
- 알람 화면 디자인 디테일 — 디자이너 합류 시점에
- 다국어 — 한국어 우선, en은 차후
