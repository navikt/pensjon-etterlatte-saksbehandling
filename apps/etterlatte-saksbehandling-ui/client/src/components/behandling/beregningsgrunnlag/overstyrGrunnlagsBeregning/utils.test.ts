import { describe, expect, it } from 'vitest'
import { validerPeriode } from './utils'

describe('validerPeriode', () => {
  it('avviser til og med før fra og med', () => {
    expect(validerPeriode('2026-05-01', new Date(2026, 0, 1))).toBe('Til og med må være lik eller etter Fra og med.')
  })

  it('tillater samme måned og senere måneder', () => {
    expect(validerPeriode('2026-05-01', new Date(2026, 4, 1))).toBeUndefined()
    expect(validerPeriode('2026-05-01', new Date(2026, 5, 1))).toBeUndefined()
  })

  it('validerer datostrenger fra skjemaet', () => {
    expect(validerPeriode('2026-05-01', '2026-01-31')).toBe('Til og med må være lik eller etter Fra og med.')
    expect(validerPeriode('2026-05-01', '2026-05-01')).toBeUndefined()
  })

  it('tillater perioder uten til og med', () => {
    expect(validerPeriode('2026-05-01')).toBeUndefined()
    expect(validerPeriode('2026-05-01', null)).toBeUndefined()
  })
})
