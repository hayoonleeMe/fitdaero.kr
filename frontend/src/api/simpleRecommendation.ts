export type FitnessGoal =
  | 'STRENGTH'
  | 'MUSCULAR_ENDURANCE'
  | 'FLEXIBILITY'
  | 'CARDIO_ENDURANCE'
  | 'WEIGHT_MANAGEMENT'
  | 'STRESS_RELIEF'

export type ActivityLevel = 'NONE' | 'LOW' | 'MODERATE' | 'HIGH'

export type ExperienceLevel = 'BEGINNER' | 'RETURNING' | 'REGULAR'

export type Weekday = 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN'

const programCategories = [
  'SWIMMING_AQUA',
  'FITNESS_STRENGTH',
  'YOGA_PILATES',
  'CARDIO',
  'DANCE_AEROBIC',
  'RACKET_SPORTS',
  'BALL_SPORTS',
  'MARTIAL_ARTS',
  'CLIMBING',
  'GOLF',
  'OTHER',
] as const

export type ProgramCategory = (typeof programCategories)[number]

export type SelectableProgramCategory = Exclude<ProgramCategory, 'OTHER'>

export interface SimpleRecommendationRequest {
  goal: FitnessGoal
  activityLevel: ActivityLevel
  experienceLevel: ExperienceLevel
  sidoCode: string
  sigunguCode?: string
  weekdays: Weekday[]
  preferredCategories: SelectableProgramCategory[]
  avoidedCategories: SelectableProgramCategory[]
}

export interface RecommendedProgram {
  programId: number
  programName: string
  category: ProgramCategory
  facilityName: string
  address: string
  startsOn: string
  endsOn: string
  weekdayText: string
  price: number | null
  priceTypeName: string | null
  homepageUrl: string | null
  score: number
  reasons: string[]
}

export interface SimpleRecommendationResponse {
  analysisType: 'SIMPLE'
  analysisSummary: string
  dataVersions: {
    publicFacilityProgram: string | null
  }
  searchScope: 'SIGUNGU' | 'SIDO' | 'SIDO_FALLBACK'
  recommendations: RecommendedProgram[]
}

export interface ApiFieldError {
  field: string
  message: string
}

interface ApiErrorResponse {
  code: string
  message: string
  fieldErrors: ApiFieldError[]
}

export class SimpleRecommendationApiError extends Error {
  readonly fieldErrors: ApiFieldError[]

  constructor(message: string, fieldErrors: ApiFieldError[] = []) {
    super(message)
    this.name = 'SimpleRecommendationApiError'
    this.fieldErrors = fieldErrors
  }
}

export async function recommendSimple(
  request: SimpleRecommendationRequest,
): Promise<SimpleRecommendationResponse> {
  let response: Response

  try {
    response = await fetch('/api/recommendations/simple', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })
  } catch {
    throw new SimpleRecommendationApiError('서버와 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.')
  }

  const body: unknown = await response.json().catch(() => null)

  if (response.ok && isSimpleRecommendationResponse(body)) {
    return body
  }

  if (response.status === 400 && isApiErrorResponse(body)) {
    throw new SimpleRecommendationApiError(body.message, body.fieldErrors)
  }

  throw new SimpleRecommendationApiError('요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.')
}

function isSimpleRecommendationResponse(value: unknown): value is SimpleRecommendationResponse {
  if (!isRecord(value) || !isRecord(value.dataVersions)) return false

  return (
    value.analysisType === 'SIMPLE' &&
    typeof value.analysisSummary === 'string' &&
    (typeof value.dataVersions.publicFacilityProgram === 'string' ||
      value.dataVersions.publicFacilityProgram === null) &&
    isSearchScope(value.searchScope) &&
    Array.isArray(value.recommendations) &&
    value.recommendations.every(isRecommendedProgram)
  )
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  return (
    isRecord(value) &&
    typeof value.code === 'string' &&
    typeof value.message === 'string' &&
    Array.isArray(value.fieldErrors) &&
    value.fieldErrors.every(isApiFieldError)
  )
}

function isRecommendedProgram(value: unknown): value is RecommendedProgram {
  return (
    isRecord(value) &&
    typeof value.programId === 'number' &&
    typeof value.programName === 'string' &&
    isProgramCategory(value.category) &&
    typeof value.facilityName === 'string' &&
    typeof value.address === 'string' &&
    typeof value.startsOn === 'string' &&
    typeof value.endsOn === 'string' &&
    typeof value.weekdayText === 'string' &&
    (typeof value.price === 'number' || value.price === null) &&
    (typeof value.priceTypeName === 'string' || value.priceTypeName === null) &&
    (typeof value.homepageUrl === 'string' || value.homepageUrl === null) &&
    typeof value.score === 'number' &&
    Array.isArray(value.reasons) &&
    value.reasons.every((reason) => typeof reason === 'string')
  )
}

function isApiFieldError(value: unknown): value is ApiFieldError {
  return isRecord(value) && typeof value.field === 'string' && typeof value.message === 'string'
}

function isProgramCategory(value: unknown): value is ProgramCategory {
  return typeof value === 'string' && (programCategories as readonly string[]).includes(value)
}

function isSearchScope(value: unknown): value is SimpleRecommendationResponse['searchScope'] {
  return value === 'SIGUNGU' || value === 'SIDO' || value === 'SIDO_FALLBACK'
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
