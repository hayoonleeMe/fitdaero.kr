# Contributing Guide

## Branch strategy

- `main`: 배포 가능한 안정 버전
- `develop`: 다음 배포를 위한 통합 브랜치
- 작업 브랜치는 `develop`에서 생성하고, 완료 후 `develop`으로 Pull Request를 생성합니다.
- 배포할 때는 `develop`에서 `main`으로 Pull Request를 생성합니다.

## Branch naming

브랜치 이름은 다음 형식을 사용합니다.

```text
<type>/<short-description>
```

| Type | Usage | Example |
| --- | --- | --- |
| `feature` | 기능 개발 | `feature/create-program-search` |
| `fix` | 버그 수정 | `fix/recommendation-score` |
| `refactor` | 기능 변경 없는 구조 개선 | `refactor/extract-program-service` |
| `docs` | 문서 작업 | `docs/update-api-guide` |
| `chore` | 설정, 의존성 등 기타 작업 | `chore/add-github-templates` |

설명은 소문자 영어와 하이픈(`-`)을 사용합니다.

## Commit message

커밋 메시지는 다음 형식을 사용합니다.

```text
<type>: <summary>
```

| Type | Usage | Example |
| --- | --- | --- |
| `feat` | 기능 추가 | `feat: 프로그램 검색 기능 추가` |
| `fix` | 버그 수정 | `fix: 추천 점수 계산 수정` |
| `refactor` | 기능 변경 없는 구조 개선 | `refactor: 프로그램 검색 로직 분리` |
| `docs` | 문서 변경 | `docs: API 사용 방법 추가` |
| `test` | 테스트 추가 또는 수정 | `test: 프로그램 검색 테스트 추가` |
| `chore` | 설정, 의존성 등 기타 작업 | `chore: 코드 포맷터 추가` |

- `type`은 소문자 영어를 사용합니다.
- `summary`는 변경 의도가 드러나도록 한글로 작성해도 됩니다.
- 하나의 커밋에는 가능한 한 하나의 목적만 담습니다.

## Issue

- 기능 개발, 버그 수정처럼 추적이 필요한 작업은 이슈를 먼저 생성합니다.
- 기능은 Feature, 오류는 Bug 템플릿을 사용합니다.
- 이슈에는 목적, 요구 사항, 완료 조건을 작성합니다.
- Pull Request 본문에 `Closes #이슈번호`로 연결합니다.

## Pull Request

- 작업 브랜치는 `develop`을 대상으로 Pull Request를 생성합니다.
- 배포 Pull Request만 `develop`에서 `main`으로 생성합니다.
- 제목은 커밋 메시지와 같은 형식을 사용합니다. 예: `feat: 프로그램 검색 기능 추가`
- 본문에는 변경 사항, 테스트 여부, 관련 이슈를 간단히 작성합니다.
- 작업 중인 Pull Request는 Draft로 생성해도 됩니다.

## Basic flow

```text
Issue (선택) → develop에서 작업 브랜치 생성 → 작업 및 커밋 → PR → develop 병합
```

작은 문서 수정처럼 이슈가 꼭 필요하지 않은 작업은 이슈 없이 진행해도 됩니다.
