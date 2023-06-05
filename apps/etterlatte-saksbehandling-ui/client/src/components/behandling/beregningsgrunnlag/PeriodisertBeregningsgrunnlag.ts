import { compareAsc, differenceInDays, endOfMonth, format, parse, startOfMonth } from 'date-fns'
import { FeilIPeriode } from '~components/behandling/beregningsgrunnlag/PerioderFelles'

export interface PeriodisertBeregningsgrunnlag<G> {
  fom: Date
  tom?: Date
  data: G
}

export interface PeriodisertBeregningsgrunnlagDto<G> {
  fom: string
  tom?: string
  data: G
}

export function periodisertBeregningsgrunnlagFraDto<G>(
  dto: PeriodisertBeregningsgrunnlagDto<G>
): PeriodisertBeregningsgrunnlag<G> {
  return {
    data: dto.data,
    fom: parse(dto.fom, 'yyyy-MM-dd', 0),
    tom: !dto.tom ? undefined : parse(dto.tom, 'yyyy-MM-dd', 0),
  }
}

export function periodisertBeregningsgrunnlagTilDto<G>(
  grunnlag: PeriodisertBeregningsgrunnlag<G>
): PeriodisertBeregningsgrunnlagDto<G> {
  return {
    data: grunnlag.data,
    fom: format(startOfMonth(grunnlag.fom), 'yyyy-MM-dd'),
    tom: !grunnlag.tom ? undefined : format(endOfMonth(grunnlag.tom), 'yyyy-MM-dd'),
  }
}

export function mapListeFraDto<G>(dto: PeriodisertBeregningsgrunnlagDto<G>[]): PeriodisertBeregningsgrunnlag<G>[] {
  return dto.map(periodisertBeregningsgrunnlagFraDto)
}

export function mapListeTilDto<G>(grunnlag: PeriodisertBeregningsgrunnlag<G>[]): PeriodisertBeregningsgrunnlagDto<G>[] {
  return grunnlag.map(periodisertBeregningsgrunnlagTilDto)
}

export function feilIKomplettePerioderOverIntervall(
  grunnlag: PeriodisertBeregningsgrunnlag<unknown>[],
  fom: Date,
  tom?: Date
): [number, FeilIPeriode][] {
  if (grunnlag.length === 0) {
    return [[0, 'INGEN_PERIODER']]
  }

  const feil = new Set<[number, FeilIPeriode]>()
  const sortertMedNormaliserteDatoer = grunnlag
    .map((g, index) => ({
      ...g,
      fom: startOfMonth(g.fom),
      tom: g.tom === undefined ? undefined : endOfMonth(g.tom),
      reellIndex: index,
    }))
    .sort((p1, p2) => compareAsc(p1.fom, p2.fom))

  for (let i = 0; i < sortertMedNormaliserteDatoer.length - 1; i += 1) {
    const p1 = sortertMedNormaliserteDatoer[i]
    const p2 = sortertMedNormaliserteDatoer[i + 1]
    if (p1.tom === undefined || compareAsc(p1.tom, p2.fom) >= 0) {
      feil.add([p1.reellIndex, 'PERIODE_OVERLAPPER_MED_NESTE'])
    } else if (Math.abs(differenceInDays(p1.tom, p2.fom)) > 1) {
      feil.add([p1.reellIndex, 'HULL_ETTER_PERIODE'])
    }
  }
  const normalisertFom = startOfMonth(fom)
  const normalistertTom = tom === undefined ? undefined : endOfMonth(tom)

  if (compareAsc(sortertMedNormaliserteDatoer[0].fom, normalisertFom) > 0) {
    feil.add([sortertMedNormaliserteDatoer[0].reellIndex, 'DEKKER_IKKE_START_AV_INTERVALL'])
  }

  const sistePeriode = sortertMedNormaliserteDatoer[sortertMedNormaliserteDatoer.length - 1]
  const sisteTom = sistePeriode.tom
  if (sisteTom !== undefined && (normalistertTom === undefined || compareAsc(sisteTom, normalistertTom) < 0)) {
    feil.add([sistePeriode.reellIndex, 'DEKKER_IKKE_SLUTT_AV_INTERVALL'])
  }

  return [...feil]
}
