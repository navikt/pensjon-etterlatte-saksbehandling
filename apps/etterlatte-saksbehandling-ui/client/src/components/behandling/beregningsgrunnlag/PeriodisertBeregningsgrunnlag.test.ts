import { describe, expect, it } from 'vitest'
import {
  PeriodisertBeregningsgrunnlag,
  feilIKomplettePerioderOverIntervall,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'

describe('feilIKomplettePerioderOverIntervall', () => {
  it('når ingen perioder er oppgitt', () => {
    expect(feilIKomplettePerioderOverIntervall([], new Date())).toContainEqual([0, 'INGEN_PERIODER'])
  })

  it('når en periode lik intervall er oppgitt (med fom uten tom)', () => {
    const fom = new Date()
    expect(feilIKomplettePerioderOverIntervall([periode(fom)], fom)).to.be.empty
  })

  it('når to perioder ikke overlapper', () => {
    const fom1 = dato(2022, 8, 1)
    const tom1 = dato(2022, 10, 31)
    const fom2 = dato(2022, 11, 1)
    const p1 = periode(fom1, tom1)
    const p2 = periode(fom2)
    expect(feilIKomplettePerioderOverIntervall([p1, p2], fom1)).to.be.empty
  })

  it('når to perioder overlapper (ingen tom)', () => {
    const fom1 = dato(2022, 8, 1)
    const fom2 = dato(2022, 10, 1)
    const p1 = periode(fom1)
    const p2 = periode(fom2)
    expect(feilIKomplettePerioderOverIntervall([p1, p2], fom1)).toContainEqual([0, 'PERIODE_OVERLAPPER_MED_NESTE'])
  })

  it('når to perioder overlapper (tom etter neste fom)', () => {
    const fom1 = dato(2022, 8, 1)
    const tom1 = dato(2022, 11, 1)
    const fom2 = dato(2022, 10, 1)
    expect(feilIKomplettePerioderOverIntervall([periode(fom1, tom1), periode(fom2)], fom1)).toContainEqual([
      0,
      'PERIODE_OVERLAPPER_MED_NESTE',
    ])
  })

  it('når to perioder har hull mellom seg', () => {
    const fom1 = dato(2022, 8, 1)
    const tom1 = dato(2022, 8, 31)
    const fom2 = dato(2022, 10, 1)
    expect(feilIKomplettePerioderOverIntervall([periode(fom1, tom1), periode(fom2)], fom1)).toContainEqual([
      0,
      'HULL_ETTER_PERIODE',
    ])
  })

  it('når første periode ikke dekker start av intervall', () => {
    const fomPeriode = dato(2022, 8, 1)
    const fomIntervall = dato(2022, 5, 1)
    expect(feilIKomplettePerioderOverIntervall([periode(fomPeriode)], fomIntervall)).toContainEqual([
      0,
      'DEKKER_IKKE_START_AV_INTERVALL',
    ])
  })

  it('når siste periode ikke dekker slutten av intervall gir det feil', () => {
    const fom = dato(2022, 8, 1)
    const tom = dato(2022, 10, 31)
    expect(feilIKomplettePerioderOverIntervall([periode(fom, tom)], fom)).toContainEqual([
      0,
      'DEKKER_IKKE_SLUTT_AV_INTERVALL',
    ])

    expect(feilIKomplettePerioderOverIntervall([periode(fom, tom)], fom, dato(2023, 1, 1))).toContainEqual([
      0,
      'DEKKER_IKKE_SLUTT_AV_INTERVALL',
    ])
  })

  it('tre perioder uten overlapp, med tom angitt som start i måneden gir ingen feil', () => {
    const p1 = periode(dato(2022, 8, 1), dato(2022, 8, 1))
    const p2 = periode(dato(2022, 9, 1), dato(2022, 10, 1))
    const p3 = periode(dato(2022, 11, 1))
    expect(feilIKomplettePerioderOverIntervall([p1, p2, p3], dato(2022, 8, 1))).to.be.empty
  })

  it('tre perioder med hull etter periode 1 (2 kronologisk) gir feil', () => {
    const p1 = periode(dato(2022, 8, 1), dato(2022, 8, 31))
    const p2 = periode(dato(2022, 9, 1), dato(2022, 10, 31))
    const p3 = periode(dato(2022, 12, 1))
    expect(feilIKomplettePerioderOverIntervall([p2, p1, p3], p1.fom)).toContainEqual([0, 'HULL_ETTER_PERIODE'])
  })
})

/**
 * Hjelpemetode for å unngå å måtte tenke på 0-indekserte måneder vs 1-indekserte år og dager
 */
const dato = (aar: number, maaned: number, dag: number): Date => new Date(aar, maaned - 1, dag)

function periode(fom: Date, tom?: Date): PeriodisertBeregningsgrunnlag<unknown> {
  return {
    fom,
    tom,
    data: undefined,
  }
}
