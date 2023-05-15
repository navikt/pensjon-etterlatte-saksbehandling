import { addDays, compareAsc, endOfMonth, isEqual, startOfMonth } from 'date-fns'

export interface PeriodisertBeregningsgrunnlag<G> {
  fom: Date
  tom?: Date
  data: G
}

const FEIL_I_PERIODE = [
  'TOM_FOER_FOM',
  'PERIODE_OVERLAPPER_MED_NESTE',
  'HULL_ETTER_PERIODE',
  'INGEN_PERIODER',
  'DEKKER_IKKE_START_AV_INTERVALL',
  'DEKKER_IKKE_SLUTT_AV_INTERVALL',
] as const
export type FeilIPeriode = (typeof FEIL_I_PERIODE)[number]

export function validerKomplettePerioder(
  grunnlag: PeriodisertBeregningsgrunnlag<unknown>[],
  fom: Date,
  tom?: Date
): [number, FeilIPeriode][] {
  if (grunnlag.length === 0) {
    return [[0, 'INGEN_PERIODER']]
  }

  const feil = new Set<[number, FeilIPeriode]>()
  const sortertMedNormaliserteDatoer = grunnlag
    .map((g) => ({ ...g, fom: startOfMonth(g.fom), tom: g.tom === undefined ? undefined : endOfMonth(g.tom) }))
    .sort((p1, p2) => compareAsc(p1.fom, p2.fom))

  for (let i = 0; i < sortertMedNormaliserteDatoer.length - 1; i += 1) {
    const p1 = sortertMedNormaliserteDatoer[i]
    const p2 = sortertMedNormaliserteDatoer[i + 1]
    if (p1.tom === undefined) {
      feil.add([i, 'PERIODE_OVERLAPPER_MED_NESTE'])
    } else if (!isEqual(addDays(p1.tom, 1), p2.fom)) {
      // TODO denne kan være overlapp også
      feil.add([i, 'HULL_ETTER_PERIODE'])
    }
  }
  const normalisertFom = startOfMonth(fom)
  const normalistertTom = tom === undefined ? undefined : endOfMonth(tom)

  if (compareAsc(sortertMedNormaliserteDatoer[0].fom, normalisertFom) > 0) {
    feil.add([0, 'DEKKER_IKKE_START_AV_INTERVALL'])
  }
  const sisteTom = sortertMedNormaliserteDatoer[sortertMedNormaliserteDatoer.length - 1].tom
  if (sisteTom !== undefined && (normalistertTom === undefined || compareAsc(sisteTom, normalistertTom) < 0)) {
    feil.add([sortertMedNormaliserteDatoer.length - 1, 'DEKKER_IKKE_SLUTT_AV_INTERVALL'])
  }

  return [...feil]
}
