# FinalAlarm Server

팀 기반 알람 앱의 백엔드. NestJS + Prisma + PostgreSQL.

## 운영 구조

별도 git 레포로 관리되며 coachDesk와 **같은 호스트**에 별도 docker-compose로 실행. coachDesk PostgreSQL을 공유.

```
Host
├── coachDesk (별도 docker-compose, 자체 네트워크 coachdesk_default)
│   └── postgres (컨테이너)
└── finalalarm-server (이 레포, 자체 docker-compose)
    └── finalalarm-api → coachdesk_default 네트워크 join → postgres 접근
```

## 로컬 개발

```bash
npm install
cp .env.example .env          # 값 채우기
docker run -d --name pg -p 5432:5432 -e POSTGRES_PASSWORD=pass postgres:16  # 임시 DB
# .env의 DATABASE_URL을 위 컨테이너에 맞추기
npx prisma migrate dev --name init
npm run start:dev
```

서버: `http://localhost:3000/api/v1/...`

## 운영 배포 (호스트에서)

### 사전 준비 (1회)

1. coachDesk 네트워크 이름 확인:
   ```bash
   docker network ls | grep coachdesk
   # 예: coachdesk_default
   ```
2. `docker-compose.yml`의 `networks.coachdesk_net.name` 값을 위 이름으로 수정
3. coachDesk의 `postgres` 서비스에 `alarm_*` 테이블을 만들 권한이 있는 사용자 확인

### 환경 변수 (`.env`)

```env
DATABASE_URL="postgresql://USER:PASS@postgres:5432/coachdesk?schema=public"
JWT_ACCESS_SECRET="..."
JWT_REFRESH_SECRET="..."
JWT_ACCESS_TTL="1h"
JWT_REFRESH_TTL="30d"
FCM_SERVICE_ACCOUNT_PATH="/run/secrets/fcm-service-account.json"
INVITE_LINK_BASE="https://finalalarm.app/i/"
PORT=3000
NODE_ENV=production
```

### FCM 키 배치

Firebase 콘솔에서 서비스 계정 JSON 다운로드 → `server/secrets/fcm-service-account.json` 으로 저장 (gitignore됨)

### DB 마이그레이션

빌드 후 컨테이너 진입해서 1회 실행 (이후 새 마이그레이션 추가될 때마다):

```bash
docker compose run --rm finalalarm-api npx prisma migrate deploy
```

### 실행

```bash
docker compose up -d --build
docker compose logs -f finalalarm-api
```

업데이트:
```bash
git pull
docker compose up -d --build
```

## 코드 구조

```
src/
├── main.ts                  bootstrap
├── app.module.ts
├── prisma/                  PrismaService
├── common/                  공통 유틸 (errors, guards, decorators)
├── auth/                    회원가입·로그인·JWT
├── me/                      내 정보
├── teams/                   팀·멤버
├── invites/                 초대 코드·링크
├── missions/                끄기 미션 프로필
├── push-tokens/             FCM 토큰 등록
├── alarms/                  알람 정의 CRUD
├── windows/                 알람 시간대
├── push-alarm/              팀원에게 알람 발사
├── events/                  알람 이벤트 (snooze, dismiss, unlock-request)
├── unlock-requests/         잠금해제 요청 inbox·approve
└── fcm/                     Firebase Admin SDK 래퍼
```

## 백그라운드 워커가 없습니다

알람 발사 / 만료 처리는 모두 클라이언트 또는 lazy expiration으로 처리. 자세한 건 `../SERVER_ARCH.md` 참조.
