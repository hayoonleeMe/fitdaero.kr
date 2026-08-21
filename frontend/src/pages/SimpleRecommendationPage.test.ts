import { describe, expect, it } from 'vitest'
import { isCategoryDisabled } from './SimpleRecommendationPage'

describe('isCategoryDisabled', () => {
  it('disables only a category selected in the opposite group', () => {
    expect(isCategoryDisabled('SWIMMING_AQUA', ['SWIMMING_AQUA'])).toBe(true)
    expect(isCategoryDisabled('YOGA_PILATES', ['SWIMMING_AQUA'])).toBe(false)
  })
})
