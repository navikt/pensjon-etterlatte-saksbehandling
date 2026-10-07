import { describe, expect, it } from 'vitest'
import { finnInnvilgedePerioder } from './innvilgedePerioder'

const oppgjoersperiode = { fom: '2025-01', tom: '2025-12' }

describe('innvilgede perioder i etteroppgjoer', () => {
  it('bevarer hullet mellom januar-mars og juli-desember', () => {
    const ytelser = [
      { fom: '2025-07', tom: '2025-12' },
      { fom: '2025-02', tom: '2025-03' },
      { fom: '2025-01', tom: '2025-01' },
    ]
    expect(finnInnvilgedePerioder(ytelser, oppgjoersperiode)).toEqual([
      { fom: '2025-01', tom: '2025-03' },
      { fom: '2025-07', tom: '2025-12' },
    ])
    expect(ytelser[2]).toEqual({ fom: '2025-01', tom: '2025-01' })
  })

  it('avgrenser perioder til oppgjoersvinduet og ignorerer andre aar', () => {
    expect(
      finnInnvilgedePerioder(
        [
          { fom: '2023-01', tom: '2023-12' },
          { fom: '2024-11', tom: '2025-03' },
          { fom: '2025-07', tom: '2026-04' },
          { fom: '2026-05', tom: '' },
        ],
        oppgjoersperiode
      )
    ).toEqual([
      { fom: '2025-01', tom: '2025-03' },
      { fom: '2025-07', tom: '2025-12' },
    ])
  })

  it('haandterer overlapp og aapen slutt uten aa duplisere perioder', () => {
    expect(
      finnInnvilgedePerioder(
        [
          { fom: '2025-01', tom: '2025-06' },
          { fom: '2025-03', tom: '2025-03' },
          { fom: '2025-07', tom: '' },
        ],
        oppgjoersperiode
      )
    ).toEqual([oppgjoersperiode])
  })

  it('gir ingen perioder naar beregningsgrunnlaget mangler', () => {
    expect(finnInnvilgedePerioder([], oppgjoersperiode)).toEqual([])
  })
})
