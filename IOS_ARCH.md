# iOS 아키텍처

SwiftUI + Observation, 단일 모듈, MVVM-lite. Android와 화면 매핑은 1:1.

## 핵심 결정

- **Min iOS**: 17.0 — `@Observable`, `Observation` 프레임워크, `LabeledContent`, `ContentUnavailableView` 등 활용
- **언어/UI**: Swift 5.10+, SwiftUI 전부. UIKit은 `UIViewControllerRepresentable`로 카메라 등 한정
- **Bundle ID**: `com.jiny.finalalarm`
- **푸시**: Firebase iOS SDK + APNs → FCM 토큰을 서버에 등록 (Android와 동일 토픽)
- **Critical Alerts 미사용**: 백업 노티 + 포그라운드 사운드 루프로 "Alarmy 스타일" 끝까지 울림
- **로컬 알림**: `UNNotificationRequest` + `UNCalendarNotificationTrigger`로 시각 매칭 등록. 사용자가 알람을 켜둔 정확한 시각에 노티 발사 → 사용자가 노티 탭 OR 앱 포그라운드면 `RingingView` 호출
- **사운드**: `AVAudioSession` `.playback` + `mixWithOthers` 끄기. 앱이 활성화되면 풀스크린 `RingingView`에서 `AVAudioPlayer`로 사운드 루프

## 폴더 구조

```
ios/
  FinalAlarm.xcodeproj
  FinalAlarm/
    FinalAlarmApp.swift            # @main
    Info.plist
    Assets.xcassets
    GoogleService-Info.plist       # Firebase
    Core/
      Network/
        APIClient.swift            # URLSession + async/await
        APIError.swift
        Endpoints.swift            # 서버 엔드포인트 enum
      Auth/
        TokenStore.swift           # Keychain wrapper (KeychainAccess or 직접 SecItem)
        AuthInterceptor.swift      # 401 → refresh
      Models/
        Dto.swift                  # Codable structs (Android DTO 거울)
      Alarm/
        AlarmScheduler.swift       # UNUserNotificationCenter wrap
        AudioPlayer.swift          # AVAudioPlayer 루프
      Push/
        PushService.swift          # FCM 등록 + 토큰 서버 전송
      Util/
        Haptics.swift, Logger.swift
    Data/
      Repositories/                # AuthRepository, AlarmRepository, ...
    UI/
      Theme/
        FA.swift                   # Color/Spacing/Typography
        FAComponents.swift         # PrimaryButton, FaTextField, ListRow, ...
      Auth/
        LoginView.swift
        SignupView.swift
        OnboardingView.swift
      Root/
        RootView.swift             # 로그인 상태 분기
        MainTabView.swift          # Home/Alarms/Teams/Settings
      Home/
        HomeView.swift
      Alarms/
        AlarmListView.swift
        AlarmEditView.swift
      Teams/
        TeamsView.swift
        TeamDetailView.swift
        TeamCreateView.swift
        TeamInviteView.swift
        JoinTeamView.swift
      Windows/
        WindowListView.swift
        WindowEditView.swift
      Missions/
        MissionListView.swift
        MissionEditView.swift
        Runners/
          MathRunner.swift
          BarcodeRunner.swift       # AVCaptureSession + Vision (VNDetectBarcodesRequest)
          ShakeRunner.swift         # CoreMotion accelerometer
      Inbox/
        InboxListView.swift
        UnlockRequestDetailView.swift
      PushAlarm/
        PushAlarmView.swift
      History/
        HistoryView.swift
      Ringing/
        RingingView.swift
      Settings/
        SettingsView.swift
```

## 의존성 (Swift Package Manager)

- `firebase-ios-sdk` — Messaging만
- (선택) `KeychainAccess` — 토큰 저장 단순화
- 시스템 프레임워크만으로 충분: AVFoundation, CoreMotion, Vision, AuthenticationServices, UserNotifications

## 데이터 흐름 (Android와 동일)

1. `APIClient` 싱글톤 (또는 `@Environment`로 주입) — Authorization 헤더 자동, 401 시 refresh 시도
2. View → ViewModel(`@Observable`) → Repository → APIClient
3. View는 `@State viewModel = SomeVm()` 또는 `@Bindable` 사용
4. 화면 복귀 갱신: `.onAppear { vm.refresh() }` (Android의 `OnResume` 자리)

## 알람 시나리오

- **로컬 알림 스케줄**: 알람 생성/수정/삭제 시 서버 동기화 + `UNUserNotificationCenter.add(request)` 64개 한도 내에서 다가오는 알람 등록
- **백그라운드**: 노티가 뜨면 사용자가 탭 → 딥링크 `finalalarm://alarm/<eventId>` → `RingingView`
- **포그라운드**: 노티 시각 도달 → `UNUserNotificationCenterDelegate.willPresent`에서 풀스크린 시트 띄우고 사운드 시작
- **앱 종료 상태**: 시스템 노티만 가능 (사운드 30초 제한). 사용자 탭 시 앱 부팅 → `RingingView`
- **팀 알람 (TEAM_APPROVAL)**: FCM 수신 시 노티 + 앱 활성화되면 즉시 `RingingView`. 미션 + 잠금해제 요청 흐름은 Android와 동일

## 인증

- 액세스 토큰 메모리 + Keychain
- 리프레시 토큰 Keychain
- `AuthInterceptor`: 모든 요청에 Bearer 첨부, 401 응답 시 refresh 1회 시도 후 재요청
- 로그아웃: Keychain 비우고 `RootView` 분기

## 푸시 (FCM)

- `FirebaseApp.configure()` `@main` `init`에서
- `UIApplicationDelegate` 채택 (`@UIApplicationDelegateAdaptor`)
- APNs 디바이스 토큰 → `Messaging.messaging().apnsToken = token`
- FCM 토큰 받으면 `/me/push-tokens` POST (platform: ios)
- 데이터 메시지 수신 시 `RingingView` 트리거 (TEAM_APPROVAL일 때)

## 화면별 매핑

Android `ANDROID_SCREENS.md`와 1:1. iOS 고유 차이만:

- **Bottom Tab**: `TabView` + `.tabItem` (Material NavigationBar 대신)
- **Top Bar**: `NavigationStack` + `.navigationTitle` / `.toolbar`
- **시각 입력**: `DatePicker(.hourAndMinute)` (Android는 텍스트 필드)
- **요일 칩**: 동일하게 7개 토글 — 직접 그림
- **시트/Dialog**: `.sheet`, `.alert`, `.confirmationDialog`

## 디자인 토큰

Android의 `FA` 컬러 그대로. SwiftUI `Color`로 옮기고 다크모드 대응:

```swift
extension Color {
    static let faPrimary = Color(red: 1.0, green: 0.482, blue: 0.420)        // #FF7B6B
    static let faPrimaryLight = Color(red: 1.0, green: 0.635, blue: 0.541)   // #FFA28A
    static let faBgTop = Color(red: 1.0, green: 0.984, blue: 0.961)          // #FFFBF5
    static let faBgBottom = Color(red: 1.0, green: 0.941, blue: 0.898)       // #FFF0E5
    static let faLabel = Color(red: 0.122, green: 0.102, blue: 0.078)        // #1F1A14
    static let faLabelSecondary = Color(red: 0.420, green: 0.369, blue: 0.329) // #6B5E54
    static let faAccent = Color(red: 0.482, green: 0.357, blue: 0.878)       // #7B5BE0
}
```

`WarmBackground`는 `LinearGradient(colors: [.faBgTop, .faBgBottom], startPoint: .top, endPoint: .bottom)` 적용한 ZStack.

## 진행 순서

1. Xcode 프로젝트 생성 + Firebase 추가
2. 디자인 토큰 + 공통 컴포넌트
3. Network/Auth (토큰, API 클라이언트)
4. Auth/Onboarding 화면
5. 메인 탭 + 알람 CRUD (서버 연동)
6. 미션
7. 알람 발사 (UNNotification + AudioPlayer + RingingView)
8. 팀/초대/시간대/Push alarm
9. FCM 통합
10. Inbox/History/Settings
