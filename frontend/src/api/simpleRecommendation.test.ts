import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  recommendSimple,
  SimpleRecommendationApiError,
  type SimpleRecommendationRequest,
} from './simpleRecommendation'

const request: SimpleRecommendationRequest = {
  goal: 'CARDIO_ENDURANCE',
  activityLevel: 'LOW',
  experienceLevel: 'BEGINNER',
  sidoCode: '1100000000',
  weekdays: ['MON'],
  preferredCategories: [],
  avoidedCategories: [],
}

afterEach(() => vi.unstubAllGlobals())

describe('recommendSimple', () => {
  it('returns a successful recommendation response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            analysisType: 'SIMPLE',
            analysisSummary: '추천 결과예요.',
            dataVersions: { publicFacilityProgram: 'version' },
            searchScope: 'SIDO',
            recommendations: [],
          }),
          { status: 200 },
        ),
      ),
    )

    await expect(recommendSimple(request)).resolves.toMatchObject({ searchScope: 'SIDO' })
    expect(fetch).toHaveBeenCalledWith('/api/recommendations/simple', expect.any(Object))
  })

  it('keeps field errors from a 400 response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            code: 'VALIDATION_ERROR',
            message: '요청값이 올바르지 않습니다.',
            fieldErrors: [{ field: 'sidoCode', message: '시도 코드는 필수입니다.' }],
          }),
          { status: 400 },
        ),
      ),
    )

    await expect(recommendSimple(request)).rejects.toMatchObject({
      fieldErrors: [{ field: 'sidoCode', message: '시도 코드는 필수입니다.' }],
    })
  })

  it('turns network failures into a retryable error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))

    await expect(recommendSimple(request)).rejects.toBeInstanceOf(SimpleRecommendationApiError)
  })

  it('rejects an incomplete successful response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(JSON.stringify({ recommendations: [] }), { status: 200 })),
    )

    await expect(recommendSimple(request)).rejects.toBeInstanceOf(SimpleRecommendationApiError)
  })
})
