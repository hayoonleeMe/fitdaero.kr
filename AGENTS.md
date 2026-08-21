# FitDaero 작업 지침

이 파일은 저장소 전체에 적용된다. 하위 디렉터리에 `AGENTS.md`가 있으면 해당 디렉터리와 그 하위 경로에서는 그 지침을 함께 따르며, 충돌할 경우 더 가까운 지침을 우선한다.

## Git 및 GitHub

- 브랜치 전략, 브랜치 이름, 커밋 메시지, 이슈와 Pull Request 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 따른다.
- 이슈를 만들거나 수정할 때는 `.github/ISSUE_TEMPLATE/feature.md` 또는 `.github/ISSUE_TEMPLATE/bug.md`를 따른다.
- Pull Request를 만들 때는 `.github/pull_request_template.md`를 따른다.
- CI 변경은 `.github/workflows/`의 기존 워크플로와 영향을 받는 경로를 함께 확인한다.

## 공통 작업 원칙

- 요청 범위와 관련 없는 기존 변경은 수정하거나 되돌리지 않는다.
- 비밀값, API 키, 원본 데이터 파일은 커밋하지 않는다.
- 변경에 맞는 가장 작은 검증을 실행하고, 실행하지 못한 검증은 이유를 함께 기록한다.

## 설계 문서

기능 구현·변경 전 관련 설계 문서를 확인하고, 동작·API·스키마를 바꾸면 같은 변경에서 문서도 갱신한다.

- 제품 범위: `docs/prd.md`
- API 계약: `docs/api.md`
- 추천 규칙: `docs/recommendation.md`
- 데이터 모델: `docs/erd.md`
- 데이터 적재: `docs/data-import.md`

## 하위 프로젝트 지침

- 백엔드 전용 빌드, 테스트, 코드 및 데이터베이스 규칙은 `backend/AGENTS.md`에 둔다.
- 프런트엔드 전용 빌드, 테스트, 코드 및 UI 규칙은 `frontend/AGENTS.md`에 둔다.
