import { describe, expect, it } from 'vitest'
import { getSigungus, regions } from './regions'

describe('regions', () => {
  it('provides all 17 sidos and their matching sigungus', () => {
    expect(regions).toHaveLength(17)
    expect(getSigungus('11')).toContainEqual({ code: '11110', name: '종로구' })
    expect(getSigungus('36')).toEqual([])
  })
})
