#!/bin/bash
# 로컬 개발 서버 실행 스크립트
# 사용법: ./start-local.sh

set -e
cd "$(dirname "$0")"

PORT=3500
export PORT

# 1) Homebrew Postgres 자동 시작
if ! brew services list 2>/dev/null | grep -qE '^postgresql@16\s+started'; then
  echo "▶ postgresql@16 시작 중…"
  brew services start postgresql@16
  sleep 2
fi

# 2) 의존성
if [ ! -d node_modules ]; then
  echo "▶ npm install (첫 실행)"
  npm install
fi

# 3) .env 체크
if [ ! -f .env ]; then
  echo "❌ .env 파일이 없습니다. .env.example 복사 후 값 채워주세요."
  exit 1
fi

# 4) DB 없으면 생성
DB_NAME=$(grep -oE 'DATABASE_URL="postgresql://[^/]+/([^?"]+)' .env | sed -E 's|.*/([^?"]+).*|\1|')
DB_NAME="${DB_NAME:-finalalarm}"
PSQL=/opt/homebrew/opt/postgresql@16/bin
if ! "$PSQL/psql" -U jiny -d postgres -tAc \
     "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" | grep -q 1; then
  echo "▶ DB '$DB_NAME' 생성"
  "$PSQL/createdb" -U jiny "$DB_NAME"
fi

# 5) Prisma 클라이언트 + 마이그레이션
npx prisma generate >/dev/null
npx prisma migrate deploy >/dev/null 2>&1 || npx prisma migrate dev --name init

# 6) 네트워크 정보
DEFAULT_IFACE=$(route -n get default 2>/dev/null | awk '/interface:/ {print $2}')
LAN_IP=""
if [ -n "$DEFAULT_IFACE" ]; then
  LAN_IP=$(ipconfig getifaddr "$DEFAULT_IFACE" 2>/dev/null || echo "")
fi
LAN_IP="${LAN_IP:-localhost}"

cat <<EOF

══════════════════════════════════════════════════════════════
  FinalAlarm 로컬 서버
══════════════════════════════════════════════════════════════
  포트:        ${PORT}
  내 LAN IP:   ${LAN_IP}
  DB:          ${DB_NAME} (postgresql@16, brew services)

  Android build.gradle.kts → debug { API_BASE_URL } 갱신 값:

    실기기:  "http://${LAN_IP}:${PORT}/api/v1/"

  맥에서 직접 호출:  http://localhost:${PORT}/api/v1/
══════════════════════════════════════════════════════════════

EOF

npm run start:dev
