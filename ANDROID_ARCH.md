# FinalAlarm — Android 아키텍처

## 기술 스택

| 영역 | 선택 | 비고 |
|------|------|------|
| 언어 | Kotlin | |
| UI | Jetpack Compose | Material 3 |
| 네비게이션 | Navigation Compose | type-safe routes (Kotlin 2.0+) |
| 비동기 | Coroutines + Flow | |
| DI | **Hilt** | Compose·WorkManager 연동 표준 |
| 네트워크 | Retrofit + OkHttp + kotlinx.serialization | |
| 로컬 DB | Room | 오프라인 캐시·이벤트 미러 |
| 백그라운드 작업 | WorkManager | 동기화·재시도용 |
| 알람 재생 | **Foreground Service** + ExoPlayer/MediaPlayer | |
| OS 알람 스케줄 | **AlarmManager (`setAlarmClock`)** | 정확성 + 락스크린 표시 |
| 푸시 | Firebase Cloud Messaging (FCM) | |
| 이미지 | Coil | |
| 로깅 | Timber | |
| 빌드 | Gradle Kotlin DSL + Version Catalog (TOML) | |
| min SDK | 26 (Android 8) | FCM·Notification Channels 안정 |
| target SDK | 35 (Android 15) | |

### 빠진 거 메모
- DataStore (Preferences) — JWT 토큰, 사용자 설정 저장
- Encrypted SharedPreferences 또는 EncryptedDataStore — refresh token 등 민감 데이터
- BiometricPrompt — (옵션) 앱 잠금
- Markdown / Lottie — 필요시
- LeakCanary (debug only)

## 모듈 구조

MVP는 **단일 모듈**, 패키지로 분리. 규모 커지면 멀티 모듈 전환.

```
app/
└── src/main/java/app/finalalarm/
    ├── di/                 # Hilt 모듈
    ├── core/
    │   ├── network/        # Retrofit, interceptors, 토큰 갱신
    │   ├── auth/           # 토큰 저장·갱신·로그아웃
    │   ├── push/           # FCM service, payload 라우팅
    │   ├── alarm/          # AlarmManager, ForegroundService, AudioPlayer
    │   ├── time/           # Clock, TimeZone, DaysOfWeekBitmask
    │   └── db/             # Room database, DAOs
    ├── data/
    │   ├── auth/
    │   ├── teams/
    │   ├── alarms/
    │   ├── windows/
    │   ├── events/
    │   ├── missions/
    │   └── unlockrequests/
    ├── domain/             # 유스케이스 (선택, MVP는 얇게)
    ├── ui/
    │   ├── theme/
    │   ├── components/     # 공통 컴포저블
    │   ├── auth/           # 로그인/회원가입 화면
    │   ├── home/           # 알람 목록
    │   ├── teams/          # 팀 화면들
    │   ├── alarms/         # 알람 편집/생성
    │   ├── missions/       # 미션 프로필 관리
    │   ├── ringing/        # 알람 울리는 화면 (Full-screen)
    │   ├── unlock/         # 잠금해제 요청·승인 화면
    │   └── settings/
    └── MainActivity.kt
```

## 핵심 컴포넌트

### AlarmScheduler (`core/alarm/`)
- 서버에서 받은 `alarm_definitions`를 로컬 `AlarmManager`에도 등록
- 이유: FCM 미수신/지연 대비 백업. 일반 알람은 로컬만으로도 동작.
- API: `setAlarmClock(triggerAt, showIntent, alarmIntent)` — 락스크린에 다음 알람 시각 노출

### AlarmReceiver (`core/alarm/`)
- `AlarmManager`에서 호출되는 BroadcastReceiver
- 받으면 즉시 ForegroundService 시작

### AlarmForegroundService (`core/alarm/`)
- foregroundServiceType: `mediaPlayback|specialUse` (Android 14+)
- ExoPlayer로 알람음 재생, 볼륨 ramp 적용
- VibrationEffect로 진동
- WakeLock (short) — 화면 켜기
- Full-screen Intent로 `RingingActivity` 띄움
- 다른 이벤트 도착(스누즈/끄기)되면 self stop

### RingingActivity (`ui/ringing/`)
- showWhenLocked + turnScreenOn flag
- 알람 정보 + [스누즈] / [끄기 시도] / [팀원 요청] 버튼
- 미션 실행 → 성공 시 dismiss API 호출

### FinalAlarmMessagingService (`core/push/`)
- `FirebaseMessagingService` 상속
- `onMessageReceived` → payload `type` 에 따라 분기:
  - `ALARM_RING` → AlarmReceiver와 같은 경로로 진입 (alarm event 즉시 시작)
  - `UNLOCK_REQUEST` → 일반 알림 표시
  - `UNLOCK_APPROVED` → RingingActivity가 떠 있으면 미션 활성화, 없으면 상태 갱신
- `onNewToken` → `POST /push-tokens`

### TokenAuthenticator (`core/network/`)
- OkHttp Authenticator로 401 만나면 refresh → 재시도
- 동시 갱신은 mutex로 1회만

### Repository 패턴 (`data/`)
- 각 도메인별 Repository
- 네트워크 우선, Room으로 캐시 + 오프라인 지원

## 권한 / Manifest

```xml
<!-- 알람 핵심 -->
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />  <!-- 재부팅 후 알람 복구 -->

<!-- 미션 (사진/QR) -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- 흔들기 — 별도 권한 불필요 (SensorManager) -->
```

## 배터리 최적화 우회 UI

설정 → 배터리 → 제한 없음으로 유도하는 가이드 화면. 첫 실행 시 + 알람 실패 감지 시 다시 안내.

- 샤오미(MIUI), 삼성(One UI), Huawei, 오포 등 제조사별 설정 경로 다름
- 라이브러리 후보: `com.github.judemanutd:autostarter` 또는 직접 인텐트 분기

## CI / 품질

- ktlint or detekt
- ./gradlew test (Unit)
- ./gradlew connectedAndroidTest (Instrumentation)
- GitHub Actions: lint + unit test on PR

## 빌드 변형

- `debug` — 로컬 서버 URL, LeakCanary on
- `release` — 운영 URL, Proguard/R8 on, FCM 운영 키

## 미정

- Kotlin Multiplatform (KMP) 도입 — 서버를 Kotlin으로 갈 경우 도메인/모델 공유 검토
- Compose Navigation 타입 안정성 ↔ Voyager / Decompose 선호 비교 (MVP는 Nav-Compose)
- Room ↔ SQLDelight (MVP는 Room)
