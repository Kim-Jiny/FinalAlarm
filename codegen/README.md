# OpenAPI 코드 생성

서버 DTO·라우트가 변경되면 한 번에 안드·iOS 모델·API 클라이언트를 재생성한다.

## 흐름

```
server/src/**/dto.ts (입력 검증)
server/src/common/dto/responses.ts (응답 스키마)
        ↓ (nest build + OPENAPI_EXPORT=1)
openapi.json (단일 진실)
        ↓ openapi-generator
codegen/out/android  (Kotlin + Retrofit + kotlinx.serialization)
codegen/out/ios      (Swift Package + URLSession + Codable)
```

## 실행

```bash
# 서버 빌드 후 openapi.json + 양쪽 클라 코드 생성
npm run codegen

# 부분만:
npm run openapi:export
npm run codegen:android
npm run codegen:ios
```

## 통합 정책

**현재**: codegen 결과물은 `codegen/out/`에 생성되지만 **앱 프로젝트가 직접 사용하지는 않음**. 이번까지 수동으로 맞춰둔 모델은 그대로 유지.

**다음 API 변경부터**:
1. 서버 DTO/응답 schema 수정
2. `npm run codegen` 실행
3. `git diff codegen/out/` 로 변경된 모델·메서드 시그니처 확인
4. 안드/iOS 앱 코드에서 해당 부분만 동기화 — 새 필드 추가/필드명 변경 등 컴파일 단계에서 누락 즉시 발견

**향후 점진 교체 (선택)**:
- 안드: `app/build.gradle.kts`에 `sourceSets["main"].kotlin.srcDirs("../codegen/out/android/src/main/kotlin")` 추가 → 기존 `Models.kt` 클래스 삭제 → import 경로만 교체
- iOS: `codegen/out/ios`를 Swift Package Dependency로 추가 → `Models.swift` 모델·API 클래스 삭제 → import 경로 교체

## 주의

- openapi.json은 commit하지 않음 (`.gitignore`에 추가됨). 매번 빌드로부터 재생성.
- `codegen/out/` 도 일반적으로 commit하지 않지만, **변경 감지 PR 검토 편의를 위해 일단 commit한다**. 자동화 시점에 재검토.
- response DTO (`server/src/common/dto/responses.ts`)는 Prisma 모델 거울. Prisma schema 변경 시 같이 갱신.
