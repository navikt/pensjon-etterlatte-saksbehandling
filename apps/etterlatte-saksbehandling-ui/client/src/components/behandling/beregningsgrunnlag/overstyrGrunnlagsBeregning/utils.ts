import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { OverstyrBeregningsperiode, OverstyrtAarsakKey } from '~shared/types/Beregning'
import { addMonths } from 'date-fns'
import { formaterTilISOString } from '~utils/formatering/dato'

export const stripWhitespace = (s: string | number): string => {
  if (typeof s === 'string') return s.replace(/\s+/g, '')
  else return s.toString().replace(/\s+/g, '')
}

export const initialOverstyrBeregningsgrunnlagPeriode = (
  behandling: IDetaljertBehandling,
  sistePeriode: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode> | undefined
): PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode> => {
  const nesteFomDato = (
    fom: Date | undefined = new Date(behandling.virkningstidspunkt!.dato),
    tom: Date | undefined
  ): Date | string => {
    return tom ? addMonths(tom, 1) : fom
  }

  return {
    fom: formaterTilISOString(
      nesteFomDato(
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

export const validerAarsak = (aarsak: OverstyrtAarsakKey | undefined): string | undefined => {
  if (!aarsak || aarsak === 'VELG_AARSAK') return 'Må settes'
  return undefined
}

export const replacePeriodePaaIndex = (
  periode: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>,
  perioder: Array<PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>>,
  index: number
): Array<PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>> => {
  const kopi = [...perioder]
  kopi.splice(index, 1, periode)
  return kopi
}
