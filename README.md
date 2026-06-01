# FinalAlarm

팀 기반 알람 앱. 본인이 등록한 알람은 본인이 미리 설정한 미션을 풀어야 끌 수 있고, 팀원 승인 알람은 팀원이 먼저 승인을 해줘야 미션 단계로 넘어갈 수 있어요. 팀원끼리 정해둔 시간대 안에서 서로에게 알람을 발사할 수도 있습니다.

## 구성

| 디렉토리 | 설명 |
|---------|------|
| `android/` | Kotlin + Jetpack Compose 앱 |
| `server/` | NestJS + Prisma + PostgreSQL API |
| `PLAN.md` | 기획·결정사항·정책 |
| `DATA_MODEL.md` | DB 스키마 |
| `API_SPEC.md` | REST API + FCM 페이로드 |
| `SERVER_ARCH.md` | 서버 아키텍처·배포 |
| `ANDROID_ARCH.md` | 안드 기술 스택·모듈 |
| `ANDROID_SCREENS.md` | 화면·플로우 |

## 빠른 시작

### 서버

```bash
cd server
npm install
cp .env.example .env  # 값 채우기
npx prisma migrate dev --name init
npm run start:dev
```

자세한 배포 가이드는 `server/README.md` 참조.

### 안드로이드

```bash
cd android
# Android Studio로 열기
# app/google-services.json 배치 (Firebase 콘솔에서)
# Build → Run
```

## 플랫폼

- Android (Kotlin · Jetpack Compose) — 우선 개발
- iOS (SwiftUI) — 추후

## 라이선스

미정.
