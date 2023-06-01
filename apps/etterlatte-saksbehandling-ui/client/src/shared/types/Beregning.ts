import {
  PeriodisertBeregningsgrunnlag,
  PeriodisertBeregningsgrunnlagDto,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { KildeSaksbehandler } from '~shared/types/kilde'

export interface Beregning {
  beregningId: string
  behandlingId: string
  type: Beregningstype
  beregningsperioder: Beregningsperiode[]
  beregnetDato: string
  grunnlagMetadata: GrunnlagMetadata
}

export interface GrunnlagMetadata {
  sakId: string
  versjon: number
}

export enum Beregningstype {
  BP = 'BP',
  OMS = 'OMS',
}

export interface Beregningsperiode {
  delytelsesId: string
  datoFOM: string
  datoTOM: string
  utbetaltBeloep: number
  soeskenFlokk: string[]
  grunnbelopMnd: number
  grunnbelop: number
  trygdetid: number
}

export interface BeregningsGrunnlagDto {
  behandlingId: string
  kilde: KildeSaksbehandler
  institusjonsopphold: InstitusjonsoppholdGrunnlag
  soeskenMedIBeregning: PeriodisertBeregningsgrunnlagDto<SoeskenMedIBeregning[]>[]
}

export interface SoeskenMedIBeregning {
  foedselsnummer: string
  skalBrukes: boolean
}

export interface BeregningsGrunnlagData {
  soeskenMedIBeregning: PeriodisertBeregningsgrunnlagDto<SoeskenMedIBeregning[]>[]
  institusjonsopphold: InstitusjonsoppholdGrunnlag | undefined
}

export type InstitusjonsoppholdGrunnlag = PeriodisertBeregningsgrunnlag<InstitusjonsoppholdIBeregning>[]

export interface InstitusjonsoppholdIBeregning {
  reduksjon: Reduksjon
  egenReduksjon?: number
  begrunnelse?: string | undefined
}

export enum Reduksjon {
  VELG_REDUKSJON = 'Velg reduksjon',
  JA_VANLIG = 'Ja, etter vanlig sats(10% av G)',
  NEI_KORT_OPPHOLD = 'Nei, kort opphold',
  JA_EGEN_PROSENT_AV_G = 'Ja, utgifter til bolig(egen % av G)',
  NEI_HOEYE_UTGIFTER_BOLIG = 'Nei, har høye utgifter til bolig',
}
