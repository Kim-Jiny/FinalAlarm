# iOS 설정 가이드

코드 작성·빌드 통과까지 모두 완료. `xcodebuild` → **BUILD SUCCEEDED** 확인됨.

## 자동 완료된 것

- Info.plist 등록 (`Finalalarm/Info.plist`)
  - 카메라/모션/로컬 네트워크/알림 권한 사용 문구
  - `UIBackgroundModes` = `audio`, `remote-notification`
  - URL Scheme `finalalarm` (초대 딥링크)
  - ATS `NSAllowsLocalNetworking` (디버그용 LAN 서버 허용)
- Entitlements 파일 작성 (`Finalalarm.entitlements`) — Push Notifications용
- `project.pbxproj`에 `INFOPLIST_FILE` / `GENERATE_INFOPLIST_FILE = NO` 적용

## Xcode UI에서 직접 해야 하는 것

### 1. Push Notifications 활성화 (FCM/팀 알람 받으려면 필수)

Apple Developer Portal의 App ID에 Push capability를 켜야 자동 서명 프로비저닝 프로파일이 갱신됩니다. **반드시 Xcode UI에서**:

1. Xcode → `Finalalarm` 타겟 → **Signing & Capabilities**
2. `+ Capability` → **Push Notifications** 추가
3. Xcode가 자동으로 `Finalalarm.entitlements` 파일을 인식·연결해줌 (이미 작성돼 있음)

> 직접 entitlements + aps-environment를 pbxproj에 박아넣으면 자동 서명이 프로비저닝 프로파일을 갱신 못 해서 빌드 실패합니다. UI에서 해야 portal 동기화됨.

### 2. Firebase SDK 추가 (FCM 토큰 발급)

1. **File → Add Package Dependencies**
2. URL: `https://github.com/firebase/firebase-ios-sdk`
3. Up to Next Major Version
4. Products: **`FirebaseMessaging`만** 체크 → Add Package

추가 완료되면 `Core/PushService.swift`의 주석 처리된 `// FirebaseApp.configure()` / `// Messaging.messaging()...` 블록을 활성화하세요 (`#if canImport(FirebaseMessaging)` 조건 안).

### 3. GoogleService-Info.plist

1. Firebase Console → 프로젝트 → iOS 앱 추가 (Bundle ID `com.jiny.finalalarm`)
2. `GoogleService-Info.plist` 다운로드
3. Xcode의 `Finalalarm/Finalalarm/` 폴더에 드래그 (Copy items if needed ✓)
4. Apple Developer → Keys → APNs 키 생성 → Firebase Console → Cloud Messaging → APNs Authentication Key 업로드

### 4. 서버 IP

`Core/AppConfig.swift`의 `apiBaseURL`이 `http://172.30.1.22:3500/api/v1/`. 본인 맥 LAN IP로 수정. (`ifconfig | grep "inet "` 으로 확인)

### 5. 알람 사운드 파일 (선택)

`alarm.mp3` 또는 `alarm.caf`를 `Finalalarm/Finalalarm/` 폴더에 드래그하면 `AlarmAudioPlayer`가 자동 픽업.

## 빌드 검증

```bash
cd ios/Finalalarm
xcodebuild -project Finalalarm.xcodeproj -scheme Finalalarm -destination "generic/platform=iOS" build
```

→ `** BUILD SUCCEEDED **`

## SourceKit-LSP 경고에 대해

`Cannot find type 'XXX' in scope` 등은 외부 LSP 인덱싱의 false positive. Xcode 빌드/실행에는 영향 없음 — 위 명령으로 검증됨.
