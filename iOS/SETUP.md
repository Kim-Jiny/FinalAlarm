# iOS 설정 가이드

코드 작성은 모두 끝났습니다. 빌드 통과 확인됨 (`xcodebuild` → BUILD SUCCEEDED).

다만 Xcode UI에서만 가능한 다음 항목을 본인이 직접 해주셔야 실제 기기에서 알람·푸시가 동작합니다.

## 1. 권한·Capabilities (필수)

Xcode → `Finalalarm` 타겟 → **Signing & Capabilities** → `+ Capability` 클릭:

- [ ] **Push Notifications**
- [ ] **Background Modes**
  - [x] Audio, AirPlay, and Picture in Picture (알람 사운드 백그라운드 재생)
  - [x] Remote notifications (팀 알람 FCM)

## 2. Info.plist 키 추가 (필수)

타겟 → **Info** 탭에서 `Custom iOS Target Properties`에 추가:

| Key | Type | Value |
|---|---|---|
| `NSCameraUsageDescription` | String | "QR/바코드 미션을 풀기 위해 카메라가 필요해요" |
| `Privacy - Local Network Usage Description` | String | (로컬 서버 디버그용) "로컬 알람 서버에 연결합니다" |

`URL Types` → `+` 클릭:
- `URL Schemes`: `finalalarm`  (초대 링크 딥링크용)
- `Role`: Editor

## 3. Firebase SDK 추가 (FCM용 — 팀 알람 받으려면 필수)

1. **File → Add Package Dependencies**
2. URL: `https://github.com/firebase/firebase-ios-sdk`
3. Up to Next Major Version
4. Products 중 **`FirebaseMessaging`만** 체크 → Add Package

추가 후 `Core/PushService.swift` 안의 주석 처리된 블록 (`// FirebaseApp.configure()` 등) 의 주석을 해제하면 자동 활성화됩니다 (`#if canImport(FirebaseMessaging)` 조건 안에 있음).

## 4. GoogleService-Info.plist 다운로드

1. Firebase Console → 프로젝트 → iOS 앱 추가
2. Bundle ID: `com.jiny.finalalarm`
3. `GoogleService-Info.plist` 다운로드
4. Xcode의 `Finalalarm/Finalalarm/` 폴더에 드래그 (Copy items if needed ✓)

## 5. APNs 인증키 (Firebase Console)

Apple Developer 계정 → Keys → APNs 키 생성 → Firebase Console → Project Settings → Cloud Messaging → APNs Authentication Key 업로드

## 6. 서버 IP 확인

`Core/AppConfig.swift` 의 `apiBaseURL`이 현재 `http://172.30.1.22:3500/api/v1/` 로 되어있습니다. 본인 맥의 LAN IP에 맞춰 수정하세요. (Android와 동일한 서버 사용)

iOS는 HTTP 평문을 기본 차단하니, 디버그 빌드에서는 Info.plist에 ATS 예외 추가가 필요합니다:

```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsLocalNetworking</key>
    <true/>
</dict>
```

## 7. 알람 사운드 파일 (선택)

`Resources/`에 `alarm.mp3` 또는 `alarm.caf` 추가하면 `AlarmAudioPlayer`가 자동으로 픽업합니다. 없으면 무음 (시스템 노티 사운드만).

## 빌드 확인

```bash
cd ios/Finalalarm
xcodebuild -project Finalalarm.xcodeproj -scheme Finalalarm -destination "generic/platform=iOS" build
```

→ `** BUILD SUCCEEDED **` 확인됨.

## 알려진 LSP 경고

`Cannot find type 'XXX' in scope` 등의 SourceKit-LSP 경고는 Xcode 외부 인덱싱 한계로 발생하는 false positive — Xcode 빌드와 실행에는 영향 없음.
