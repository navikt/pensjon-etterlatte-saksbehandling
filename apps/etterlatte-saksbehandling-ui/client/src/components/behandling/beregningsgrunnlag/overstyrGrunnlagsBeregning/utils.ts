import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { InstitusjonsoppholdIBeregning, OverstyrBeregningsperiode, OverstyrtAarsakKey } from '~shared/types/Beregning'
import { addMonths, lastDayOfMonth } from 'date-fns'
import { formaterTilISOString } from '~utils/formatering/dato'

export const stripWhitespace = (s: string | number): string => {
  if (typeof s === 'string') return s.replace(/\s+/g, '')
  else return s.toString().replace(/\s+/g, '')
}
const nesteFomDato = (
  behandling: IDetaljertBehandling,
  fom: Date | undefined = new Date(behandling.virkningstidspunkt!.dato),
  tom: Date | undefined
): Date | string => {
  return tom ? addMonths(tom, 1) : fom
}

export const initialOverstyrBeregningsgrunnlagPeriode = (
  behandling: IDetaljertBehandling,
  sistePeriode: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode> | undefined
): PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode> => {
  return {
    fom: formaterTilISOString(
      nesteFomDato(
        behandling,
        sistePeriode ? new Date(sistePeriode.fom) : new Date(behandling.virkningstidspunkt!.dato),
        sistePeriode && sistePeriode.fom ? new Date(sistePeriode.fom) : undefined
      )
    ),
    tom: undefined,
    data: {
      utbetaltBeloep: '',
      trygdetid: '',
      trygdetidForIdent: '',
      prorataBroekNevner: '',
      prorataBroekTeller: '',
      beskrivelse: '',
      aarsak: 'VELG_AARSAK',
    },
  }
}

export const initalInstitusjonsoppholdPeriode = (
  behandling: IDetaljertBehandling,
  sistePeriode: PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning> | undefined
): PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning> => {
  return {
    fom: formaterTilISOString(
      nesteFomDato(
        behandling,
        sistePeriode ? new Date(sistePeriode.fom) : new Date(behandling.virkningstidspunkt!.dato),
        sistePeriode && sistePeriode.fom ? new Date(sistePeriode.fom) : undefined
      )
    ),
    tom: undefined,
    data: {
      reduksjon: 'VELG_REDUKSJON',
      egenReduksjon: undefined,
      begrunnelse: '',
    },
  }
}

export const konverterTilSisteDagIMaaneden = (dato: string): string => {
  return formaterTilISOString(lastDayOfMonth(dato))
}

export const validerAarsak = (aarsak: OverstyrtAarsakKey | undefined): string | undefined => {
  if (!aarsak || aarsak === 'VELG_AARSAK') return 'Må settes'
  return undefined
}

export const replacePeriodePaaIndex = <G>(
  periode: PeriodisertBeregningsgrunnlagDto<G>,
  perioder: Array<PeriodisertBeregningsgrunnlagDto<G>>,
  index: number
): Array<PeriodisertBeregningsgrunnlagDto<G>> => {
  const kopi = [...perioder]
  kopi.splice(index, 1, periode)

  return kopi
}
