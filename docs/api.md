# FitDaero MVP API 명세

## 공통 규칙

- Base path: `/api`
- Content-Type: `application/json`
- 모든 추천 요청은 저장하지 않는다.
- `sidoCode`는 필수, `sigunguCode`는 선택이다.
- `weekdays`는 하나 이상의 `MON`~`SUN` 값이다.
- `preferredCategories`와 `avoidedCategories`는 `ProgramCategory`의 복수 선택값이며 서로 겹칠 수 없다.
- 추천 결과는 최대 5건이다.
- 추천 요청은 적재를 실행하지 않는다. 기본 실행에서는 기존 완료 데이터만 사용하며, 프로그램 데이터가 없으면 빈 `recommendations`를 반환한다.
- `dataVersions.publicFacilityProgram`은 가장 최근 완료 프로그램 적재의 `data_version`이며, 완료 프로그램 데이터가 없으면 `null`이다. 정밀 분석의 `dataVersions.fitnessReference`는 사용한 최신 완료 체력 API 적재의 `data_version`이다.

### 공통 입력 필드

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `goal` | `FitnessGoal` | Y | 사용자의 주 운동 목표 |
| `sidoCode` | string | Y | 숫자 2자리 시도 코드 |
| `sigunguCode` | string | N | 숫자 5자리 시군구 코드. 생략하면 시도 범위로 검색 |
| `weekdays` | array of `Weekday` | Y | 하나 이상의 가능한 요일 |
| `preferredCategories` | array of `ProgramCategory` | N | 선호 종목군. 생략 시 빈 목록 |
| `avoidedCategories` | array of `ProgramCategory` | N | 제외할 종목군. 생략 시 빈 목록 |

`FitnessGoal`은 `STRENGTH`, `MUSCULAR_ENDURANCE`, `FLEXIBILITY`, `CARDIO_ENDURANCE`, `WEIGHT_MANAGEMENT`, `STRESS_RELIEF`다.

`ProgramCategory`는 `SWIMMING_AQUA`, `FITNESS_STRENGTH`, `YOGA_PILATES`, `CARDIO`, `DANCE_AEROBIC`, `RACKET_SPORTS`, `BALL_SPORTS`, `MARTIAL_ARTS`, `CLIMBING`, `GOLF`, `OTHER`다. `OTHER`는 선호·비선호 입력값으로 허용하지 않으며, 선호·비선호 종목은 서로 겹칠 수 없다.

## 간편 분석

### `POST /api/recommendations/simple`

#### 요청

```json
{
  "goal": "CARDIO_ENDURANCE",
  "activityLevel": "LOW",
  "experienceLevel": "BEGINNER",
  "sidoCode": "11",
  "sigunguCode": "11200",
  "weekdays": ["MON", "WED", "FRI"],
  "preferredCategories": ["SWIMMING_AQUA"],
  "avoidedCategories": ["MARTIAL_ARTS"]
}
```

| 추가 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `activityLevel` | `NONE`, `LOW`, `MODERATE`, `HIGH` | Y | 주간 활동량 |
| `experienceLevel` | `BEGINNER`, `RETURNING`, `REGULAR` | Y | 운동 경험 |

#### 응답

`analysisType`은 `SIMPLE`이다. `analysisSummary`는 응답 기반 추천임을 명시하며 체력 측정 상태·백분위·등급 필드를 포함하지 않는다.

```json
{
  "analysisType": "SIMPLE",
  "analysisSummary": "선택한 목표와 생활 응답을 바탕으로 추천했어요.",
  "dataVersions": {
    "publicFacilityProgram": "KS_PUBLIC_ALSFC_PROGRM_INFO_202606"
  },
  "searchScope": "SIGUNGU",
  "recommendations": []
}
```

## 정밀 분석

### `POST /api/recommendations/detailed`

#### 요청

```json
{
  "goal": "STRENGTH",
  "comparisonSex": "F",
  "age": 31,
  "relativeGrip": 42.5,
  "sitAndReach": 18.2,
  "crossSitUp": 23,
  "sidoCode": "11",
  "sigunguCode": "11200",
  "weekdays": ["MON", "WED", "FRI"],
  "preferredCategories": ["FITNESS_STRENGTH"],
  "avoidedCategories": []
}
```

| 추가 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| `comparisonSex` | `F`, `M` | Y | 원본 비교 기준 성별 |
| `age` | integer | Y | 요청일 기준 만 나이, 19~64 |
| `relativeGrip` | decimal | N | 상대악력 %, 0 초과인 유한 수 |
| `sitAndReach` | decimal | N | 앉아윗몸앞으로굽히기 cm, 음수 허용 유한 수 |
| `crossSitUp` | integer | N | 교차윗몸일으키기 회, 0 이상 |

세 측정값 중 하나 이상은 반드시 입력한다.

#### 응답

`analysisType`은 `DETAILED`다. `fitnessFactors`에는 입력된 항목만 포함하며, 요청 나이에 해당하는 동일 성별·10세 단위 연령대 기준값을 사용한다. 상태는 `LOW`, `NORMAL`, `HIGH` 중 하나다. 기준값이 없는 입력 항목은 `422 REFERENCE_NOT_AVAILABLE`로 요청을 거절한다.

비교 기준은 `completed_at DESC, id DESC` 순서의 가장 최근 `COMPLETED` 상태 체력 API 적재본에서만 조회한다. 완료된 기준값이 없거나 요청 항목의 기준값이 없으면 `422 REFERENCE_NOT_AVAILABLE`을 반환한다.

- `LOW`: 25 분위 미만
- `NORMAL`: 25 분위 이상 75 분위 이하
- `HIGH`: 75 분위 초과

```json
{
  "analysisType": "DETAILED",
  "analysisSummary": "2022년 이후 공개 체력측정 데이터의 동일 성별·연령대 분포와 비교한 결과예요.",
  "fitnessFactors": [
    {
      "metric": "RELATIVE_GRIP",
      "status": "LOW",
      "sampleCount": 12468
    }
  ],
  "dataVersions": {
    "publicFacilityProgram": "KS_PUBLIC_ALSFC_PROGRM_INFO_202606",
    "fitnessReference": "NFA_API_202201-202608"
  },
  "searchScope": "SIDO_FALLBACK",
  "recommendations": []
}
```

## 공통 추천 결과

```json
{
  "programId": 1,
  "programName": "성인 초급 수영",
  "category": "SWIMMING_AQUA",
  "facilityName": "금호교육문화관수영장",
  "address": "서울특별시 성동구 ...",
  "startsOn": "2026-08-01",
  "endsOn": "2026-08-31",
  "weekdayText": "월수금",
  "price": 49500,
  "priceTypeName": null,
  "homepageUrl": "https://example.com",
  "score": 82.5,
  "reasons": ["근력 목표에 맞는 종목이에요.", "가능한 요일 중 월·수·금에 참여할 수 있어요."]
}
```

`searchScope`은 `SIGUNGU`, `SIDO`, `SIDO_FALLBACK` 중 하나다. `SIDO`는 시군구를 입력하지 않았을 때 사용한다. 후보가 없으면 빈 `recommendations`를 반환한다.

## 오류 응답

모든 오류는 다음 구조를 사용한다. `fieldErrors`는 필드별 검증 오류가 없으면 빈 배열이다.

```json
{
  "code": "VALIDATION_ERROR",
  "message": "요청값이 올바르지 않습니다.",
  "fieldErrors": [
    {"field": "age", "message": "19~64세만 입력할 수 있습니다."}
  ]
}
```

| 상태 | 코드 | 발생 조건 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 필수값 누락, enum·요일·지역 입력 오류, 선호·비선호 중복, 정밀 분석 나이·성별·측정값 검증 오류 |
| `422` | `REFERENCE_NOT_AVAILABLE` | 최신 완료 체력 API 적재본 또는 요청 항목의 성별·연령대 기준값이 없음 |
