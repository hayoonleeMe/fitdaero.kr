import { useState, type SubmitEvent } from 'react'
import {
  recommendSimple,
  SimpleRecommendationApiError,
  type ActivityLevel,
  type ExperienceLevel,
  type FitnessGoal,
  type SelectableProgramCategory,
  type SimpleRecommendationRequest,
  type SimpleRecommendationResponse,
  type Weekday,
} from '../api/simpleRecommendation'

const goals: ReadonlyArray<{ value: FitnessGoal; label: string }> = [
  { value: 'STRENGTH', label: '근력' },
  { value: 'MUSCULAR_ENDURANCE', label: '근지구력' },
  { value: 'FLEXIBILITY', label: '유연성' },
  { value: 'CARDIO_ENDURANCE', label: '심폐지구력' },
  { value: 'WEIGHT_MANAGEMENT', label: '체중관리' },
  { value: 'STRESS_RELIEF', label: '스트레스 해소' },
]

const activityLevels: ReadonlyArray<{ value: ActivityLevel; label: string }> = [
  { value: 'NONE', label: '거의 하지 않음' },
  { value: 'LOW', label: '가볍게 함' },
  { value: 'MODERATE', label: '보통' },
  { value: 'HIGH', label: '꾸준히 함' },
]

const experienceLevels: ReadonlyArray<{ value: ExperienceLevel; label: string }> = [
  { value: 'BEGINNER', label: '처음 시작해요' },
  { value: 'RETURNING', label: '쉬었다가 다시 해요' },
  { value: 'REGULAR', label: '규칙적으로 해요' },
]

const weekdays: ReadonlyArray<{ value: Weekday; label: string }> = [
  { value: 'MON', label: '월' },
  { value: 'TUE', label: '화' },
  { value: 'WED', label: '수' },
  { value: 'THU', label: '목' },
  { value: 'FRI', label: '금' },
  { value: 'SAT', label: '토' },
  { value: 'SUN', label: '일' },
]

const categories: ReadonlyArray<{ value: SelectableProgramCategory; label: string }> = [
  { value: 'SWIMMING_AQUA', label: '수영·아쿠아' },
  { value: 'FITNESS_STRENGTH', label: '헬스·근력 운동' },
  { value: 'YOGA_PILATES', label: '요가·필라테스' },
  { value: 'CARDIO', label: '걷기·러닝·사이클' },
  { value: 'DANCE_AEROBIC', label: '댄스·에어로빅' },
  { value: 'RACKET_SPORTS', label: '라켓 스포츠' },
  { value: 'BALL_SPORTS', label: '구기 스포츠' },
  { value: 'MARTIAL_ARTS', label: '무예·격투' },
  { value: 'CLIMBING', label: '클라이밍' },
  { value: 'GOLF', label: '골프' },
]

const searchScopeLabels = {
  SIGUNGU: '선택한 시군구에서 찾았어요.',
  SIDO: '선택한 시도에서 찾았어요.',
  SIDO_FALLBACK: '선택한 시군구에 결과가 없어 같은 시도에서 찾았어요.',
} as const

const inputClassName =
  'mt-2 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-slate-900 outline-none transition focus:border-blue-600 focus:ring-2 focus:ring-blue-100 aria-[invalid=true]:border-red-500 aria-[invalid=true]:focus:ring-red-100'

type FieldErrors = Record<string, string>

export default function SimpleRecommendationPage() {
  const [response, setResponse] = useState<SimpleRecommendationResponse | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [requestError, setRequestError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [preferredCategories, setPreferredCategories] = useState<SelectableProgramCategory[]>([])
  const [avoidedCategories, setAvoidedCategories] = useState<SelectableProgramCategory[]>([])

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()

    const formData = new FormData(event.currentTarget)
    const selectedWeekdays = formData.getAll('weekdays').map(String) as Weekday[]

    if (selectedWeekdays.length === 0) {
      setFieldErrors({ weekdays: '가능한 요일을 하나 이상 선택해 주세요.' })
      return
    }

    const sigunguCode = String(formData.get('sigunguCode') ?? '').trim()
    const request: SimpleRecommendationRequest = {
      goal: String(formData.get('goal')) as FitnessGoal,
      activityLevel: String(formData.get('activityLevel')) as ActivityLevel,
      experienceLevel: String(formData.get('experienceLevel')) as ExperienceLevel,
      sidoCode: String(formData.get('sidoCode')).trim(),
      ...(sigunguCode ? { sigunguCode } : {}),
      weekdays: selectedWeekdays,
      preferredCategories,
      avoidedCategories,
    }

    setFieldErrors({})
    setRequestError(null)
    setIsSubmitting(true)

    try {
      setResponse(await recommendSimple(request))
    } catch (error) {
      if (error instanceof SimpleRecommendationApiError) {
        const errors = error.fieldErrors.reduce<FieldErrors>((result, fieldError) => {
          if (!(fieldError.field in result)) result[fieldError.field] = fieldError.message
          return result
        }, {})
        setFieldErrors(errors)
        setRequestError(error.message)
      } else {
        setRequestError('요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const categoryError =
    fieldErrors.preferredCategories ??
    fieldErrors.avoidedCategories ??
    fieldErrors.noOverlappingCategories ??
    fieldErrors.noOtherCategory

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-8 text-slate-900 sm:py-12">
      <div className="mx-auto max-w-3xl">
        <header className="mb-8">
          <p className="text-sm font-semibold tracking-wide text-blue-700">FITDAERO</p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">내게 맞는 운동 프로그램 찾기</h1>
          <p className="mt-3 leading-6 text-slate-600">
            운동 목표와 가능한 조건을 알려주시면 참여할 수 있는 공공 운동 프로그램을 추천해 드려요.
          </p>
        </header>

        <form
          className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200 sm:p-8"
          onSubmit={handleSubmit}
        >
          <div className="grid gap-6 sm:grid-cols-2">
            <SelectField id="goal" label="운동 목표" error={fieldErrors.goal} options={goals} />
            <SelectField
              id="activityLevel"
              label="주간 활동량"
              error={fieldErrors.activityLevel}
              options={activityLevels}
            />
            <SelectField
              id="experienceLevel"
              label="운동 경험"
              error={fieldErrors.experienceLevel}
              options={experienceLevels}
            />
            <div>
              <label className="text-sm font-medium" htmlFor="sidoCode">
                시도 코드
              </label>
              <input
                aria-describedby={fieldErrors.sidoCode ? 'sidoCode-error' : undefined}
                aria-invalid={Boolean(fieldErrors.sidoCode)}
                className={inputClassName}
                id="sidoCode"
                maxLength={20}
                name="sidoCode"
                placeholder="예: 1100000000"
                required
              />
              <FieldError id="sidoCode-error" message={fieldErrors.sidoCode} />
            </div>
            <div className="sm:col-span-2">
              <label className="text-sm font-medium" htmlFor="sigunguCode">
                시군구 코드 <span className="font-normal text-slate-500">(선택)</span>
              </label>
              <input
                aria-describedby={fieldErrors.sigunguCode ? 'sigunguCode-error' : undefined}
                aria-invalid={Boolean(fieldErrors.sigunguCode)}
                className={inputClassName}
                id="sigunguCode"
                maxLength={20}
                name="sigunguCode"
                placeholder="비워 두면 시도 전체에서 찾아요. 예: 1120000000"
              />
              <FieldError id="sigunguCode-error" message={fieldErrors.sigunguCode} />
            </div>
          </div>

          <fieldset className="mt-7">
            <legend className="text-sm font-medium">가능한 요일</legend>
            <div
              aria-describedby={fieldErrors.weekdays ? 'weekdays-error' : undefined}
              className="mt-3 grid grid-cols-4 gap-2 sm:grid-cols-7"
            >
              {weekdays.map((weekday) => (
                <label
                  className="cursor-pointer rounded-lg border border-slate-200 px-3 py-2 text-center text-sm transition has-checked:border-blue-600 has-checked:bg-blue-50 has-checked:text-blue-800"
                  key={weekday.value}
                >
                  <input className="sr-only" name="weekdays" type="checkbox" value={weekday.value} />
                  {weekday.label}
                </label>
              ))}
            </div>
            <FieldError id="weekdays-error" message={fieldErrors.weekdays} />
          </fieldset>

          <fieldset className="mt-7">
            <legend className="text-sm font-medium">선호 종목</legend>
            <p className="mt-1 text-sm text-slate-500">관심 있는 종목을 모두 선택할 수 있어요.</p>
            <CategoryOptions
              disabledCategories={avoidedCategories}
              name="preferredCategories"
              selectedCategories={preferredCategories}
              onChange={(category, checked) =>
                setPreferredCategories((current) => toggleCategory(current, category, checked))
              }
            />
          </fieldset>

          <fieldset className="mt-7">
            <legend className="text-sm font-medium">비선호 종목</legend>
            <p className="mt-1 text-sm text-slate-500">추천에서 제외할 종목을 선택하세요.</p>
            <CategoryOptions
              disabledCategories={preferredCategories}
              name="avoidedCategories"
              selectedCategories={avoidedCategories}
              onChange={(category, checked) =>
                setAvoidedCategories((current) => toggleCategory(current, category, checked))
              }
            />
            <FieldError id="categories-error" message={categoryError} />
          </fieldset>

          {requestError && (
            <p className="mt-6 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
              {requestError}
            </p>
          )}

          <button
            className="mt-7 w-full rounded-lg bg-blue-700 px-4 py-3 font-semibold text-white transition hover:bg-blue-800 disabled:cursor-not-allowed disabled:bg-blue-400"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? '추천을 찾는 중이에요...' : '운동 프로그램 추천받기'}
          </button>
        </form>

        {response && <RecommendationResults response={response} />}
      </div>
    </main>
  )
}

function SelectField<T extends string>({
  id,
  label,
  error,
  options,
}: {
  id: string
  label: string
  error?: string
  options: ReadonlyArray<{ value: T; label: string }>
}) {
  return (
    <div>
      <label className="text-sm font-medium" htmlFor={id}>
        {label}
      </label>
      <select
        aria-describedby={error ? `${id}-error` : undefined}
        aria-invalid={Boolean(error)}
        className={inputClassName}
        id={id}
        name={id}
        required
        defaultValue=""
      >
        <option disabled value="">
          선택해 주세요
        </option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <FieldError id={`${id}-error`} message={error} />
    </div>
  )
}

function CategoryOptions({
  name,
  selectedCategories,
  disabledCategories,
  onChange,
}: {
  name: 'preferredCategories' | 'avoidedCategories'
  selectedCategories: SelectableProgramCategory[]
  disabledCategories: SelectableProgramCategory[]
  onChange: (category: SelectableProgramCategory, checked: boolean) => void
}) {
  return (
    <div className="mt-3 grid gap-2 sm:grid-cols-2">
      {categories.map((category) => {
        const disabled = isCategoryDisabled(category.value, disabledCategories)

        return (
          <label
            className={`flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2.5 text-sm ${
              disabled ? 'cursor-not-allowed bg-slate-100 text-slate-400' : 'cursor-pointer'
            }`}
            key={category.value}
          >
            <input
              checked={selectedCategories.includes(category.value)}
              className="size-4 accent-blue-700"
              disabled={disabled}
              name={name}
              type="checkbox"
              value={category.value}
              onChange={(event) => onChange(category.value, event.target.checked)}
            />
            {category.label}
          </label>
        )
      })}
    </div>
  )
}

export function isCategoryDisabled(
  category: SelectableProgramCategory,
  oppositeCategories: SelectableProgramCategory[],
) {
  return oppositeCategories.includes(category)
}

function toggleCategory(
  selectedCategories: SelectableProgramCategory[],
  category: SelectableProgramCategory,
  checked: boolean,
) {
  return checked
    ? [...selectedCategories, category]
    : selectedCategories.filter((selectedCategory) => selectedCategory !== category)
}

function FieldError({ id, message }: { id: string; message?: string }) {
  return message ? (
    <p className="mt-2 text-sm text-red-600" id={id} role="alert">
      {message}
    </p>
  ) : null
}

function RecommendationResults({ response }: { response: SimpleRecommendationResponse }) {
  return (
    <section aria-live="polite" className="mt-8">
      <div className="rounded-2xl bg-blue-700 p-5 text-white sm:p-7">
        <p className="text-sm font-medium text-blue-100">간편 추천 결과</p>
        <h2 className="mt-2 text-xl font-bold">{response.analysisSummary}</h2>
        <p className="mt-3 text-sm leading-6 text-blue-100">{searchScopeLabels[response.searchScope]}</p>
        <p className="mt-3 text-sm text-blue-100">
          프로그램 데이터 버전:{' '}
          {response.dataVersions.publicFacilityProgram ?? '현재 적재된 데이터가 없습니다.'}
        </p>
      </div>

      {response.recommendations.length === 0 ? (
        <div className="mt-4 rounded-2xl bg-white p-6 text-center shadow-sm ring-1 ring-slate-200">
          <h3 className="font-semibold">조건에 맞는 프로그램을 찾지 못했어요.</h3>
          <p className="mt-2 text-sm leading-6 text-slate-600">지역, 요일 또는 선호 종목을 바꿔 다시 추천받아 보세요.</p>
        </div>
      ) : (
        <div className="mt-4 grid gap-4">
          {response.recommendations.map((program) => (
            <article className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200" key={program.programId}>
              <p className="text-sm font-medium text-blue-700">{program.facilityName}</p>
              <h3 className="mt-1 text-xl font-bold">{program.programName}</h3>
              <dl className="mt-4 grid gap-2 text-sm text-slate-600">
                <div>
                  <dt className="sr-only">주소</dt>
                  <dd>{program.address}</dd>
                </div>
                <div>
                  <dt className="sr-only">기간</dt>
                  <dd>
                    {formatDate(program.startsOn)} ~ {formatDate(program.endsOn)} · {program.weekdayText}
                  </dd>
                </div>
                <div>
                  <dt className="sr-only">가격</dt>
                  <dd>{formatPrice(program.price, program.priceTypeName)}</dd>
                </div>
              </dl>
              <ul className="mt-4 space-y-1 rounded-lg bg-blue-50 p-3 text-sm text-blue-950">
                {program.reasons.map((reason) => (
                  <li key={reason}>· {reason}</li>
                ))}
              </ul>
              {program.homepageUrl && (
                <a
                  className="mt-4 inline-flex text-sm font-semibold text-blue-700 underline underline-offset-4"
                  href={program.homepageUrl}
                  rel="noreferrer"
                  target="_blank"
                >
                  프로그램 홈페이지 보기
                </a>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium' }).format(new Date(`${value}T00:00:00`))
}

function formatPrice(price: number | null, priceTypeName: string | null) {
  if (price === null) return priceTypeName ?? '가격 정보 없음'

  const formattedPrice = new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(price)

  return priceTypeName ? `${formattedPrice} (${priceTypeName})` : formattedPrice
}
